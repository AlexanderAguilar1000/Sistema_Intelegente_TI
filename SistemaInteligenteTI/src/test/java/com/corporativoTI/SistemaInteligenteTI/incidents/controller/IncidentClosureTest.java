package com.corporativoTI.SistemaInteligenteTI.incidents.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-14 (T-19): {@code POST /api/incidents/{id}/closure} con motivo (duplicado, no reproducible o
 * descartado) pasa a Cerrado sin solución y no exige texto de solución; sin motivo devuelve 422.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IncidentClosureTest {

    private static final String USER_HEADER = "X-User-Id";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private IncidentRepository incidentRepository;

    private User supervisor;
    private User assignedTechnician;
    private User otherTechnician;

    @BeforeEach
    void setUp() {
        supervisor = userRepository.findByUsername("laura.supervisora").orElseThrow();
        assignedTechnician = technician("tecnico.t19.asignado");
        otherTechnician = technician("tecnico.t19.otro");
    }

    private User technician(String username) {
        User user = new User();
        user.setUsername(username);
        user.setFullName("Técnico " + username);
        user.setRole(Role.TECHNICIAN);
        user.setArea(Area.APPLICATIONS);
        return userRepository.save(user);
    }

    private Incident incident(IncidentStatus status) {
        Incident incident = new Incident();
        incident.setTitle("T19 incidente");
        incident.setDescription("Descripción");
        incident.setStatus(status);
        incident.setClassificationStatus(ClassificationStatus.AI_CLASSIFIED);
        incident.setType(IncidentType.BUG);
        incident.setPriority(Priority.HIGH);
        incident.setArea(Area.APPLICATIONS);
        incident.setCreatedBy(supervisor);
        incident.setCreatedAt(Instant.now());
        if (status != IncidentStatus.REGISTERED) {
            incident.setAssignedTo(assignedTechnician);
            incident.setAssignedBy(supervisor);
            incident.setAssignedAt(Instant.now());
        }
        if (status == IncidentStatus.IN_PROGRESS || status == IncidentStatus.RESOLVED) {
            incident.setStartedAt(Instant.now());
        }
        if (status == IncidentStatus.RESOLVED) {
            incident.setSolution("Se reinició el servicio y se verificó el funcionamiento correcto.");
            incident.setClosedAt(Instant.now());
        }
        if (status == IncidentStatus.CLOSED_WITHOUT_SOLUTION) {
            incident.setClosureReason(ClosureReason.DISCARDED);
            incident.setClosedAt(Instant.now());
        }
        return incidentRepository.saveAndFlush(incident);
    }

    private ResultActions close(User actor, Long id, String body) throws Exception {
        return mockMvc.perform(
                post("/api/incidents/{id}/closure", id)
                        .header(USER_HEADER, actor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
    }

    private static String reasonBody(String reason) {
        return "{\"reason\":\"" + reason + "\"}";
    }

    @Test
    void assignedTechnicianClosesInProgressIncidentWithEachReason() throws Exception {
        for (ClosureReason reason : ClosureReason.values()) {
            Incident incident = incident(IncidentStatus.IN_PROGRESS);

            close(assignedTechnician, incident.getId(), reasonBody(reason.name()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(incident.getId()))
                    .andExpect(jsonPath("$.status").value("CLOSED_WITHOUT_SOLUTION"))
                    .andExpect(jsonPath("$.closureReason").value(reason.name()))
                    .andExpect(jsonPath("$.solution").doesNotExist())
                    .andExpect(jsonPath("$.closedAt").isNotEmpty());

            incidentRepository.flush();
            Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
            assertThat(saved.getStatus()).isEqualTo(IncidentStatus.CLOSED_WITHOUT_SOLUTION);
            assertThat(saved.getClosureReason()).isEqualTo(reason);
            assertThat(saved.getSolution()).isNull();
            assertThat(saved.getClosedAt()).isNotNull();
        }
    }

    @Test
    void assignedTechnicianClosesAssignedIncident() throws Exception {
        Incident incident = incident(IncidentStatus.ASSIGNED);

        close(assignedTechnician, incident.getId(), reasonBody("DUPLICATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED_WITHOUT_SOLUTION"))
                .andExpect(jsonPath("$.closureReason").value("DUPLICATE"));
    }

    @Test
    void missingReasonReturns422() throws Exception {
        Incident incident = incident(IncidentStatus.IN_PROGRESS);

        close(assignedTechnician, incident.getId(), "{}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("MISSING_FIELD"));

        Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(saved.getClosureReason()).isNull();
        assertThat(saved.getClosedAt()).isNull();
    }

    @Test
    void reasonOutsideTheListReturns422() throws Exception {
        Incident incident = incident(IncidentStatus.IN_PROGRESS);

        close(assignedTechnician, incident.getId(), reasonBody("OTRO_MOTIVO"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CATALOG_VALUE"));

        assertThat(incidentRepository.findById(incident.getId()).orElseThrow().getStatus())
                .isEqualTo(IncidentStatus.IN_PROGRESS);
    }

    @Test
    void closedIncidentsReturn409() throws Exception {
        for (IncidentStatus finalStatus :
                new IncidentStatus[] {IncidentStatus.RESOLVED, IncidentStatus.CLOSED_WITHOUT_SOLUTION}) {
            Incident incident = incident(finalStatus);

            close(assignedTechnician, incident.getId(), reasonBody("DISCARDED"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("INCIDENT_CLOSED"));
        }
    }

    @Test
    void anotherTechnicianCannotCloseIt() throws Exception {
        Incident incident = incident(IncidentStatus.IN_PROGRESS);

        close(otherTechnician, incident.getId(), reasonBody("DISCARDED"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN_ROLE"));

        assertThat(incidentRepository.findById(incident.getId()).orElseThrow().getStatus())
                .isEqualTo(IncidentStatus.IN_PROGRESS);
    }

    @Test
    void supervisorCannotCloseIt() throws Exception {
        Incident incident = incident(IncidentStatus.IN_PROGRESS);

        close(supervisor, incident.getId(), reasonBody("DISCARDED"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN_ROLE"));
    }

    @Test
    void unassignedIncidentCannotBeClosedByATechnician() throws Exception {
        Incident incident = incident(IncidentStatus.REGISTERED);

        close(assignedTechnician, incident.getId(), reasonBody("DISCARDED"))
                .andExpect(status().isForbidden());

        assertThat(incidentRepository.findById(incident.getId()).orElseThrow().getStatus())
                .isEqualTo(IncidentStatus.REGISTERED);
    }

    @Test
    void unknownIncidentReturns404() throws Exception {
        close(assignedTechnician, 999_999_999L, reasonBody("DISCARDED"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INCIDENT_NOT_FOUND"));
    }

    @Test
    void requestWithoutActiveUserReturns400() throws Exception {
        mockMvc.perform(
                        post("/api/incidents/1/closure")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_ACTIVE_USER"));
    }
}
