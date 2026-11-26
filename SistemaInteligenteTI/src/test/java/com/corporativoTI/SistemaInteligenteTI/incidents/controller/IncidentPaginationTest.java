package com.corporativoTI.SistemaInteligenteTI.incidents.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
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
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-8 (T-46): {@code GET /api/incidents} pagina con {@code page} y {@code size} (0 y 20 por
 * defecto, máximo 100), ordena del más reciente al más antiguo con orden estable y devuelve los
 * totales. Una página fuera de rango llega vacía y un tamaño inválido devuelve 422.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IncidentPaginationTest {

    private static final String USER_HEADER = "X-User-Id";

    /** Fecha base fija para que el orden esperado no dependa del reloj. */
    private static final Instant BASE = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private IncidentRepository incidentRepository;

    private User supervisor;
    private User technicianA;
    private User technicianB;

    @BeforeEach
    void setUp() {
        supervisor = userRepository.findByUsername("laura.supervisora").orElseThrow();
        technicianA = createTechnician("tecnico.t46.a");
        technicianB = createTechnician("tecnico.t46.b");
    }

    private User createTechnician(String username) {
        User technician = new User();
        technician.setUsername(username);
        technician.setFullName("Técnico " + username);
        technician.setRole(Role.TECHNICIAN);
        technician.setArea(Area.APPLICATIONS);
        return userRepository.save(technician);
    }

    private Incident createIncident(
            String title, User assignedTo, Priority priority, Instant createdAt) {
        Incident incident = new Incident();
        incident.setTitle(title);
        incident.setDescription("Descripción de " + title);
        incident.setStatus(IncidentStatus.ASSIGNED);
        incident.setCreatedBy(supervisor);
        incident.setAssignedTo(assignedTo);
        incident.setCreatedAt(createdAt);
        incident.setClassificationStatus(ClassificationStatus.AI_CLASSIFIED);
        incident.setType(IncidentType.BUG);
        incident.setPriority(priority);
        incident.setArea(Area.APPLICATIONS);
        return incidentRepository.save(incident);
    }

    /** Crea {@code count} incidentes del técnico; el número 1 es el más antiguo. */
    private void createIncidents(User assignedTo, int count) {
        for (int i = 1; i <= count; i++) {
            createIncident("T46 " + i, assignedTo, Priority.MEDIUM, BASE.plusSeconds(i));
        }
    }

    /** Lista como {@code actor}; los parámetros se pasan en pares nombre, valor. */
    private ResultActions listAs(User actor, String... params) throws Exception {
        var request = get("/api/incidents").header(USER_HEADER, actor.getId());
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        return mockMvc.perform(request);
    }

    @Test
    void withoutParametersReturnsFirstPageOfTwenty() throws Exception {
        createIncidents(technicianA, 25);

        listAs(technicianA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents", hasSize(20)))
                .andExpect(jsonPath("$.incidents[0].title").value("T46 25"))
                .andExpect(jsonPath("$.incidents[19].title").value("T46 6"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.empty").value(false));
    }

    @Test
    void secondPageReturnsTheRemainingIncidents() throws Exception {
        createIncidents(technicianA, 25);

        listAs(technicianA, "page", "1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents", hasSize(5)))
                .andExpect(jsonPath("$.incidents[0].title").value("T46 5"))
                .andExpect(jsonPath("$.incidents[4].title").value("T46 1"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void sizeAcceptsTheMaximumOfOneHundred() throws Exception {
        createIncidents(technicianA, 101);

        listAs(technicianA, "size", "100")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents", hasSize(100)))
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.totalElements").value(101))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void ordersByCreationDateDescendingAndBreaksTiesByIdAcrossPages() throws Exception {
        // Se guardan desordenados para que el resultado no dependa del orden de inserción.
        createIncident("T46 intermedio", technicianA, Priority.MEDIUM, BASE);
        createIncident("T46 reciente", technicianA, Priority.MEDIUM, BASE.plusSeconds(60));
        createIncident("T46 antiguo", technicianA, Priority.MEDIUM, BASE.minusSeconds(60));
        createIncident("T46 empate id menor", technicianA, Priority.MEDIUM, BASE.minusSeconds(30));
        createIncident("T46 empate id mayor", technicianA, Priority.MEDIUM, BASE.minusSeconds(30));

        listAs(technicianA, "size", "2", "page", "0")
                .andExpect(
                        jsonPath(
                                "$.incidents[*].title",
                                contains("T46 reciente", "T46 intermedio")));
        listAs(technicianA, "size", "2", "page", "1")
                .andExpect(
                        jsonPath(
                                "$.incidents[*].title",
                                contains("T46 empate id mayor", "T46 empate id menor")));
        listAs(technicianA, "size", "2", "page", "2")
                .andExpect(jsonPath("$.incidents[*].title", contains("T46 antiguo")));
    }

    @Test
    void pageOutOfRangeIsEmptyButKeepsTheRealTotals() throws Exception {
        createIncidents(technicianA, 5);

        listAs(technicianA, "page", "50", "size", "2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents", hasSize(0)))
                .andExpect(jsonPath("$.empty").value(true))
                .andExpect(jsonPath("$.message").value(containsString("No hay incidentes")))
                .andExpect(jsonPath("$.page").value(50))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void hugePageNumberIsAlsoJustAnEmptyPage() throws Exception {
        createIncidents(technicianA, 3);

        listAs(technicianA, "page", String.valueOf(Integer.MAX_VALUE), "size", "100")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents", hasSize(0)))
                .andExpect(jsonPath("$.page").value(Integer.MAX_VALUE))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void withoutIncidentsTotalsAreZero() throws Exception {
        listAs(technicianA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents", hasSize(0)))
                .andExpect(jsonPath("$.empty").value(true))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @ParameterizedTest
    @CsvSource({
        "size, 0",
        "size, -1",
        "size, 101",
        "size, abc",
        "page, -1",
        "page, abc",
    })
    void invalidPagingParameterIsRejectedWith422(String name, String value) throws Exception {
        listAs(supervisor, name, value)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.message").value(containsString(name)));
    }

    @Test
    void technicianPaginatesOnlyItsOwnIncidents() throws Exception {
        createIncidents(technicianA, 3);
        createIncidents(technicianB, 5);

        listAs(technicianA, "size", "2", "page", "1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents", hasSize(1)))
                .andExpect(jsonPath("$.incidents[0].assignedTo").value(technicianA.getId().intValue()))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void supervisorTotalsCountEveryIncident() throws Exception {
        createIncidents(technicianA, 2);
        createIncidents(technicianB, 2);
        int total = (int) incidentRepository.count();

        listAs(supervisor, "size", "1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(total))
                .andExpect(jsonPath("$.totalPages").value(total));
    }

    @Test
    void priorityFilterIsAppliedBeforePaginating() throws Exception {
        for (int i = 1; i <= 3; i++) {
            createIncident("T46 alta " + i, technicianA, Priority.HIGH, BASE.plusSeconds(i));
            createIncident("T46 baja " + i, technicianA, Priority.LOW, BASE.plusSeconds(10 + i));
        }

        listAs(technicianA, "priority", "HIGH", "size", "2", "page", "1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents[*].title", contains("T46 alta 1")))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void supervisorPriorityFilterTotalsOnlyCountThatPriority() throws Exception {
        createIncident("T46 alta", technicianA, Priority.HIGH, BASE);
        createIncident("T46 baja", technicianB, Priority.LOW, BASE);
        int high =
                (int)
                        incidentRepository.findAll().stream()
                                .filter(incident -> incident.getPriority() == Priority.HIGH)
                                .count();

        listAs(supervisor, "priority", "HIGH", "size", "1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents[0].priority").value("HIGH"))
                .andExpect(jsonPath("$.totalElements").value(high))
                .andExpect(jsonPath("$.totalPages").value(high));
    }
}
