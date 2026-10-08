package com.corporativoTI.SistemaInteligenteTI.users.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.corporativoTI.SistemaInteligenteTI.catalog.model.Area;
import com.corporativoTI.SistemaInteligenteTI.users.model.Role;
import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import com.corporativoTI.SistemaInteligenteTI.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-2: {@code POST /api/technicians} crea un técnico con rol y área; rechaza usuario repetido,
 * área fuera de catálogo y campos faltantes indicando la causa.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TechnicianControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;

    /** Las acciones sobre técnicos son de supervisor (T-06): se actúa como el supervisor precargado. */
    private String supervisorId() {
        return String.valueOf(userRepository.findByUsername("laura.supervisora").orElseThrow().getId());
    }

    private ResultActions postTechnician(String json) throws Exception {
        return mockMvc.perform(
                post("/api/technicians")
                        .header("X-User-Id", supervisorId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json));
    }

    @Test
    void createsTechnicianWithTechnicianRoleAndArea() throws Exception {
        postTechnician(
                        """
                        {"fullName": "Marta Técnica T07", "username": "marta.t07", "area": "DATABASE"}
                        """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.fullName").value("Marta Técnica T07"))
                .andExpect(jsonPath("$.role").value("TECHNICIAN"))
                .andExpect(jsonPath("$.area").value("DATABASE"));

        User saved = userRepository.findByUsername("marta.t07").orElseThrow();
        assertThat(saved.getRole()).isEqualTo(Role.TECHNICIAN);
        assertThat(saved.getArea()).isEqualTo(Area.DATABASE);
        assertThat(saved.getFullName()).isEqualTo("Marta Técnica T07");
    }

    @Test
    void rejectsRepeatedUsernameIndicatingTheCause() throws Exception {
        User existing = new User();
        existing.setUsername("repetido.t07");
        existing.setFullName("Ya Existe T07");
        existing.setRole(Role.TECHNICIAN);
        existing.setArea(Area.SECURITY);
        userRepository.save(existing);
        long usersBefore = userRepository.count();

        postTechnician(
                        """
                        {"fullName": "Otro Nombre", "username": "repetido.t07", "area": "APPLICATIONS"}
                        """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USERNAME_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value(containsString("repetido.t07")));

        assertThat(userRepository.count()).isEqualTo(usersBefore);
    }

    @Test
    void rejectsAreaOutsideTheCatalogIndicatingTheCause() throws Exception {
        postTechnician(
                        """
                        {"fullName": "Sin Área T07", "username": "area.mala.t07", "area": "COCINA"}
                        """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("INVALID_AREA"))
                .andExpect(jsonPath("$.message").value(containsString("COCINA")));

        assertThat(userRepository.findByUsername("area.mala.t07")).isEmpty();
    }

    @Test
    void rejectsMissingFullNameIndicatingTheField() throws Exception {
        postTechnician(
                        """
                        {"username": "sin.nombre.t07", "area": "SECURITY"}
                        """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("MISSING_FIELD"))
                .andExpect(jsonPath("$.message").value(containsString("fullName")));

        assertThat(userRepository.findByUsername("sin.nombre.t07")).isEmpty();
    }

    @Test
    void rejectsMissingUsernameIndicatingTheField() throws Exception {
        postTechnician(
                        """
                        {"fullName": "Sin Usuario T07", "username": "   ", "area": "SECURITY"}
                        """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("MISSING_FIELD"))
                .andExpect(jsonPath("$.message").value(containsString("username")));
    }

    private User saveUser(String username, Role role, Area area) {
        User user = new User();
        user.setUsername(username);
        user.setFullName("Nombre " + username);
        user.setRole(role);
        user.setArea(area);
        return userRepository.save(user);
    }

    /** RF-7 (T-08): solo los técnicos del área pedida, ninguno de otra y ningún supervisor. */
    @Test
    void listsOnlyTechniciansOfTheRequestedArea() throws Exception {
        saveUser("apps.uno.t08", Role.TECHNICIAN, Area.APPLICATIONS);
        saveUser("apps.dos.t08", Role.TECHNICIAN, Area.APPLICATIONS);
        saveUser("db.uno.t08", Role.TECHNICIAN, Area.DATABASE);
        saveUser("super.t08", Role.SUPERVISOR, null);

        mockMvc.perform(get("/api/technicians").header("X-User-Id", supervisorId()).param("area", "APPLICATIONS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.fullName == 'Nombre apps.uno.t08')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.fullName == 'Nombre apps.dos.t08')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.fullName == 'Nombre db.uno.t08')]").isEmpty())
                .andExpect(jsonPath("$[?(@.fullName == 'Nombre super.t08')]").isEmpty())
                .andExpect(jsonPath("$[?(@.area != 'APPLICATIONS')]").isEmpty())
                .andExpect(jsonPath("$[?(@.role != 'TECHNICIAN')]").isEmpty());

        mockMvc.perform(get("/api/technicians").header("X-User-Id", supervisorId()).param("area", "DATABASE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.fullName == 'Nombre db.uno.t08')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.fullName == 'Nombre apps.uno.t08')]").isEmpty());
    }

    @Test
    void returnsEmptyListWhenAreaHasNoTechnicians() throws Exception {
        long inSecurity = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.TECHNICIAN && u.getArea() == Area.SECURITY)
                .count();
        saveUser("apps.solo.t08", Role.TECHNICIAN, Area.APPLICATIONS);

        mockMvc.perform(get("/api/technicians").header("X-User-Id", supervisorId()).param("area", "SECURITY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value((int) inSecurity))
                .andExpect(jsonPath("$[?(@.fullName == 'Nombre apps.solo.t08')]").isEmpty());
    }

    @Test
    void rejectsListingWithAreaOutsideTheCatalog() throws Exception {
        mockMvc.perform(get("/api/technicians").header("X-User-Id", supervisorId()).param("area", "COCINA"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("INVALID_AREA"))
                .andExpect(jsonPath("$.message").value(containsString("COCINA")));
    }

    @Test
    void rejectsListingWithoutArea() throws Exception {
        mockMvc.perform(get("/api/technicians").header("X-User-Id", supervisorId()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("MISSING_FIELD"))
                .andExpect(jsonPath("$.message").value(containsString("area")));
    }

    @Test
    void rejectsMissingAreaIndicatingTheField() throws Exception {
        postTechnician(
                        """
                        {"fullName": "Sin Área T07", "username": "sin.area.t07"}
                        """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("MISSING_FIELD"))
                .andExpect(jsonPath("$.message").value(containsString("area")));

        assertThat(userRepository.findByUsername("sin.area.t07")).isEmpty();
    }
}
