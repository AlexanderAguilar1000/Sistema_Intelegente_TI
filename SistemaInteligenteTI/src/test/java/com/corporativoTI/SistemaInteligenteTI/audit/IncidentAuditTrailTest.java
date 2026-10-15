package com.corporativoTI.SistemaInteligenteTI.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.corporativoTI.SistemaInteligenteTI.audit.model.IncidentEvent;
import com.corporativoTI.SistemaInteligenteTI.audit.model.IncidentEventAction;
import com.corporativoTI.SistemaInteligenteTI.audit.repository.IncidentEventRepository;
import com.corporativoTI.SistemaInteligenteTI.catalog.model.Area;
import com.corporativoTI.SistemaInteligenteTI.users.model.Role;
import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import com.corporativoTI.SistemaInteligenteTI.users.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * RNF-6 (T-22): cada acción sobre un incidente deja un evento en {@code incident_events} con el
 * usuario que la hizo y la fecha en que se hizo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IncidentAuditTrailTest {

    private static final String USER_HEADER = "X-User-Id";
    private static final String SOLUTION = "Se reinició el servicio y se verificó el funcionamiento correcto.";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private IncidentEventRepository eventRepository;

    private User supervisor;
    private User otherSupervisor;
    private User firstTechnician;
    private User secondTechnician;

    @BeforeEach
    void setUp() {
        supervisor = userRepository.findByUsername("laura.supervisora").orElseThrow();
        otherSupervisor = user("supervisor.t22", Role.SUPERVISOR, null);
        firstTechnician = user("tecnico.t22.uno", Role.TECHNICIAN, Area.APPLICATIONS);
        secondTechnician = user("tecnico.t22.dos", Role.TECHNICIAN, Area.APPLICATIONS);
    }

    private User user(String username, Role role, Area area) {
        User user = new User();
        user.setUsername(username);
        user.setFullName("Usuario " + username);
        user.setRole(role);
        user.setArea(area);
        return userRepository.save(user);
    }

    private String send(MockHttpServletRequestBuilder request, User actor, String json, int expectedStatus)
            throws Exception {
        MockHttpServletRequestBuilder builder =
                request.header(USER_HEADER, actor.getId()).contentType(MediaType.APPLICATION_JSON);
        if (json != null) {
            builder = builder.content(json);
        }
        return mockMvc.perform(builder)
                .andExpect(status().is(expectedStatus))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private long registerIncident() throws Exception {
        String body =
                send(
                        post("/api/incidents"),
                        supervisor,
                        "{\"title\":\"Servidor caído\",\"description\":\"No responde el servidor de aplicaciones\"}",
                        201);
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private void classify(long id, String area) throws Exception {
        send(
                patch("/api/incidents/{id}/classification", id),
                supervisor,
                "{\"type\":\"BUG\",\"priority\":\"HIGH\",\"area\":\"" + area + "\"}",
                200);
    }

    private void assign(long id, User actor, User technician) throws Exception {
        send(
                put("/api/incidents/{id}/assignment", id),
                actor,
                "{\"technicianId\":" + technician.getId() + "}",
                200);
    }

    private List<IncidentEvent> eventsOf(long incidentId) {
        eventRepository.flush();
        return eventRepository.findByIncidentIdOrderByIdAsc(incidentId);
    }

    @Test
    void fullCycleLeavesOneEventPerActionWithItsUserAndDate() throws Exception {
        Instant before = Instant.now().minusSeconds(5);

        long id = registerIncident();
        classify(id, "APPLICATIONS");
        assign(id, supervisor, firstTechnician);
        assign(id, otherSupervisor, secondTechnician);
        send(post("/api/incidents/{id}/start", id), secondTechnician, null, 200);
        send(
                post("/api/incidents/{id}/resolution", id),
                secondTechnician,
                "{\"solution\":\"" + SOLUTION + "\"}",
                200);

        Instant after = Instant.now().plusSeconds(5);
        List<IncidentEvent> events = eventsOf(id);

        assertThat(events).extracting(IncidentEvent::getAction)
                .containsExactly(
                        IncidentEventAction.CREATED,
                        IncidentEventAction.MANUALLY_CLASSIFIED,
                        IncidentEventAction.ASSIGNED,
                        IncidentEventAction.REASSIGNED,
                        IncidentEventAction.STARTED,
                        IncidentEventAction.RESOLVED);
        assertThat(events).extracting(event -> event.getPerformedBy().getId())
                .containsExactly(
                        supervisor.getId(),
                        supervisor.getId(),
                        supervisor.getId(),
                        otherSupervisor.getId(),
                        secondTechnician.getId(),
                        secondTechnician.getId());
        assertThat(events)
                .allSatisfy(
                        event ->
                                assertThat(event.getPerformedAt()).isBetween(before, after));
    }

    @Test
    void closingWithoutSolutionLeavesItsEvent() throws Exception {
        long id = registerIncident();
        classify(id, "APPLICATIONS");
        assign(id, supervisor, firstTechnician);
        send(post("/api/incidents/{id}/closure", id), firstTechnician, "{\"reason\":\"DUPLICATE\"}", 200);

        List<IncidentEvent> events = eventsOf(id);

        assertThat(events).extracting(IncidentEvent::getAction)
                .containsExactly(
                        IncidentEventAction.CREATED,
                        IncidentEventAction.MANUALLY_CLASSIFIED,
                        IncidentEventAction.ASSIGNED,
                        IncidentEventAction.CLOSED_WITHOUT_SOLUTION);
        assertThat(events.get(3).getPerformedBy().getId()).isEqualTo(firstTechnician.getId());
    }

    @Test
    void areaChangeThatWithdrawsTheAssignmentRecordsBothEvents() throws Exception {
        long id = registerIncident();
        classify(id, "APPLICATIONS");
        assign(id, supervisor, firstTechnician);

        classify(id, "DATABASE");

        List<IncidentEvent> events = eventsOf(id);
        assertThat(events).extracting(IncidentEvent::getAction)
                .containsExactly(
                        IncidentEventAction.CREATED,
                        IncidentEventAction.MANUALLY_CLASSIFIED,
                        IncidentEventAction.ASSIGNED,
                        IncidentEventAction.MANUALLY_CLASSIFIED,
                        IncidentEventAction.UNASSIGNED_BY_AREA_CHANGE);
        assertThat(events.get(4).getPerformedBy().getId()).isEqualTo(supervisor.getId());
    }

    @Test
    void rejectedActionsLeaveNoEvent() throws Exception {
        long id = registerIncident();

        // Asignar sin clasificar (409) e iniciar sin ser el técnico asignado (403).
        send(
                put("/api/incidents/{id}/assignment", id),
                supervisor,
                "{\"technicianId\":" + firstTechnician.getId() + "}",
                409);
        send(post("/api/incidents/{id}/start", id), firstTechnician, null, 403);

        assertThat(eventsOf(id)).extracting(IncidentEvent::getAction)
                .containsExactly(IncidentEventAction.CREATED);
    }
}
