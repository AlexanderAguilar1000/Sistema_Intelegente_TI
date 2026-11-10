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
import com.corporativoTI.SistemaInteligenteTI.ai.dto.AnalysisReference;
import com.corporativoTI.SistemaInteligenteTI.ai.dto.AnalysisRequest;
import com.corporativoTI.SistemaInteligenteTI.ai.dto.AnalysisResponse;
import com.corporativoTI.SistemaInteligenteTI.ai.model.AiResult;
import com.corporativoTI.SistemaInteligenteTI.audit.model.IncidentEvent;
import com.corporativoTI.SistemaInteligenteTI.audit.model.IncidentEventAction;
import com.corporativoTI.SistemaInteligenteTI.audit.repository.IncidentEventRepository;
import com.corporativoTI.SistemaInteligenteTI.catalog.model.Area;
import com.corporativoTI.SistemaInteligenteTI.catalog.model.IncidentType;
import com.corporativoTI.SistemaInteligenteTI.catalog.model.Priority;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.ClassificationStatus;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.Incident;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.IncidentStatus;
import com.corporativoTI.SistemaInteligenteTI.incidents.repository.IncidentRepository;
import com.corporativoTI.SistemaInteligenteTI.users.model.Role;
import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import com.corporativoTI.SistemaInteligenteTI.users.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-11 (T-34): {@code POST /api/incidents/{id}/analysis} devuelve pasos y referencias al técnico
 * asignado; el supervisor recibe 403; un segundo análisis mientras hay uno en curso recibe 409; un
 * fallo de la IA devuelve error sin alterar el incidente. El cliente de IA está simulado (RNF-9).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IncidentAnalysisTest {

    private static final String USER_HEADER = "X-User-Id";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private IncidentEventRepository eventRepository;

    @MockitoBean private AiClient aiClient;

    private User supervisor;
    private User technician;
    private User otherTechnician;

    @BeforeEach
    void setUp() {
        supervisor = userRepository.findByUsername("laura.supervisora").orElseThrow();
        technician = technician("tecnico.t34a");
        otherTechnician = technician("tecnico.t34b");
    }

    private User technician(String username) {
        User user = new User();
        user.setUsername(username);
        user.setFullName("Técnico " + username);
        user.setRole(Role.TECHNICIAN);
        user.setArea(Area.APPLICATIONS);
        return userRepository.save(user);
    }

    private Incident incident(IncidentStatus status, User assignedTo) {
        Incident incident = new Incident();
        incident.setTitle("T34 incidente");
        incident.setDescription("La aplicación no carga el formulario de pedidos");
        incident.setStatus(status);
        incident.setClassificationStatus(ClassificationStatus.AI_CLASSIFIED);
        incident.setType(IncidentType.BUG);
        incident.setPriority(Priority.HIGH);
        incident.setArea(Area.APPLICATIONS);
        incident.setCreatedBy(supervisor);
        incident.setAssignedTo(assignedTo);
        incident.setCreatedAt(Instant.now());
        if (status == IncidentStatus.RESOLVED) {
            incident.setSolution("Se reinició el servicio y se verificó el funcionamiento.");
            incident.setClosedAt(Instant.now());
        }
        return incidentRepository.saveAndFlush(incident);
    }

    private ResultActions analyze(User actor, Long id) throws Exception {
        return mockMvc.perform(
                post("/api/incidents/{id}/analysis", id).header(USER_HEADER, actor.getId()));
    }

    private List<IncidentEventAction> actionsOf(Incident incident) {
        return eventRepository.findByIncidentIdOrderByIdAsc(incident.getId()).stream()
                .map(IncidentEvent::getAction)
                .toList();
    }

    private static AnalysisResponse grounded() {
        return new AnalysisResponse(
                true,
                List.of("Revisar el registro del servicio", "Reiniciar el servicio"),
                List.of(new AnalysisReference(7L, "Formulario de pedidos caído", 0.83)),
                "modelo-simulado",
                1200);
    }

    @Test
    void assignedTechnicianGetsStepsAndReferences() throws Exception {
        Incident incident = incident(IncidentStatus.IN_PROGRESS, technician);
        when(aiClient.analyze(any())).thenReturn(new AiResult.Success<>(grounded()));

        analyze(technician, incident.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grounded").value(true))
                .andExpect(jsonPath("$.steps[0]").value("Revisar el registro del servicio"))
                .andExpect(jsonPath("$.steps[1]").value("Reiniciar el servicio"))
                .andExpect(jsonPath("$.references[0].incidentId").value(7))
                .andExpect(jsonPath("$.references[0].title").value("Formulario de pedidos caído"))
                .andExpect(jsonPath("$.references[0].score").value(0.83));

        ArgumentCaptor<AnalysisRequest> captor = ArgumentCaptor.forClass(AnalysisRequest.class);
        verify(aiClient).analyze(captor.capture());
        assertThat(captor.getValue().incidentId()).isEqualTo(incident.getId());
        assertThat(captor.getValue().type()).isEqualTo("BUG");
        assertThat(captor.getValue().priority()).isEqualTo("HIGH");
        assertThat(captor.getValue().area()).isEqualTo("APPLICATIONS");
        assertThat(captor.getValue().topK()).isEqualTo(3);
        assertThat(captor.getValue().minScore()).isEqualTo(0.75);

        Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(saved.getAnalysisStartedAt()).isNull();
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(actionsOf(saved)).containsExactly(IncidentEventAction.ANALYSIS_REQUESTED);
    }

    @Test
    void worksForAnAssignedIncidentAndForAGeneralRecommendationWithoutCases() throws Exception {
        Incident incident = incident(IncidentStatus.ASSIGNED, technician);
        when(aiClient.analyze(any()))
                .thenReturn(
                        new AiResult.Success<>(
                                new AnalysisResponse(
                                        false, List.of("Comprobar la configuración"), List.of(), "m", 900)));

        analyze(technician, incident.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grounded").value(false))
                .andExpect(jsonPath("$.steps[0]").value("Comprobar la configuración"))
                .andExpect(jsonPath("$.references").isEmpty());
    }

    @Test
    void supervisorCannotAnalyzeAndTheAiIsNotCalled() throws Exception {
        Incident incident = incident(IncidentStatus.IN_PROGRESS, technician);

        analyze(supervisor, incident.getId())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN_ROLE"));

        verify(aiClient, never()).analyze(any());
    }

    @Test
    void technicianWhoIsNotAssignedCannotAnalyze() throws Exception {
        Incident incident = incident(IncidentStatus.IN_PROGRESS, technician);

        analyze(otherTechnician, incident.getId())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN_ROLE"));

        verify(aiClient, never()).analyze(any());
    }

    @Test
    void secondAnalysisWhileOneIsRunningReturns409() throws Exception {
        Incident incident = incident(IncidentStatus.IN_PROGRESS, technician);
        incident.setAnalysisStartedAt(Instant.now().minusSeconds(5));
        incidentRepository.saveAndFlush(incident);

        analyze(technician, incident.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ANALYSIS_IN_PROGRESS"));

        verify(aiClient, never()).analyze(any());
    }

    @Test
    void expiredAnalysisMarkDoesNotBlockANewAnalysis() throws Exception {
        Incident incident = incident(IncidentStatus.IN_PROGRESS, technician);
        incident.setAnalysisStartedAt(Instant.now().minus(Duration.ofSeconds(31)));
        incidentRepository.saveAndFlush(incident);
        when(aiClient.analyze(any())).thenReturn(new AiResult.Success<>(grounded()));

        analyze(technician, incident.getId()).andExpect(status().isOk());

        assertThat(incidentRepository.findById(incident.getId()).orElseThrow().getAnalysisStartedAt())
                .isNull();
    }

    @Test
    void aiUnavailableReturnsErrorAndLeavesTheIncidentUnchangedAndRetryable() throws Exception {
        Incident incident = incident(IncidentStatus.IN_PROGRESS, technician);
        when(aiClient.analyze(any())).thenReturn(new AiResult.Unavailable<>("HttpTimeoutException"));

        analyze(technician, incident.getId())
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("AI_UNAVAILABLE"));

        Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(saved.getAssignedTo().getId()).isEqualTo(technician.getId());
        assertThat(saved.getSolution()).isNull();
        assertThat(saved.getAnalysisStartedAt()).isNull();
        assertThat(actionsOf(saved)).containsExactly(IncidentEventAction.ANALYSIS_FAILED);

        // Reintento posible: la marca de análisis en curso se liberó.
        when(aiClient.analyze(any())).thenReturn(new AiResult.Success<>(grounded()));
        analyze(technician, incident.getId()).andExpect(status().isOk());
    }

    @Test
    void aiRejectionReturnsErrorWithoutAlteringTheIncident() throws Exception {
        Incident incident = incident(IncidentStatus.IN_PROGRESS, technician);
        when(aiClient.analyze(any()))
                .thenReturn(new AiResult.Rejected<>("INVALID_RECOMMENDATION", "sin pasos con texto"));

        analyze(technician, incident.getId())
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.errorCode").value("AI_INVALID_RESPONSE"));

        Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(saved.getAnalysisStartedAt()).isNull();
        assertThat(actionsOf(saved)).containsExactly(IncidentEventAction.ANALYSIS_FAILED);
    }

    @Test
    void responseWithoutAnyStepWithTextIsTreatedAsFailure() throws Exception {
        Incident incident = incident(IncidentStatus.IN_PROGRESS, technician);
        when(aiClient.analyze(any()))
                .thenReturn(
                        new AiResult.Success<>(
                                new AnalysisResponse(false, List.of("  ", ""), List.of(), "m", 10)));

        analyze(technician, incident.getId())
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.errorCode").value("AI_INVALID_RESPONSE"));

        assertThat(actionsOf(incidentRepository.findById(incident.getId()).orElseThrow()))
                .containsExactly(IncidentEventAction.ANALYSIS_FAILED);
    }

    @Test
    void resolvedIncidentCannotBeAnalyzed() throws Exception {
        Incident incident = incident(IncidentStatus.RESOLVED, technician);

        analyze(technician, incident.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INCIDENT_CLOSED"));

        verify(aiClient, never()).analyze(any());
    }

    @Test
    void unknownIncidentReturns404() throws Exception {
        analyze(technician, 999_999_999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INCIDENT_NOT_FOUND"));
    }

    @Test
    void requestWithoutActiveUserReturns400() throws Exception {
        mockMvc.perform(post("/api/incidents/1/analysis"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_ACTIVE_USER"));
    }
}
