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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-13 (T-32): al resolver se pide la indexación y el incidente queda marcado como indexado; si
 * la indexación falla, sigue Resuelto pero sin indexar y no se pierde la solución (RNF-8). El
 * cliente de IA está simulado (RNF-9).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IncidentResolutionIndexingTest {

    private static final String USER_HEADER = "X-User-Id";
    private static final String SOLUTION =
            "Se reinició el servicio y se verificó el funcionamiento correcto.";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private IncidentEventRepository eventRepository;

    @MockitoBean private AiClient aiClient;

    private User technician;
    private Incident incident;

    @BeforeEach
    void setUp() {
        User supervisor = userRepository.findByUsername("laura.supervisora").orElseThrow();
        technician = new User();
        technician.setUsername("tecnico.t32");
        technician.setFullName("Técnico T32");
        technician.setRole(Role.TECHNICIAN);
        technician.setArea(Area.APPLICATIONS);
        technician = userRepository.save(technician);

        incident = new Incident();
        incident.setTitle("T32 en progreso");
        incident.setDescription("Descripción");
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incident.setClassificationStatus(ClassificationStatus.AI_CLASSIFIED);
        incident.setType(IncidentType.BUG);
        incident.setPriority(Priority.HIGH);
        incident.setArea(Area.APPLICATIONS);
        incident.setCreatedBy(supervisor);
        incident.setAssignedTo(technician);
        incident.setCreatedAt(Instant.now());
        incident = incidentRepository.saveAndFlush(incident);
    }

    private ResultActions resolve() throws Exception {
        return mockMvc.perform(
                post("/api/incidents/{id}/resolution", incident.getId())
                        .header(USER_HEADER, technician.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"solution\": \"" + SOLUTION + "\"}"));
    }

    private List<IncidentEventAction> actions() {
        return eventRepository.findByIncidentIdOrderByIdAsc(incident.getId()).stream()
                .map(IncidentEvent::getAction)
                .toList();
    }

    @Test
    void marksTheResolvedIncidentAsIndexedWhenTheAiIndexesIt() throws Exception {
        when(aiClient.index(any()))
                .thenReturn(new AiResult.Success<>(new EmbeddingResponse(true, "modelo-simulado", 384)));

        resolve().andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVED"));

        Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
        assertThat(saved.isIndexed()).isTrue();
        assertThat(actions()).containsExactly(IncidentEventAction.RESOLVED, IncidentEventAction.INDEXED);
    }

    @Test
    void sendsTitleDescriptionSolutionTypeAndAreaToTheAi() throws Exception {
        when(aiClient.index(any()))
                .thenReturn(new AiResult.Success<>(new EmbeddingResponse(true, "modelo-simulado", 384)));

        resolve().andExpect(status().isOk());

        ArgumentCaptor<EmbeddingRequest> captor = ArgumentCaptor.forClass(EmbeddingRequest.class);
        verify(aiClient).index(captor.capture());
        EmbeddingRequest sent = captor.getValue();
        assertThat(sent.incidentId()).isEqualTo(incident.getId());
        assertThat(sent.title()).isEqualTo("T32 en progreso");
        assertThat(sent.description()).isEqualTo("Descripción");
        assertThat(sent.solution()).isEqualTo(SOLUTION);
        assertThat(sent.type()).isEqualTo("BUG");
        assertThat(sent.area()).isEqualTo("APPLICATIONS");
    }

    @Test
    void staysResolvedWithTheSolutionButUnindexedWhenTheIndexingFails() throws Exception {
        when(aiClient.index(any())).thenReturn(new AiResult.Unavailable<>("HttpTimeoutException"));

        resolve()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.solution").value(SOLUTION));

        Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
        assertThat(saved.getSolution()).isEqualTo(SOLUTION);
        assertThat(saved.getClosedAt()).isNotNull();
        assertThat(saved.isIndexed()).isFalse();
        assertThat(actions())
                .containsExactly(IncidentEventAction.RESOLVED, IncidentEventAction.INDEXING_FAILED);
    }

    @Test
    void staysResolvedButUnindexedWhenTheAiRejectsOrDoesNotConfirm() throws Exception {
        when(aiClient.index(any()))
                .thenReturn(new AiResult.Rejected<>("AI_REJECTED", "rechazado"));

        resolve().andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVED"));

        assertThat(incidentRepository.findById(incident.getId()).orElseThrow().isIndexed()).isFalse();

        // Respuesta 200 sin confirmación de indexado: tampoco se marca.
        Incident other = new Incident();
        other.setTitle("T32 otro");
        other.setDescription("Descripción");
        other.setStatus(IncidentStatus.IN_PROGRESS);
        other.setClassificationStatus(ClassificationStatus.AI_CLASSIFIED);
        other.setType(IncidentType.BUG);
        other.setPriority(Priority.HIGH);
        other.setArea(Area.APPLICATIONS);
        other.setCreatedBy(incident.getCreatedBy());
        other.setAssignedTo(technician);
        other.setCreatedAt(Instant.now());
        other = incidentRepository.saveAndFlush(other);
        when(aiClient.index(any()))
                .thenReturn(new AiResult.Success<>(new EmbeddingResponse(false, "modelo-simulado", 384)));

        mockMvc.perform(
                        post("/api/incidents/{id}/resolution", other.getId())
                                .header(USER_HEADER, technician.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"solution\": \"" + SOLUTION + "\"}"))
                .andExpect(status().isOk());

        assertThat(incidentRepository.findById(other.getId()).orElseThrow().isIndexed()).isFalse();
    }

    @Test
    void doesNotIndexWhenTheSolutionIsTooShort() throws Exception {
        mockMvc.perform(
                        post("/api/incidents/{id}/resolution", incident.getId())
                                .header(USER_HEADER, technician.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"solution\": \"corta\"}"))
                .andExpect(status().isUnprocessableEntity());

        verify(aiClient, never()).index(any());
        assertThat(incidentRepository.findById(incident.getId()).orElseThrow().isIndexed()).isFalse();
    }
}
