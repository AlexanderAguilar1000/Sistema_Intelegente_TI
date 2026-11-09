package com.corporativoTI.SistemaInteligenteTI.incidents.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.corporativoTI.SistemaInteligenteTI.ai.client.AiClient;
import com.corporativoTI.SistemaInteligenteTI.ai.dto.ClassificationResponse;
import com.corporativoTI.SistemaInteligenteTI.ai.model.AiResult;
import com.corporativoTI.SistemaInteligenteTI.audit.model.IncidentEvent;
import com.corporativoTI.SistemaInteligenteTI.audit.model.IncidentEventAction;
import com.corporativoTI.SistemaInteligenteTI.audit.repository.IncidentEventRepository;
import com.corporativoTI.SistemaInteligenteTI.catalog.model.Area;
import com.corporativoTI.SistemaInteligenteTI.catalog.model.IncidentType;
import com.corporativoTI.SistemaInteligenteTI.catalog.model.Priority;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.ClassificationStatus;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.ClosureReason;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.Incident;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.IncidentStatus;
import com.corporativoTI.SistemaInteligenteTI.incidents.repository.IncidentRepository;
import com.corporativoTI.SistemaInteligenteTI.users.model.Role;
import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import com.corporativoTI.SistemaInteligenteTI.users.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-4 (T-31): {@code POST /api/incidents/{id}/classification/retry} deja el incidente clasificado
 * si la IA responde bien y lo deja pendiente si vuelve a fallar; solo lo puede lanzar el
 * supervisor. El cliente de IA está simulado (RNF-9).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IncidentClassificationRetryTest {

    private static final String USER_HEADER = "X-User-Id";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private IncidentEventRepository eventRepository;

    @MockitoBean private AiClient aiClient;

    private User supervisor;
    private User technician;

    @BeforeEach
    void setUp() {
        supervisor = userRepository.findByUsername("laura.supervisora").orElseThrow();
        technician = new User();
        technician.setUsername("tecnico.t31");
        technician.setFullName("Técnico T31");
        technician.setRole(Role.TECHNICIAN);
        technician.setArea(Area.APPLICATIONS);
        technician = userRepository.save(technician);
    }

    private Incident pendingIncident() {
        Incident incident = new Incident();
        incident.setTitle("T31 pendiente");
        incident.setDescription("Descripción");
        incident.setStatus(IncidentStatus.REGISTERED);
        incident.setClassificationStatus(ClassificationStatus.PENDING);
        incident.setCreatedBy(supervisor);
        incident.setCreatedAt(Instant.now());
        return incidentRepository.saveAndFlush(incident);
    }

    private Incident classifiedIncident(ClassificationStatus classification) {
        Incident incident = pendingIncident();
        incident.setClassificationStatus(classification);
        incident.setType(IncidentType.BUG);
        incident.setPriority(Priority.HIGH);
        incident.setArea(Area.APPLICATIONS);
        return incidentRepository.saveAndFlush(incident);
    }

    private ResultActions retry(User actor, Long id) throws Exception {
        return mockMvc.perform(
                post("/api/incidents/{id}/classification/retry", id).header(USER_HEADER, actor.getId()));
    }

    private static ClassificationResponse classification(String type, String priority, String area) {
        return new ClassificationResponse(type, priority, area, "modelo-simulado", 900);
    }

    private List<IncidentEventAction> actionsOf(Incident incident) {
        return eventRepository.findByIncidentIdOrderByIdAsc(incident.getId()).stream()
                .map(IncidentEvent::getAction)
                .toList();
    }

    @Test
    void leavesThePendingIncidentClassifiedWhenTheAiAnswers() throws Exception {
        Incident incident = pendingIncident();
        when(aiClient.classify(any()))
                .thenReturn(new AiResult.Success<>(classification("ACCESS", "HIGH", "SECURITY")));

        retry(supervisor, incident.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incident.getId()))
                .andExpect(jsonPath("$.classificationStatus").value("AI_CLASSIFIED"))
                .andExpect(jsonPath("$.type").value("ACCESS"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.area").value("SECURITY"))
                .andExpect(jsonPath("$.status").value("REGISTERED"));

        Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(saved.getClassificationStatus()).isEqualTo(ClassificationStatus.AI_CLASSIFIED);
        assertThat(saved.getType()).isEqualTo(IncidentType.ACCESS);
        assertThat(saved.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(saved.getArea()).isEqualTo(Area.SECURITY);
        assertThat(actionsOf(saved))
                .containsExactly(
                        IncidentEventAction.CLASSIFICATION_RETRIED, IncidentEventAction.AI_CLASSIFIED);
    }

    @Test
    void leavesTheIncidentPendingWhenTheAiFailsAgain() throws Exception {
        Incident incident = pendingIncident();
        when(aiClient.classify(any())).thenReturn(new AiResult.Unavailable<>("HttpTimeoutException"));

        retry(supervisor, incident.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classificationStatus").value("PENDING"))
                .andExpect(jsonPath("$.type").doesNotExist())
                .andExpect(jsonPath("$.priority").doesNotExist())
                .andExpect(jsonPath("$.area").doesNotExist());

        Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(saved.getClassificationStatus()).isEqualTo(ClassificationStatus.PENDING);
        assertThat(saved.getType()).isNull();
        assertThat(actionsOf(saved))
                .containsExactly(
                        IncidentEventAction.CLASSIFICATION_RETRIED,
                        IncidentEventAction.CLASSIFICATION_FAILED);
    }

    @Test
    void leavesTheIncidentPendingWhenTheAiRejectsOrAnswersOutsideTheCatalog() throws Exception {
        Incident rejected = pendingIncident();
        when(aiClient.classify(any()))
                .thenReturn(new AiResult.Rejected<>("INVALID_CLASSIFICATION", "valor fuera de catálogo"));
        retry(supervisor, rejected.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classificationStatus").value("PENDING"));

        Incident outside = pendingIncident();
        when(aiClient.classify(any()))
                .thenReturn(new AiResult.Success<>(classification("MAGIA", "HIGH", "SECURITY")));
        retry(supervisor, outside.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classificationStatus").value("PENDING"))
                .andExpect(jsonPath("$.type").doesNotExist());
    }

    @Test
    void technicianCannotRetryAndTheAiIsNotCalled() throws Exception {
        Incident incident = pendingIncident();

        retry(technician, incident.getId())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN_ROLE"));

        verify(aiClient, never()).classify(any());
        assertThat(incidentRepository.findById(incident.getId()).orElseThrow().getClassificationStatus())
                .isEqualTo(ClassificationStatus.PENDING);
    }

    @Test
    void rejectsRetryOnAnIncidentThatIsNotPendingWithoutCallingTheAi() throws Exception {
        for (ClassificationStatus classified :
                new ClassificationStatus[] {
                    ClassificationStatus.AI_CLASSIFIED, ClassificationStatus.MANUALLY_CORRECTED
                }) {
            Incident incident = classifiedIncident(classified);

            retry(supervisor, incident.getId())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("CLASSIFICATION_NOT_PENDING"));

            Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
            assertThat(saved.getClassificationStatus()).isEqualTo(classified);
            assertThat(saved.getPriority()).isEqualTo(Priority.HIGH);
        }
        verify(aiClient, never()).classify(any());
    }

    @Test
    void closedIncidentsReturn409WithoutCallingTheAi() throws Exception {
        Incident incident = pendingIncident();
        incident.setStatus(IncidentStatus.CLOSED_WITHOUT_SOLUTION);
        incident.setAssignedTo(technician);
        incident.setClosureReason(ClosureReason.DISCARDED);
        incident.setClosedAt(Instant.now());
        incidentRepository.saveAndFlush(incident);

        retry(supervisor, incident.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INCIDENT_CLOSED"));

        verify(aiClient, never()).classify(any());
    }

    @Test
    void unknownIncidentReturns404() throws Exception {
        retry(supervisor, 999_999_999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INCIDENT_NOT_FOUND"));
    }

    @Test
    void requestWithoutActiveUserReturns400() throws Exception {
        mockMvc.perform(post("/api/incidents/1/classification/retry"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_ACTIVE_USER"));
    }
}
