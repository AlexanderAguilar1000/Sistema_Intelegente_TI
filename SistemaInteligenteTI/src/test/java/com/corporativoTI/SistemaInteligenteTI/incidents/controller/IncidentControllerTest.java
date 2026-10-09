package com.corporativoTI.SistemaInteligenteTI.incidents.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.corporativoTI.SistemaInteligenteTI.catalog.model.Area;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.ClassificationStatus;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.Incident;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.IncidentStatus;
import com.corporativoTI.SistemaInteligenteTI.incidents.repository.IncidentRepository;
import com.corporativoTI.SistemaInteligenteTI.users.model.Role;
import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import com.corporativoTI.SistemaInteligenteTI.users.repository.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-3 (T-09): {@code POST /api/incidents} guarda el incidente en estado Registrado y pendiente de
 * clasificación, con fecha y supervisor; título o descripción vacíos devuelven 422 indicando el campo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IncidentControllerTest {

    private static final String USER_HEADER = "X-User-Id";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private IncidentRepository incidentRepository;

    private User supervisor() {
        return userRepository.findByUsername("laura.supervisora").orElseThrow();
    }

    private ResultActions postIncident(User actor, String json) throws Exception {
        return mockMvc.perform(
                post("/api/incidents")
                        .header(USER_HEADER, actor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json));
    }

    @Test
    void registersIncidentAsRegisteredAndPendingClassification() throws Exception {
        User supervisor = supervisor();
        Instant before = Instant.now().minusSeconds(5);

        postIncident(
                        supervisor,
                        """
                        {"title": "El correo no sincroniza", "description": "Outlook no descarga mensajes nuevos desde ayer."}
                        """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("El correo no sincroniza"))
                .andExpect(jsonPath("$.description").value("Outlook no descarga mensajes nuevos desde ayer."))
                .andExpect(jsonPath("$.status").value("REGISTERED"))
                .andExpect(jsonPath("$.classificationStatus").value("PENDING"))
                .andExpect(jsonPath("$.createdBy").value(supervisor.getId()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        Incident saved = incidentRepository.findAll().stream()
                .filter(i -> i.getTitle().equals("El correo no sincroniza"))
                .findFirst()
                .orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.REGISTERED);
        assertThat(saved.getClassificationStatus()).isEqualTo(ClassificationStatus.PENDING);
        assertThat(saved.getCreatedBy().getId()).isEqualTo(supervisor.getId());
        assertThat(saved.getCreatedAt()).isAfter(before);
        assertThat(saved.getAssignedTo()).isNull();
    }

    @Test
    void rejectsEmptyTitleIndicatingTheField() throws Exception {
        long before = incidentRepository.count();

        postIncident(supervisor(), """
                {"title": "   ", "description": "Descripción válida"}
                """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("MISSING_FIELD"))
                .andExpect(jsonPath("$.message").value(containsString("title")));

        assertThat(incidentRepository.count()).isEqualTo(before);
    }

    @Test
    void rejectsMissingDescriptionIndicatingTheField() throws Exception {
        long before = incidentRepository.count();

        postIncident(supervisor(), """
                {"title": "Título válido"}
                """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("MISSING_FIELD"))
                .andExpect(jsonPath("$.message").value(containsString("description")));

        assertThat(incidentRepository.count()).isEqualTo(before);
    }

    @Test
    void technicianCannotRegisterIncidents() throws Exception {
        User technician = new User();
        technician.setUsername("tecnico.t09");
        technician.setFullName("Técnico T09");
        technician.setRole(Role.TECHNICIAN);
        technician.setArea(Area.APPLICATIONS);
        technician = userRepository.save(technician);
        long before = incidentRepository.count();

        postIncident(technician, """
                {"title": "Título", "description": "Descripción"}
                """)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN_ROLE"));

        assertThat(incidentRepository.count()).isEqualTo(before);
    }

    @Test
    void requestWithoutActiveUserReturns400() throws Exception {
        mockMvc.perform(
                        post("/api/incidents")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"title": "Título", "description": "Descripción"}
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_ACTIVE_USER"));
    }
}
