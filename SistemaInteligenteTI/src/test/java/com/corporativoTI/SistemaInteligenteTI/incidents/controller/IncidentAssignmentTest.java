package com.corporativoTI.SistemaInteligenteTI.incidents.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * RF-7 (T-15): {@code PUT /api/incidents/{id}/assignment} pasa el incidente a Asignado y registra
 * técnico, fecha y supervisor; un técnico de otra área se rechaza con motivo, y un incidente
 * pendiente de clasificación no se puede asignar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IncidentAssignmentTest {

    private static final String USER_HEADER = "X-User-Id";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private IncidentRepository incidentRepository;

    private User supervisor;
    private User applicationsTechnician;
    private User databaseTechnician;

    @BeforeEach
    void setUp() {
        supervisor = userRepository.findByUsername("laura.supervisora").orElseThrow();
        applicationsTechnician = technician("tecnico.t15.apps", Area.APPLICATIONS);
        databaseTechnician = technician("tecnico.t15.db", Area.DATABASE);
    }

    private User technician(String username, Area area) {
        User user = new User();
        user.setUsername(username);
        user.setFullName("Técnico " + username);
        user.setRole(Role.TECHNICIAN);
        user.setArea(area);
        return userRepository.save(user);
    }

    private Incident incident(ClassificationStatus classification, IncidentStatus status) {
        Incident incident = new Incident();
        incident.setTitle("T15 incidente");
        incident.setDescription("Descripción");
        incident.setStatus(status);
        incident.setClassificationStatus(classification);
        incident.setCreatedBy(supervisor);
        incident.setCreatedAt(Instant.now());
        if (classification != ClassificationStatus.PENDING) {
            incident.setType(IncidentType.BUG);
            incident.setPriority(Priority.HIGH);
            incident.setArea(Area.APPLICATIONS);
        }
        if (status != IncidentStatus.REGISTERED) {
            incident.setAssignedTo(applicationsTechnician);
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

    private Incident classifiedRegistered() {
        return incident(ClassificationStatus.AI_CLASSIFIED, IncidentStatus.REGISTERED);
    }

    private ResultActions assign(User actor, Long id, String json) throws Exception {
        return mockMvc.perform(
                put("/api/incidents/{id}/assignment", id)
                        .header(USER_HEADER, actor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json));
    }

    private static String body(User technician) {
        return "{\"technicianId\": " + technician.getId() + "}";
    }

    @Test
    void assignsClassifiedIncidentToTechnicianOfItsArea() throws Exception {
        Incident incident = classifiedRegistered();

        assign(supervisor, incident.getId(), body(applicationsTechnician))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incident.getId()))
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.assignedTo").value(applicationsTechnician.getId()))
                .andExpect(jsonPath("$.assignedBy").value(supervisor.getId()))
                .andExpect(jsonPath("$.assignedAt").isNotEmpty());

        incidentRepository.flush();
        Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
        assertThat(saved.getAssignedTo().getId()).isEqualTo(applicationsTechnician.getId());
        assertThat(saved.getAssignedBy().getId()).isEqualTo(supervisor.getId());
        assertThat(saved.getAssignedAt()).isNotNull();
    }

    @Test
    void manuallyClassifiedIncidentCanBeAssigned() throws Exception {
        Incident incident = incident(ClassificationStatus.MANUALLY_CORRECTED, IncidentStatus.REGISTERED);

        assign(supervisor, incident.getId(), body(applicationsTechnician))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    @Test
    void rejectsTechnicianFromAnotherAreaWithReason() throws Exception {
        Incident incident = classifiedRegistered();

        assign(supervisor, incident.getId(), body(databaseTechnician))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("TECHNICIAN_AREA_MISMATCH"))
                .andExpect(jsonPath("$.message").value(containsString("APPLICATIONS")));

        Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.REGISTERED);
        assertThat(saved.getAssignedTo()).isNull();
        assertThat(saved.getAssignedBy()).isNull();
        assertThat(saved.getAssignedAt()).isNull();
    }

    @Test
    void pendingClassificationIncidentCannotBeAssigned() throws Exception {
        Incident incident = incident(ClassificationStatus.PENDING, IncidentStatus.REGISTERED);

        assign(supervisor, incident.getId(), body(applicationsTechnician))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INCIDENT_NOT_CLASSIFIED"))
                .andExpect(jsonPath("$.message").value(containsString("clasific")));

        Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.REGISTERED);
        assertThat(saved.getAssignedTo()).isNull();
    }

    @Test
    void closedIncidentsCannotBeAssigned() throws Exception {
        for (IncidentStatus finalStatus :
                new IncidentStatus[] {IncidentStatus.RESOLVED, IncidentStatus.CLOSED_WITHOUT_SOLUTION}) {
            Incident incident = incident(ClassificationStatus.AI_CLASSIFIED, finalStatus);

            assign(supervisor, incident.getId(), body(applicationsTechnician))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("INCIDENT_CLOSED"));
        }
    }

    @Test
    void technicianCannotAssign() throws Exception {
        Incident incident = classifiedRegistered();

        assign(applicationsTechnician, incident.getId(), body(applicationsTechnician))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN_ROLE"));

        assertThat(incidentRepository.findById(incident.getId()).orElseThrow().getStatus())
                .isEqualTo(IncidentStatus.REGISTERED);
    }

    @Test
    void rejectsMissingTechnicianIdIndicatingTheField() throws Exception {
        Incident incident = classifiedRegistered();

        assign(supervisor, incident.getId(), "{}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("MISSING_FIELD"))
                .andExpect(jsonPath("$.message").value(containsString("technicianId")));
    }

    @Test
    void rejectsUnknownTechnician() throws Exception {
        Incident incident = classifiedRegistered();

        assign(supervisor, incident.getId(), "{\"technicianId\": 999999999}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TECHNICIAN"));
    }

    @Test
    void rejectsAssigningToASupervisor() throws Exception {
        Incident incident = classifiedRegistered();

        assign(supervisor, incident.getId(), body(supervisor))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TECHNICIAN"));
    }

    @Test
    void unknownIncidentReturns404() throws Exception {
        assign(supervisor, 999_999_999L, body(applicationsTechnician))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INCIDENT_NOT_FOUND"));
    }

    @Test
    void requestWithoutActiveUserReturns400() throws Exception {
        mockMvc.perform(
                        put("/api/incidents/1/assignment")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body(applicationsTechnician)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_ACTIVE_USER"));
    }
}
