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
 * RF-12 (T-18): {@code POST /api/incidents/{id}/resolution} con 30 caracteres o más pasa a Resuelto
 * y guarda solución, fecha y técnico; con menos de 30 devuelve 422; si no está En progreso devuelve
 * 409.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IncidentResolutionTest {

    private static final String USER_HEADER = "X-User-Id";
    private static final String VALID_SOLUTION =
            "Se reinició el servicio y se verificó el funcionamiento correcto.";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private IncidentRepository incidentRepository;

    private User supervisor;
    private User assignedTechnician;
    private User otherTechnician;

    @BeforeEach
    void setUp() {
        supervisor = userRepository.findByUsername("laura.supervisora").orElseThrow();
        assignedTechnician = technician("tecnico.t18.asignado");
        otherTechnician = technician("tecnico.t18.otro");
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
        incident.setTitle("T18 incidente");
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
            incident.setSolution(VALID_SOLUTION);
            incident.setClosedAt(Instant.now());
        }
        if (status == IncidentStatus.CLOSED_WITHOUT_SOLUTION) {
            incident.setClosureReason(ClosureReason.DISCARDED);
            incident.setClosedAt(Instant.now());
        }
        return incidentRepository.saveAndFlush(incident);
    }

    private ResultActions resolve(User actor, Long id, String solution) throws Exception {
        String body = solution == null ? "{}" : "{\"solution\":\"" + solution + "\"}";
        return mockMvc.perform(
                post("/api/incidents/{id}/resolution", id)
                        .header(USER_HEADER, actor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
    }

    @Test
    void assignedTechnicianResolvesWithValidSolution() throws Exception {
        Incident incident = incident(IncidentStatus.IN_PROGRESS);

        resolve(assignedTechnician, incident.getId(), VALID_SOLUTION)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incident.getId()))
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.solution").value(VALID_SOLUTION))
                .andExpect(jsonPath("$.assignedTo").value(assignedTechnician.getId()))
                .andExpect(jsonPath("$.closedAt").isNotEmpty());

        incidentRepository.flush();
        Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
        assertThat(saved.getSolution()).isEqualTo(VALID_SOLUTION);
        assertThat(saved.getClosedAt()).isNotNull();
        assertThat(saved.getAssignedTo().getId()).isEqualTo(assignedTechnician.getId());
    }

    @Test
    void solutionOfExactlyThirtyCharactersIsAccepted() throws Exception {
        Incident incident = incident(IncidentStatus.IN_PROGRESS);
        String thirty = "a".repeat(30);

        resolve(assignedTechnician, incident.getId(), thirty)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.solution").value(thirty));
    }

    @Test
    void solutionShorterThanThirtyCharactersReturns422() throws Exception {
        Incident incident = incident(IncidentStatus.IN_PROGRESS);

        resolve(assignedTechnician, incident.getId(), "a".repeat(29))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("SOLUTION_TOO_SHORT"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(saved.getSolution()).isNull();
        assertThat(saved.getClosedAt()).isNull();
    }

    @Test
    void missingSolutionReturns422() throws Exception {
        Incident incident = incident(IncidentStatus.IN_PROGRESS);

        resolve(assignedTechnician, incident.getId(), null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("MISSING_FIELD"));

        assertThat(incidentRepository.findById(incident.getId()).orElseThrow().getStatus())
                .isEqualTo(IncidentStatus.IN_PROGRESS);
    }

    @Test
    void incidentNotInProgressReturns409() throws Exception {
        Incident incident = incident(IncidentStatus.ASSIGNED);

        resolve(assignedTechnician, incident.getId(), VALID_SOLUTION)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TRANSITION"));

        Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
        assertThat(saved.getSolution()).isNull();
    }

    @Test
    void closedIncidentsReturn409() throws Exception {
        for (IncidentStatus finalStatus :
                new IncidentStatus[] {IncidentStatus.RESOLVED, IncidentStatus.CLOSED_WITHOUT_SOLUTION}) {
            Incident incident = incident(finalStatus);

            resolve(assignedTechnician, incident.getId(), VALID_SOLUTION)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("INCIDENT_CLOSED"));
        }
    }

    @Test
    void anotherTechnicianCannotResolveIt() throws Exception {
        Incident incident = incident(IncidentStatus.IN_PROGRESS);

        resolve(otherTechnician, incident.getId(), VALID_SOLUTION)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN_ROLE"));

        assertThat(incidentRepository.findById(incident.getId()).orElseThrow().getStatus())
                .isEqualTo(IncidentStatus.IN_PROGRESS);
    }

    @Test
    void supervisorCannotResolveIt() throws Exception {
        Incident incident = incident(IncidentStatus.IN_PROGRESS);

        resolve(supervisor, incident.getId(), VALID_SOLUTION)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN_ROLE"));

        assertThat(incidentRepository.findById(incident.getId()).orElseThrow().getStatus())
                .isEqualTo(IncidentStatus.IN_PROGRESS);
    }

    @Test
    void unknownIncidentReturns404() throws Exception {
        resolve(assignedTechnician, 999_999_999L, VALID_SOLUTION)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INCIDENT_NOT_FOUND"));
    }

    @Test
    void requestWithoutActiveUserReturns400() throws Exception {
        mockMvc.perform(
                        post("/api/incidents/1/resolution")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_ACTIVE_USER"));
    }
}
