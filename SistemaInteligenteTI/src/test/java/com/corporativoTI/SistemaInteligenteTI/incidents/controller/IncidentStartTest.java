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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-10 (T-17): {@code POST /api/incidents/{id}/start} pasa a En progreso solo si lo pide el
 * técnico asignado; otro técnico recibe 403 y un estado incorrecto 409.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IncidentStartTest {

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
        assignedTechnician = technician("tecnico.t17.asignado");
        otherTechnician = technician("tecnico.t17.otro");
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
        incident.setTitle("T17 incidente");
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
        if (status == IncidentStatus.IN_PROGRESS) {
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

    private ResultActions start(User actor, Long id) throws Exception {
        return mockMvc.perform(post("/api/incidents/{id}/start", id).header(USER_HEADER, actor.getId()));
    }

    @Test
    void assignedTechnicianStartsTheWork() throws Exception {
        Incident incident = incident(IncidentStatus.ASSIGNED);

        start(assignedTechnician, incident.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incident.getId()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.assignedTo").value(assignedTechnician.getId()))
                .andExpect(jsonPath("$.startedAt").isNotEmpty());

        incidentRepository.flush();
        Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(saved.getStartedAt()).isNotNull();
    }

    @Test
    void anotherTechnicianCannotStartIt() throws Exception {
        Incident incident = incident(IncidentStatus.ASSIGNED);

        start(otherTechnician, incident.getId())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN_ROLE"));

        Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
        assertThat(saved.getStartedAt()).isNull();
    }

    @Test
    void supervisorCannotStartIt() throws Exception {
        Incident incident = incident(IncidentStatus.ASSIGNED);

        start(supervisor, incident.getId())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN_ROLE"));

        assertThat(incidentRepository.findById(incident.getId()).orElseThrow().getStatus())
                .isEqualTo(IncidentStatus.ASSIGNED);
    }

    @Test
    void incidentAlreadyInProgressReturns409() throws Exception {
        Incident incident = incident(IncidentStatus.IN_PROGRESS);

        start(assignedTechnician, incident.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TRANSITION"));
    }

    @Test
    void unassignedIncidentCannotBeStarted() throws Exception {
        Incident incident = incident(IncidentStatus.REGISTERED);

        start(assignedTechnician, incident.getId()).andExpect(status().isForbidden());

        assertThat(incidentRepository.findById(incident.getId()).orElseThrow().getStatus())
                .isEqualTo(IncidentStatus.REGISTERED);
    }

    @Test
    void closedIncidentsReturn409() throws Exception {
        for (IncidentStatus finalStatus :
                new IncidentStatus[] {IncidentStatus.RESOLVED, IncidentStatus.CLOSED_WITHOUT_SOLUTION}) {
            Incident incident = incident(finalStatus);

            start(assignedTechnician, incident.getId())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("INCIDENT_CLOSED"));
        }
    }

    @Test
    void unknownIncidentReturns404() throws Exception {
        start(assignedTechnician, 999_999_999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INCIDENT_NOT_FOUND"));
    }

    @Test
    void requestWithoutActiveUserReturns400() throws Exception {
        mockMvc.perform(post("/api/incidents/1/start"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_ACTIVE_USER"));
    }
}
