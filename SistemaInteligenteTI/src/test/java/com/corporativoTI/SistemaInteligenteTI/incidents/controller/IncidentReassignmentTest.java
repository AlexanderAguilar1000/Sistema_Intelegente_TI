package com.corporativoTI.SistemaInteligenteTI.incidents.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 * RF-6 y RF-7 (T-16): reasignar a otro técnico del área deja el incidente en Asignado con el nuevo
 * responsable; cambiar el área a una que excluye al técnico actual lo devuelve a Registrado sin
 * técnico; el técnico anterior que intenta actuar recibe 403.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IncidentReassignmentTest {

    private static final String USER_HEADER = "X-User-Id";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private IncidentRepository incidentRepository;

    private User supervisor;
    private User otherSupervisor;
    private User firstTechnician;
    private User secondTechnician;
    private User databaseTechnician;

    @BeforeEach
    void setUp() {
        supervisor = userRepository.findByUsername("laura.supervisora").orElseThrow();
        otherSupervisor = user("supervisor.t16", Role.SUPERVISOR, null);
        firstTechnician = user("tecnico.t16.uno", Role.TECHNICIAN, Area.APPLICATIONS);
        secondTechnician = user("tecnico.t16.dos", Role.TECHNICIAN, Area.APPLICATIONS);
        databaseTechnician = user("tecnico.t16.db", Role.TECHNICIAN, Area.DATABASE);
    }

    private User user(String username, Role role, Area area) {
        User user = new User();
        user.setUsername(username);
        user.setFullName("Usuario " + username);
        user.setRole(role);
        user.setArea(area);
        return userRepository.save(user);
    }

    /** Incidente de área APPLICATIONS ya asignado a {@code firstTechnician}. */
    private Incident assignedIncident(IncidentStatus status) {
        Incident incident = new Incident();
        incident.setTitle("T16 incidente");
        incident.setDescription("Descripción");
        incident.setStatus(status);
        incident.setClassificationStatus(ClassificationStatus.AI_CLASSIFIED);
        incident.setType(IncidentType.BUG);
        incident.setPriority(Priority.HIGH);
        incident.setArea(Area.APPLICATIONS);
        incident.setCreatedBy(supervisor);
        incident.setCreatedAt(Instant.now());
        incident.setAssignedTo(firstTechnician);
        incident.setAssignedBy(supervisor);
        incident.setAssignedAt(Instant.now());
        if (status == IncidentStatus.IN_PROGRESS) {
            incident.setStartedAt(Instant.now());
        }
        return incidentRepository.saveAndFlush(incident);
    }

    private ResultActions assign(User actor, Long id, User technician) throws Exception {
        return mockMvc.perform(
                put("/api/incidents/{id}/assignment", id)
                        .header(USER_HEADER, actor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"technicianId\": " + technician.getId() + "}"));
    }

    private ResultActions classify(Long id, String json) throws Exception {
        return mockMvc.perform(
                patch("/api/incidents/{id}/classification", id)
                        .header(USER_HEADER, supervisor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json));
    }

    private ResultActions viewAs(User actor, Long id) throws Exception {
        return mockMvc.perform(get("/api/incidents/{id}", id).header(USER_HEADER, actor.getId()));
    }

    @Test
    void reassignsAssignedIncidentToAnotherTechnicianOfTheArea() throws Exception {
        Incident incident = assignedIncident(IncidentStatus.ASSIGNED);

        assign(otherSupervisor, incident.getId(), secondTechnician)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.assignedTo").value(secondTechnician.getId()))
                .andExpect(jsonPath("$.assignedBy").value(otherSupervisor.getId()));

        incidentRepository.flush();
        Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
        assertThat(saved.getAssignedTo().getId()).isEqualTo(secondTechnician.getId());
        assertThat(saved.getAssignedBy().getId()).isEqualTo(otherSupervisor.getId());
    }

    @Test
    void reassigningAnInProgressIncidentReturnsItToAssigned() throws Exception {
        Incident incident = assignedIncident(IncidentStatus.IN_PROGRESS);

        assign(supervisor, incident.getId(), secondTechnician)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.assignedTo").value(secondTechnician.getId()))
                .andExpect(jsonPath("$.startedAt").doesNotExist());
    }

    @Test
    void reassignmentToATechnicianOfAnotherAreaIsRejectedAndKeepsTheCurrentOne() throws Exception {
        Incident incident = assignedIncident(IncidentStatus.ASSIGNED);

        assign(supervisor, incident.getId(), databaseTechnician)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("TECHNICIAN_AREA_MISMATCH"));

        Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(saved.getAssignedTo().getId()).isEqualTo(firstTechnician.getId());
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
    }

    @Test
    void previousTechnicianLosesAccessAfterReassignment() throws Exception {
        Incident incident = assignedIncident(IncidentStatus.ASSIGNED);
        viewAs(firstTechnician, incident.getId()).andExpect(status().isOk());

        assign(supervisor, incident.getId(), secondTechnician).andExpect(status().isOk());

        viewAs(firstTechnician, incident.getId())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN_ROLE"));
        viewAs(secondTechnician, incident.getId()).andExpect(status().isOk());
    }

    @Test
    void changingToAnAreaThatExcludesTheTechnicianReturnsTheIncidentToRegistered() throws Exception {
        Incident incident = assignedIncident(IncidentStatus.ASSIGNED);

        classify(incident.getId(), "{\"type\": \"BUG\", \"priority\": \"HIGH\", \"area\": \"DATABASE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REGISTERED"))
                .andExpect(jsonPath("$.area").value("DATABASE"))
                .andExpect(jsonPath("$.classificationStatus").value("MANUALLY_CORRECTED"))
                .andExpect(jsonPath("$.assignedTo").doesNotExist())
                .andExpect(jsonPath("$.assignedBy").doesNotExist())
                .andExpect(jsonPath("$.assignedAt").doesNotExist());

        incidentRepository.flush();
        Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.REGISTERED);
        assertThat(saved.getAssignedTo()).isNull();
        assertThat(saved.getAssignedBy()).isNull();
        assertThat(saved.getAssignedAt()).isNull();
    }

    @Test
    void areaChangeOnAnInProgressIncidentAlsoWithdrawsTheAssignment() throws Exception {
        Incident incident = assignedIncident(IncidentStatus.IN_PROGRESS);

        classify(incident.getId(), "{\"type\": \"BUG\", \"priority\": \"HIGH\", \"area\": \"SECURITY\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REGISTERED"))
                .andExpect(jsonPath("$.assignedTo").doesNotExist())
                .andExpect(jsonPath("$.startedAt").doesNotExist());
    }

    @Test
    void previousTechnicianLosesAccessAfterTheAssignmentIsWithdrawn() throws Exception {
        Incident incident = assignedIncident(IncidentStatus.ASSIGNED);

        classify(incident.getId(), "{\"type\": \"BUG\", \"priority\": \"HIGH\", \"area\": \"DATABASE\"}")
                .andExpect(status().isOk());

        viewAs(firstTechnician, incident.getId())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("asignados")));
    }

    @Test
    void reclassifyingWithinTheSameAreaKeepsTheAssignment() throws Exception {
        Incident incident = assignedIncident(IncidentStatus.IN_PROGRESS);

        classify(incident.getId(), "{\"type\": \"ACCESS\", \"priority\": \"LOW\", \"area\": \"APPLICATIONS\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.assignedTo").value(firstTechnician.getId()));
    }
}
