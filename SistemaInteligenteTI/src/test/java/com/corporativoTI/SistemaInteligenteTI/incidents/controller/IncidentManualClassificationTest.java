package com.corporativoTI.SistemaInteligenteTI.incidents.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
 * RF-6 (T-14): {@code PATCH /api/incidents/{id}/classification} fija tipo, prioridad y área con
 * valores de catálogo, quita la marca de pendiente y deja la clasificación como corregida
 * manualmente; sobre un incidente cerrado devuelve 409.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IncidentManualClassificationTest {

    private static final String USER_HEADER = "X-User-Id";
    private static final String VALID_BODY =
            """
            {"type": "NETWORK", "priority": "LOW", "area": "INFRASTRUCTURE"}
            """;

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private IncidentRepository incidentRepository;

    private User supervisor;
    private User technician;

    @BeforeEach
    void setUp() {
        supervisor = userRepository.findByUsername("laura.supervisora").orElseThrow();
        technician = new User();
        technician.setUsername("tecnico.t14");
        technician.setFullName("Técnico T14");
        technician.setRole(Role.TECHNICIAN);
        technician.setArea(Area.APPLICATIONS);
        technician = userRepository.save(technician);
    }

    private Incident pendingIncident() {
        Incident incident = new Incident();
        incident.setTitle("T14 pendiente");
        incident.setDescription("Descripción");
        incident.setStatus(IncidentStatus.REGISTERED);
        incident.setClassificationStatus(ClassificationStatus.PENDING);
        incident.setCreatedBy(supervisor);
        incident.setCreatedAt(Instant.now());
        return incidentRepository.save(incident);
    }

    private Incident classifiedIncident(IncidentStatus status) {
        Incident incident = pendingIncident();
        incident.setClassificationStatus(ClassificationStatus.AI_CLASSIFIED);
        incident.setType(IncidentType.BUG);
        incident.setPriority(Priority.HIGH);
        incident.setArea(Area.APPLICATIONS);
        incident.setStatus(status);
        if (status != IncidentStatus.REGISTERED) {
            incident.setAssignedTo(technician);
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

    private ResultActions classify(User actor, Long id, String json) throws Exception {
        return mockMvc.perform(
                patch("/api/incidents/{id}/classification", id)
                        .header(USER_HEADER, actor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json));
    }

    @Test
    void classifiesPendingIncidentAsManuallyCorrected() throws Exception {
        Incident incident = pendingIncident();

        classify(supervisor, incident.getId(), VALID_BODY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incident.getId()))
                .andExpect(jsonPath("$.type").value("NETWORK"))
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andExpect(jsonPath("$.area").value("INFRASTRUCTURE"))
                .andExpect(jsonPath("$.classificationStatus").value("MANUALLY_CORRECTED"));

        Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(saved.getType()).isEqualTo(IncidentType.NETWORK);
        assertThat(saved.getPriority()).isEqualTo(Priority.LOW);
        assertThat(saved.getArea()).isEqualTo(Area.INFRASTRUCTURE);
        assertThat(saved.getClassificationStatus()).isEqualTo(ClassificationStatus.MANUALLY_CORRECTED);
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.REGISTERED);
    }

    @Test
    void correctsAnAiClassificationWithoutChangingTheStatus() throws Exception {
        Incident incident = classifiedIncident(IncidentStatus.IN_PROGRESS);

        classify(supervisor, incident.getId(), VALID_BODY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andExpect(jsonPath("$.classificationStatus").value("MANUALLY_CORRECTED"));
    }

    @Test
    void closedIncidentsCannotBeReclassified() throws Exception {
        for (IncidentStatus finalStatus :
                new IncidentStatus[] {IncidentStatus.RESOLVED, IncidentStatus.CLOSED_WITHOUT_SOLUTION}) {
            Incident incident = classifiedIncident(finalStatus);

            classify(supervisor, incident.getId(), VALID_BODY)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("INCIDENT_CLOSED"));

            Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
            assertThat(saved.getPriority()).isEqualTo(Priority.HIGH);
            assertThat(saved.getClassificationStatus()).isEqualTo(ClassificationStatus.AI_CLASSIFIED);
        }
    }

    @Test
    void rejectsValuesOutsideTheCatalogIndicatingTheField() throws Exception {
        Incident incident = pendingIncident();

        classify(
                        supervisor,
                        incident.getId(),
                        """
                        {"type": "NETWORK", "priority": "URGENT", "area": "INFRASTRUCTURE"}
                        """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CATALOG_VALUE"))
                .andExpect(jsonPath("$.message").value(containsString("priority")));

        Incident saved = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(saved.getClassificationStatus()).isEqualTo(ClassificationStatus.PENDING);
        assertThat(saved.getPriority()).isNull();
    }

    @Test
    void rejectsMissingFieldIndicatingTheField() throws Exception {
        Incident incident = pendingIncident();

        classify(
                        supervisor,
                        incident.getId(),
                        """
                        {"type": "NETWORK", "priority": "LOW"}
                        """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("MISSING_FIELD"))
                .andExpect(jsonPath("$.message").value(containsString("area")));
    }

    @Test
    void technicianCannotClassifyManually() throws Exception {
        Incident incident = pendingIncident();

        classify(technician, incident.getId(), VALID_BODY)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN_ROLE"));

        assertThat(incidentRepository.findById(incident.getId()).orElseThrow().getClassificationStatus())
                .isEqualTo(ClassificationStatus.PENDING);
    }

    @Test
    void unknownIncidentReturns404() throws Exception {
        classify(supervisor, 999_999_999L, VALID_BODY)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INCIDENT_NOT_FOUND"));
    }

    @Test
    void requestWithoutActiveUserReturns400() throws Exception {
        mockMvc.perform(
                        patch("/api/incidents/1/classification")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_ACTIVE_USER"));
    }
}
