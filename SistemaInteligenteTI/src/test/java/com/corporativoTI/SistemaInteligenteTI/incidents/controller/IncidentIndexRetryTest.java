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
import com.corporativoTI.SistemaInteligenteTI.ai.dto.EmbeddingRequest;
import com.corporativoTI.SistemaInteligenteTI.ai.dto.EmbeddingResponse;
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
 * RF-13 (T-33): {@code POST /api/incidents/{id}/index/retry} marca como indexado un incidente
 * resuelto que quedó fuera del histórico; mientras siga sin indexar, el backend lo expone como
 * {@code indexed = false} y el servicio de IA no tiene su fila (por eso no sale como caso similar).
 * Solo lo lanza el supervisor. El cliente de IA está simulado (RNF-9).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IncidentIndexRetryTest {

    private static final String USER_HEADER = "X-User-Id";
    private static final String SOLUTION =
            "Se reinició el servicio y se verificó el funcionamiento correcto.";

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
        technician.setUsername("tecnico.t33");
        technician.setFullName("Técnico T33");
        technician.setRole(Role.TECHNICIAN);
        technician.setArea(Area.APPLICATIONS);
        technician = userRepository.save(technician);
    }

    private Incident incident(IncidentStatus status, boolean indexed) {
        Incident incident = new Incident();
        incident.setTitle("T33 incidente");
        incident.setDescription("Descripción");
        incident.setStatus(status);
        incident.setClassificationStatus(ClassificationStatus.AI_CLASSIFIED);
        incident.setType(IncidentType.BUG);
        incident.setPriority(Priority.HIGH);
        incident.setArea(Area.APPLICATIONS);
        incident.setCreatedBy(supervisor);
        incident.setAssignedTo(technician);
        incident.setCreatedAt(Instant.now());
        if (status == IncidentStatus.RESOLVED) {
            incident.setSolution(SOLUTION);
            incident.setClosedAt(Instant.now());
            incident.setIndexed(indexed);
        }
        return incidentRepository.saveAndFlush(incident);
    }

    private ResultActions retry(User actor, Long id) throws Exception {
        return mockMvc.perform(
                post("/api/incidents/{id}/index/retry", id).header(USER_HEADER, actor.getId()));
    }

    private List<IncidentEventAction> actionsOf(Incident incident) {
        return eventRepository.findByIncidentIdOrderByIdAsc(incident.getId()).stream()
                .map(IncidentEvent::getAction)
                .toList();
    }

    @Test
    void marksTheUnindexedResolvedIncidentAsIndexed() throws Exception {
        Incident incident = incident(IncidentStatus.RESOLVED, false);
        when(aiClient.index(any()))
                .thenReturn(new AiResult.Success<>(new EmbeddingResponse(true, "modelo-simulado", 384)));

        retry(supervisor, incident.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.indexed").value(true));

        ArgumentCaptor<EmbeddingRequest> captor = ArgumentCaptor.forClass(EmbeddingRequest.class);
        verify(aiClient).index(captor.capture());
        assertThat(captor.getValue().incidentId()).isEqualTo(incident.getId());
        assertThat(captor.getValue().solution()).isEqualTo(SOLUTION);

        Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(saved.isIndexed()).isTrue();
        assertThat(actionsOf(saved)).containsExactly(IncidentEventAction.INDEXED);
    }

    @Test
    void staysUnindexedAndKeepsTheSolutionWhenTheAiFailsAgain() throws Exception {
        Incident incident = incident(IncidentStatus.RESOLVED, false);
        when(aiClient.index(any())).thenReturn(new AiResult.Unavailable<>("HttpTimeoutException"));

        retry(supervisor, incident.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.solution").value(SOLUTION))
                .andExpect(jsonPath("$.indexed").value(false));

        Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(saved.isIndexed()).isFalse();
        assertThat(saved.getSolution()).isEqualTo(SOLUTION);
        assertThat(actionsOf(saved)).containsExactly(IncidentEventAction.INDEXING_FAILED);
    }

    @Test
    void technicianCannotRetryAndTheAiIsNotCalled() throws Exception {
        Incident incident = incident(IncidentStatus.RESOLVED, false);

        retry(technician, incident.getId())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN_ROLE"));

        verify(aiClient, never()).index(any());
        assertThat(incidentRepository.findById(incident.getId()).orElseThrow().isIndexed()).isFalse();
    }

    @Test
    void rejectsIncidentsThatAreNotResolvedWithoutCallingTheAi() throws Exception {
        Incident inProgress = incident(IncidentStatus.IN_PROGRESS, false);

        retry(supervisor, inProgress.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INCIDENT_NOT_RESOLVED"));

        verify(aiClient, never()).index(any());
    }

    @Test
    void reindexingAnAlreadyIndexedIncidentIsSafe() throws Exception {
        Incident incident = incident(IncidentStatus.RESOLVED, true);
        when(aiClient.index(any()))
                .thenReturn(new AiResult.Success<>(new EmbeddingResponse(true, "modelo-simulado", 384)));

        retry(supervisor, incident.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indexed").value(true));
    }

    @Test
    void unknownIncidentReturns404() throws Exception {
        retry(supervisor, 999_999_999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INCIDENT_NOT_FOUND"));
    }

    @Test
    void requestWithoutActiveUserReturns400() throws Exception {
        mockMvc.perform(post("/api/incidents/1/index/retry"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_ACTIVE_USER"));
    }
}
