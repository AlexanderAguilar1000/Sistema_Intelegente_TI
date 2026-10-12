package com.corporativoTI.SistemaInteligenteTI.incidents.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
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
 * RF-8 (T-12): {@code GET /api/incidents?priority=HIGH} devuelve solo los de prioridad alta dentro
 * de los visibles, los pendientes de clasificación quedan fuera del filtro, y sin resultados se
 * indica que la lista está vacía.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IncidentPriorityFilterTest {

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
        technicianA = createTechnician("tecnico.t12.a");
        technicianB = createTechnician("tecnico.t12.b");
    }

    private User createTechnician(String username) {
        User technician = new User();
        technician.setUsername(username);
        technician.setFullName("Técnico " + username);
        technician.setRole(Role.TECHNICIAN);
        technician.setArea(Area.APPLICATIONS);
        return userRepository.save(technician);
    }

    /** Crea un incidente clasificado con la prioridad dada, o pendiente si {@code priority} es nulo. */
    private Incident createIncident(String title, Priority priority, User assignedTo) {
        Incident incident = new Incident();
        incident.setTitle(title);
        incident.setDescription("Descripción de " + title);
        incident.setStatus(assignedTo == null ? IncidentStatus.REGISTERED : IncidentStatus.ASSIGNED);
        incident.setCreatedBy(supervisor);
        incident.setAssignedTo(assignedTo);
        incident.setCreatedAt(Instant.now());
        if (priority == null) {
            incident.setClassificationStatus(ClassificationStatus.PENDING);
        } else {
            incident.setClassificationStatus(ClassificationStatus.AI_CLASSIFIED);
            incident.setType(IncidentType.BUG);
            incident.setPriority(priority);
            incident.setArea(Area.APPLICATIONS);
        }
        return incidentRepository.save(incident);
    }

    private ResultActions listAs(User actor, String priority) throws Exception {
        return mockMvc.perform(
                get("/api/incidents").param("priority", priority).header(USER_HEADER, actor.getId()));
    }

    @Test
    void supervisorFiltersByHighPriority() throws Exception {
        createIncident("T12 alta 1", Priority.HIGH, technicianA);
        createIncident("T12 alta 2", Priority.HIGH, null);
        createIncident("T12 media", Priority.MEDIUM, technicianA);
        createIncident("T12 baja", Priority.LOW, technicianB);

        listAs(supervisor, "HIGH")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents[*].title").value(hasItems("T12 alta 1", "T12 alta 2")))
                .andExpect(jsonPath("$.incidents[*].title").value(not(hasItem("T12 media"))))
                .andExpect(jsonPath("$.incidents[*].title").value(not(hasItem("T12 baja"))))
                .andExpect(jsonPath("$.incidents[*].priority").value(not(hasItem("MEDIUM"))))
                .andExpect(jsonPath("$.incidents[*].priority").value(not(hasItem("LOW"))))
                .andExpect(jsonPath("$.empty").value(false));
    }

    @Test
    void technicianFiltersWithinItsOwnIncidentsOnly() throws Exception {
        createIncident("T12 alta de A", Priority.HIGH, technicianA);
        createIncident("T12 media de A", Priority.MEDIUM, technicianA);
        createIncident("T12 alta de B", Priority.HIGH, technicianB);

        listAs(technicianA, "HIGH")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents", hasSize(1)))
                .andExpect(jsonPath("$.incidents[*].title", contains("T12 alta de A")))
                .andExpect(jsonPath("$.incidents[0].priority").value("HIGH"));
    }

    @Test
    void pendingClassificationIncidentsAreLeftOutOfTheFilter() throws Exception {
        createIncident("T12 pendiente", null, technicianA);
        createIncident("T12 alta", Priority.HIGH, technicianA);

        listAs(technicianA, "HIGH")
                .andExpect(jsonPath("$.incidents", hasSize(1)))
                .andExpect(jsonPath("$.incidents[0].title").value("T12 alta"));
        listAs(technicianA, "LOW")
                .andExpect(jsonPath("$.incidents", hasSize(0)));
    }

    @Test
    void withoutFilterPendingIncidentsAreStillListed() throws Exception {
        createIncident("T12 pendiente", null, technicianA);
        createIncident("T12 alta", Priority.HIGH, technicianA);

        mockMvc.perform(get("/api/incidents").header(USER_HEADER, technicianA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents", hasSize(2)))
                .andExpect(jsonPath("$.empty").value(false));
    }

    @Test
    void emptyResultIndicatesTheListIsEmpty() throws Exception {
        createIncident("T12 media", Priority.MEDIUM, technicianA);

        listAs(technicianA, "HIGH")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents", hasSize(0)))
                .andExpect(jsonPath("$.empty").value(true))
                .andExpect(jsonPath("$.message").value(containsString("No hay incidentes")));
    }

    @Test
    void unknownPriorityIsRejectedWith422() throws Exception {
        listAs(supervisor, "URGENT")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.message").value(containsString("priority")));
    }
}
