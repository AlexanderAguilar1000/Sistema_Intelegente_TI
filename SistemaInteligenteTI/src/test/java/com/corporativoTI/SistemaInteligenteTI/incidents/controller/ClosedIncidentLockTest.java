package com.corporativoTI.SistemaInteligenteTI.incidents.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-12 y RF-14 (T-20): corregir la clasificación, reasignar, iniciar, resolver o cerrar un
 * incidente ya cerrado devuelve 409 en todos los casos, sobre los dos estados finales.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ClosedIncidentLockTest {

    private static final String USER_HEADER = "X-User-Id";
    private static final String SOLUTION = "Se reinició el servicio y se verificó el funcionamiento correcto.";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private IncidentRepository incidentRepository;

    private User supervisor;
    private User assignedTechnician;
    private User newTechnician;

    @BeforeEach
    void setUp() {
        supervisor = userRepository.findByUsername("laura.supervisora").orElseThrow();
        assignedTechnician = technician("tecnico.t20.asignado");
        newTechnician = technician("tecnico.t20.nuevo");
    }

    private User technician(String username) {
        User user = new User();
        user.setUsername(username);
        user.setFullName("Técnico " + username);
        user.setRole(Role.TECHNICIAN);
        user.setArea(Area.APPLICATIONS);
        return userRepository.save(user);
    }

    private Incident closedIncident(IncidentStatus finalStatus) {
        Incident incident = new Incident();
        incident.setTitle("T20 incidente");
        incident.setDescription("Descripción");
        incident.setStatus(finalStatus);
        incident.setClassificationStatus(ClassificationStatus.AI_CLASSIFIED);
        incident.setType(IncidentType.BUG);
        incident.setPriority(Priority.HIGH);
        incident.setArea(Area.APPLICATIONS);
        incident.setCreatedBy(supervisor);
        incident.setCreatedAt(Instant.now());
        incident.setAssignedTo(assignedTechnician);
        incident.setAssignedBy(supervisor);
        incident.setAssignedAt(Instant.now());
        incident.setStartedAt(Instant.now());
        incident.setClosedAt(Instant.now());
        if (finalStatus == IncidentStatus.RESOLVED) {
            incident.setSolution(SOLUTION);
        } else {
            incident.setClosureReason(ClosureReason.DISCARDED);
        }
        return incidentRepository.saveAndFlush(incident);
    }

    /** Cada acción prohibida sobre un cerrado, con el usuario que sí podría pedirla. */
    private MockHttpServletRequestBuilder action(String name, Long id) {
        return switch (name) {
            case "classify" ->
                    patch("/api/incidents/{id}/classification", id)
                            .header(USER_HEADER, supervisor.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"type\":\"NETWORK\",\"priority\":\"LOW\",\"area\":\"APPLICATIONS\"}");
            case "reassign" ->
                    put("/api/incidents/{id}/assignment", id)
                            .header(USER_HEADER, supervisor.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"technicianId\":" + newTechnician.getId() + "}");
            case "start" -> post("/api/incidents/{id}/start", id).header(USER_HEADER, assignedTechnician.getId());
            case "resolve" ->
                    post("/api/incidents/{id}/resolution", id)
                            .header(USER_HEADER, assignedTechnician.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"solution\":\"" + SOLUTION + "\"}");
            case "close" ->
                    post("/api/incidents/{id}/closure", id)
                            .header(USER_HEADER, assignedTechnician.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"DUPLICATE\"}");
            default -> throw new IllegalArgumentException(name);
        };
    }

    static Stream<Arguments> actionsOnFinalStates() {
        Stream.Builder<Arguments> cases = Stream.builder();
        for (IncidentStatus finalStatus :
                new IncidentStatus[] {IncidentStatus.RESOLVED, IncidentStatus.CLOSED_WITHOUT_SOLUTION}) {
            for (String action : new String[] {"classify", "reassign", "start", "resolve", "close"}) {
                cases.add(Arguments.of(finalStatus, action));
            }
        }
        return cases.build();
    }

    @ParameterizedTest(name = "{1} sobre {0} devuelve 409")
    @MethodSource("actionsOnFinalStates")
    void actionOnClosedIncidentReturns409AndChangesNothing(IncidentStatus finalStatus, String action)
            throws Exception {
        Incident incident = closedIncident(finalStatus);
        Instant closedAt = incident.getClosedAt();

        mockMvc.perform(action(action, incident.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INCIDENT_CLOSED"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        incidentRepository.flush();
        Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(finalStatus);
        assertThat(saved.getClosedAt()).isEqualTo(closedAt);
        assertThat(saved.getType()).isEqualTo(IncidentType.BUG);
        assertThat(saved.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(saved.getAssignedTo().getId()).isEqualTo(assignedTechnician.getId());
    }
}
