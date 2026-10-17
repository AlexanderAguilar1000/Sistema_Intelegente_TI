package com.corporativoTI.SistemaInteligenteTI.incidents.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-8 (T-11): {@code GET /api/incidents} devuelve todos los incidentes al supervisor y solo los
 * suyos al técnico, en la misma llamada.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IncidentListControllerTest {

    private static final String USER_HEADER = "X-User-Id";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private IncidentRepository incidentRepository;

    private User supervisor;
    private User technicianA;
    private User technicianB;

    @BeforeEach
    void setUp() {
        supervisor = userRepository.findByUsername("laura.supervisora").orElseThrow();
        technicianA = createTechnician("tecnico.t11.a");
        technicianB = createTechnician("tecnico.t11.b");
    }

    private User createTechnician(String username) {
        User technician = new User();
        technician.setUsername(username);
        technician.setFullName("Técnico " + username);
        technician.setRole(Role.TECHNICIAN);
        technician.setArea(Area.APPLICATIONS);
        return userRepository.save(technician);
    }

    private Incident createIncident(String title, User assignedTo) {
        Incident incident = new Incident();
        incident.setTitle(title);
        incident.setDescription("Descripción de " + title);
        incident.setStatus(assignedTo == null ? IncidentStatus.REGISTERED : IncidentStatus.ASSIGNED);
        incident.setClassificationStatus(ClassificationStatus.PENDING);
        incident.setCreatedBy(supervisor);
        incident.setAssignedTo(assignedTo);
        incident.setCreatedAt(Instant.now());
        return incidentRepository.save(incident);
    }

    private ResultActions listAs(User actor) throws Exception {
        return mockMvc.perform(get("/api/incidents").header(USER_HEADER, actor.getId()));
    }

    @Test
    void supervisorSeesAllIncidents() throws Exception {
        createIncident("T11 sin asignar", null);
        createIncident("T11 de A", technicianA);
        createIncident("T11 de B", technicianB);

        listAs(supervisor)
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.incidents[*].title")
                                .value(hasItems("T11 sin asignar", "T11 de A", "T11 de B")));
    }

    @Test
    void technicianSeesOnlyItsOwnIncidents() throws Exception {
        Incident ofA = createIncident("T11 de A", technicianA);
        createIncident("T11 de B", technicianB);
        createIncident("T11 sin asignar", null);

        listAs(technicianA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents", hasSize(1)))
                .andExpect(jsonPath("$.incidents[0].id").value(ofA.getId()))
                .andExpect(jsonPath("$.incidents[0].title").value("T11 de A"));
    }

    @Test
    void eachTechnicianDoesNotSeeTheOthersIncidents() throws Exception {
        createIncident("T11 de A", technicianA);
        createIncident("T11 de B", technicianB);

        listAs(technicianB)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents", hasSize(1)))
                .andExpect(jsonPath("$.incidents[0].title").value("T11 de B"))
                .andExpect(jsonPath("$.incidents[*].title").value(not(hasItems("T11 de A"))));
    }

    @Test
    void technicianWithoutIncidentsGetsEmptyList() throws Exception {
        createIncident("T11 de B", technicianB);

        listAs(technicianA).andExpect(status().isOk()).andExpect(jsonPath("$.incidents", hasSize(0)));
    }

    @Test
    void listExposesTypeAreaAndAssignedTechnicianWhenPresent() throws Exception {
        Incident incident = createIncident("T11 clasificado", technicianA);
        incident.setClassificationStatus(ClassificationStatus.MANUALLY_CORRECTED);
        incident.setType(IncidentType.NETWORK);
        incident.setPriority(Priority.HIGH);
        incident.setArea(Area.APPLICATIONS);
        incidentRepository.save(incident);

        listAs(supervisor)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents[?(@.id == " + incident.getId() + ")].type").value("NETWORK"))
                .andExpect(
                        jsonPath("$.incidents[?(@.id == " + incident.getId() + ")].area")
                                .value("APPLICATIONS"))
                .andExpect(
                        jsonPath("$.incidents[?(@.id == " + incident.getId() + ")].assignedTo")
                                .value(technicianA.getId().intValue()))
                .andExpect(
                        jsonPath("$.incidents[?(@.id == " + incident.getId() + ")].assignedToName")
                                .value(technicianA.getFullName()));
    }

    @Test
    void listReturnsNullTypeAreaAndTechnicianForPendingUnassignedIncident() throws Exception {
        Incident incident = createIncident("T11 pendiente", null);

        listAs(supervisor)
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.incidents[?(@.id == " + incident.getId() + ")].type").value(hasItem(nullValue())))
                .andExpect(
                        jsonPath("$.incidents[?(@.id == " + incident.getId() + ")].area").value(hasItem(nullValue())))
                .andExpect(
                        jsonPath("$.incidents[?(@.id == " + incident.getId() + ")].assignedTo")
                                .value(hasItem(nullValue())))
                .andExpect(
                        jsonPath("$.incidents[?(@.id == " + incident.getId() + ")].assignedToName")
                                .value(hasItem(nullValue())));
    }

    @Test
    void requestWithoutActiveUserReturns400() throws Exception {
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_ACTIVE_USER"));
    }
}
