package com.corporativoTI.SistemaInteligenteTI.incidents.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.corporativoTI.SistemaInteligenteTI.catalog.model.Area;
import com.corporativoTI.SistemaInteligenteTI.catalog.model.IncidentType;
import com.corporativoTI.SistemaInteligenteTI.catalog.model.Priority;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.ClassificationStatus;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.Incident;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.IncidentAction;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.IncidentStateMachine;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.IncidentStatus;
import com.corporativoTI.SistemaInteligenteTI.incidents.repository.IncidentRepository;
import com.corporativoTI.SistemaInteligenteTI.users.model.Role;
import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import com.corporativoTI.SistemaInteligenteTI.users.repository.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * RF-7 y RF-12 (T-21): ante dos acciones simultáneas sobre el mismo incidente gana una sola y la
 * otra recibe 409. El choque se simula de verdad: tras la lectura de la petición, otra transacción
 * confirma su cambio (sube {@code version}), así que el guardado de la petición pierde la carrera.
 *
 * <p>No es {@code @Transactional}: el conflicto de versión solo aparece al confirmar la transacción
 * del caso de uso, por eso los datos se crean confirmados y se limpian al final.
 */
@SpringBootTest
@AutoConfigureMockMvc
class IncidentConcurrencyTest {

    private static final String USER_HEADER = "X-User-Id";
    private static final String SOLUTION = "Se reinició el servicio y se verificó el funcionamiento correcto.";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;

    @Autowired private IncidentRepository incidentRepository;
    @MockitoSpyBean private IncidentStateMachine stateMachine;

    private User supervisor;
    private User technicianA;
    private User technicianB;
    private Incident incident;

    @BeforeEach
    void setUp() {
        supervisor = userRepository.findByUsername("laura.supervisora").orElseThrow();
        technicianA = technician("tecnico.t21.a");
        technicianB = technician("tecnico.t21.b");
    }

    @AfterEach
    void cleanUp() {
        Mockito.reset(stateMachine);
        if (incident != null) {
            incidentRepository.deleteById(incident.getId());
        }
        userRepository.deleteById(technicianA.getId());
        userRepository.deleteById(technicianB.getId());
    }

    private User technician(String username) {
        User user = new User();
        user.setUsername(username);
        user.setFullName("Técnico " + username);
        user.setRole(Role.TECHNICIAN);
        user.setArea(Area.APPLICATIONS);
        return userRepository.save(user);
    }

    private Incident persistIncident(IncidentStatus status) {
        Incident newIncident = new Incident();
        newIncident.setTitle("T21 incidente");
        newIncident.setDescription("Descripción");
        newIncident.setStatus(status);
        newIncident.setClassificationStatus(ClassificationStatus.AI_CLASSIFIED);
        newIncident.setType(IncidentType.BUG);
        newIncident.setPriority(Priority.HIGH);
        newIncident.setArea(Area.APPLICATIONS);
        newIncident.setCreatedBy(supervisor);
        newIncident.setCreatedAt(Instant.now());
        if (status != IncidentStatus.REGISTERED) {
            newIncident.setAssignedTo(technicianA);
            newIncident.setAssignedBy(supervisor);
            newIncident.setAssignedAt(Instant.now());
        }
        if (status == IncidentStatus.IN_PROGRESS) {
            newIncident.setStartedAt(Instant.now());
        }
        return incidentRepository.saveAndFlush(newIncident);
    }

    /**
     * Hace que la otra "petición simultánea" gane: justo después de que la petición lea el incidente
     * y decida la transición (y antes de guardar), confirma su propia asignación a {@code winner} y
     * sube la versión.
     */
    private void otherRequestCommitsAfterRead(Long incidentId, User winner) {
        Mockito.doAnswer(
                        invocation -> {
                            Object target = invocation.callRealMethod();
                            // Transacción independiente: la otra petición confirma de verdad.
                            TransactionTemplate otherRequest = new TransactionTemplate(transactionManager);
                            otherRequest.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                            otherRequest.executeWithoutResult(
                                    tx ->
                                            jdbcTemplate.update(
                                                    "UPDATE incidents SET status = 'ASSIGNED', assigned_to = ?,"
                                                            + " assigned_by = ?, assigned_at = now(),"
                                                            + " started_at = NULL, version = version + 1"
                                                            + " WHERE id = ?",
                                                    winner.getId(),
                                                    supervisor.getId(),
                                                    incidentId));
                            return target;
                        })
                .when(stateMachine)
                .transition(any(IncidentStatus.class), any(IncidentAction.class));
    }

    @Test
    void secondConcurrentAssignmentGets409AndTheWinnerStays() throws Exception {
        incident = persistIncident(IncidentStatus.REGISTERED);
        otherRequestCommitsAfterRead(incident.getId(), technicianB);

        mockMvc.perform(
                        put("/api/incidents/{id}/assignment", incident.getId())
                                .header(USER_HEADER, supervisor.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"technicianId\":" + technicianA.getId() + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONCURRENT_MODIFICATION"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        Mockito.reset(stateMachine);
        Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(saved.getAssignedTo().getId()).isEqualTo(technicianB.getId());
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
    }

    @Test
    void lateResolutionAfterAConcurrentChangeGets409AndSavesNothing() throws Exception {
        incident = persistIncident(IncidentStatus.IN_PROGRESS);
        // Mientras el técnico A resuelve, el supervisor reasigna a B y confirma antes.
        otherRequestCommitsAfterRead(incident.getId(), technicianB);

        mockMvc.perform(
                        post("/api/incidents/{id}/resolution", incident.getId())
                                .header(USER_HEADER, technicianA.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"solution\":\"" + SOLUTION + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONCURRENT_MODIFICATION"));

        Mockito.reset(stateMachine);
        Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
        assertThat(saved.getSolution()).isNull();
        assertThat(saved.getClosedAt()).isNull();
        assertThat(saved.getAssignedTo().getId()).isEqualTo(technicianB.getId());
    }

    @Test
    void requestWithoutConflictStillSucceeds() throws Exception {
        incident = persistIncident(IncidentStatus.REGISTERED);

        mockMvc.perform(
                        put("/api/incidents/{id}/assignment", incident.getId())
                                .header(USER_HEADER, supervisor.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"technicianId\":" + technicianA.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }
}
