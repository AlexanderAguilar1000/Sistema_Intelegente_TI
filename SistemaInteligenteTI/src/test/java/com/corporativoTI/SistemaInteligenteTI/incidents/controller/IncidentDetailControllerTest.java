package com.corporativoTI.SistemaInteligenteTI.incidents.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * RF-9 (T-13): {@code GET /api/incidents/{id}} devuelve título, descripción, clasificación, estado,
 * fechas y solución o motivo de cierre; un técnico que pide un incidente ajeno recibe 403 y el
 * supervisor puede ver cualquiera.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IncidentDetailControllerTest {

    private static final String USER_HEADER = "X-User-Id";
    private static final String SOLUTION =
            "Se reinició el servicio de sincronización y se recreó el perfil de correo.";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private IncidentRepository incidentRepository;

    private User supervisor;
    private User technicianA;
    private User technicianB;

    @BeforeEach
    void setUp() {
        supervisor = userRepository.findByUsername("laura.supervisora").orElseThrow();
        technicianA = createTechnician("tecnico.t13.a");
        technicianB = createTechnician("tecnico.t13.b");
    }

    private User createTechnician(String username) {
        User technician = new User();
        technician.setUsername(username);
        technician.setFullName("Técnico " + username);
        technician.setRole(Role.TECHNICIAN);
        technician.setArea(Area.APPLICATIONS);
        return userRepository.save(technician);
    }

    private Incident baseIncident(String title, IncidentStatus status, User assignedTo) {
        Incident incident = new Incident();
        incident.setTitle(title);
        incident.setDescription("Descripción de " + title);
        incident.setStatus(status);
        incident.setClassificationStatus(ClassificationStatus.AI_CLASSIFIED);
        incident.setType(IncidentType.CONFIGURATION);
        incident.setPriority(Priority.HIGH);
        incident.setArea(Area.APPLICATIONS);
        incident.setCreatedBy(supervisor);
        incident.setAssignedTo(assignedTo);
        incident.setCreatedAt(Instant.now());
        return incident;
    }

    private ResultActions detailAs(User actor, Long id) throws Exception {
        return mockMvc.perform(get("/api/incidents/{id}", id).header(USER_HEADER, actor.getId()));
    }

    @Test
    void supervisorSeesFullDetailOfAnyIncident() throws Exception {
        Incident incident = baseIncident("T13 asignado", IncidentStatus.ASSIGNED, technicianA);
        incident.setAssignedAt(Instant.now());
        incident = incidentRepository.save(incident);

        detailAs(supervisor, incident.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incident.getId()))
                .andExpect(jsonPath("$.title").value("T13 asignado"))
                .andExpect(jsonPath("$.description").value("Descripción de T13 asignado"))
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.classificationStatus").value("AI_CLASSIFIED"))
                .andExpect(jsonPath("$.type").value("CONFIGURATION"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.area").value("APPLICATIONS"))
                .andExpect(jsonPath("$.createdBy").value(supervisor.getId()))
                .andExpect(jsonPath("$.assignedTo").value(technicianA.getId()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.assignedAt").isNotEmpty())
                .andExpect(jsonPath("$.startedAt").value(nullValue()))
                .andExpect(jsonPath("$.closedAt").value(nullValue()))
                .andExpect(jsonPath("$.solution").value(nullValue()))
                .andExpect(jsonPath("$.closureReason").value(nullValue()));
    }

    @Test
    void assignedTechnicianSeesItsOwnIncident() throws Exception {
        Incident incident =
                incidentRepository.save(baseIncident("T13 propio", IncidentStatus.ASSIGNED, technicianA));

        detailAs(technicianA, incident.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("T13 propio"));
    }

    @Test
    void technicianRequestingAnotherTechniciansIncidentGets403() throws Exception {
        Incident incident =
                incidentRepository.save(baseIncident("T13 ajeno", IncidentStatus.ASSIGNED, technicianB));

        detailAs(technicianA, incident.getId())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN_ROLE"))
                .andExpect(jsonPath("$.message").value(containsString("incidente")));
    }

    @Test
    void technicianRequestingAnUnassignedIncidentGets403() throws Exception {
        Incident incident =
                incidentRepository.save(baseIncident("T13 sin asignar", IncidentStatus.REGISTERED, null));

        detailAs(technicianA, incident.getId()).andExpect(status().isForbidden());
    }

    @Test
    void resolvedIncidentShowsSolutionAndCloseDate() throws Exception {
        Incident incident = baseIncident("T13 resuelto", IncidentStatus.RESOLVED, technicianA);
        incident.setSolution(SOLUTION);
        incident.setStartedAt(Instant.now());
        incident.setClosedAt(Instant.now());
        incident = incidentRepository.save(incident);

        detailAs(technicianA, incident.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.solution").value(SOLUTION))
                .andExpect(jsonPath("$.startedAt").isNotEmpty())
                .andExpect(jsonPath("$.closedAt").isNotEmpty())
                .andExpect(jsonPath("$.closureReason").value(nullValue()));
    }

    @Test
    void closedWithoutSolutionIncidentShowsClosureReason() throws Exception {
        Incident incident =
                baseIncident("T13 cerrado", IncidentStatus.CLOSED_WITHOUT_SOLUTION, technicianA);
        incident.setClosureReason(ClosureReason.DUPLICATE);
        incident.setClosedAt(Instant.now());
        incident = incidentRepository.save(incident);

        detailAs(supervisor, incident.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED_WITHOUT_SOLUTION"))
                .andExpect(jsonPath("$.closureReason").value("DUPLICATE"))
                .andExpect(jsonPath("$.solution").value(nullValue()))
                .andExpect(jsonPath("$.closedAt").isNotEmpty());
    }

    @Test
    void pendingClassificationIncidentShowsNoClassification() throws Exception {
        Incident incident = baseIncident("T13 pendiente", IncidentStatus.REGISTERED, null);
        incident.setClassificationStatus(ClassificationStatus.PENDING);
        incident.setType(null);
        incident.setPriority(null);
        incident.setArea(null);
        incident = incidentRepository.save(incident);

        detailAs(supervisor, incident.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classificationStatus").value("PENDING"))
                .andExpect(jsonPath("$.type").value(nullValue()))
                .andExpect(jsonPath("$.priority").value(nullValue()))
                .andExpect(jsonPath("$.area").value(nullValue()));
    }

    @Test
    void unknownIncidentReturns404() throws Exception {
        detailAs(supervisor, 999_999_999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INCIDENT_NOT_FOUND"));
    }

    @Test
    void requestWithoutActiveUserReturns400() throws Exception {
        mockMvc.perform(get("/api/incidents/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_ACTIVE_USER"));
    }
}
