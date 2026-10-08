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
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-1 (T-06): un técnico que llama a una acción de supervisor recibe 403 con mensaje en español.
 * Se cubren dos endpoints de supervisor: el alta de técnicos y la consulta de técnicos por área
 * (usada al asignar).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SupervisorOnlyActionsTest {

    private static final String USER_HEADER = "X-User-Id";
    private static final String VALID_BODY =
            """
            {"fullName": "Nuevo Técnico T06", "username": "nuevo.t06", "area": "SECURITY"}
            """;

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;

    private User saveUser(String username, Role role, Area area) {
        User user = new User();
        user.setUsername(username);
        user.setFullName("Nombre " + username);
        user.setRole(role);
        user.setArea(area);
        return userRepository.save(user);
    }

    @Test
    void technicianCannotCreateTechnicians() throws Exception {
        User technician = saveUser("tecnico.post.t06", Role.TECHNICIAN, Area.APPLICATIONS);
        long usersBefore = userRepository.count();

        mockMvc.perform(
                        post("/api/technicians")
                                .header(USER_HEADER, technician.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN_ROLE"))
                .andExpect(jsonPath("$.message").value(containsString("supervisor")));

        assertThat(userRepository.count()).isEqualTo(usersBefore);
        assertThat(userRepository.findByUsername("nuevo.t06")).isEmpty();
    }

    @Test
    void technicianCannotListTechniciansByArea() throws Exception {
        User technician = saveUser("tecnico.get.t06", Role.TECHNICIAN, Area.DATABASE);

        mockMvc.perform(
                        get("/api/technicians")
                                .param("area", "DATABASE")
                                .header(USER_HEADER, technician.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN_ROLE"))
                .andExpect(jsonPath("$.message").value(containsString("supervisor")));
    }

    @Test
    void roleIsCheckedBeforeValidatingTheBody() throws Exception {
        User technician = saveUser("tecnico.vacio.t06", Role.TECHNICIAN, Area.SECURITY);

        mockMvc.perform(
                        post("/api/technicians")
                                .header(USER_HEADER, technician.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN_ROLE"));
    }

    @Test
    void supervisorCanCreateTechnicians() throws Exception {
        User supervisor = saveUser("super.post.t06", Role.SUPERVISOR, null);

        mockMvc.perform(
                        post("/api/technicians")
                                .header(USER_HEADER, supervisor.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("TECHNICIAN"));
    }

    @Test
    void supervisorCanListTechniciansByArea() throws Exception {
        User supervisor = saveUser("super.get.t06", Role.SUPERVISOR, null);

        mockMvc.perform(
                        get("/api/technicians")
                                .param("area", "DATABASE")
                                .header(USER_HEADER, supervisor.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void requestWithoutActiveUserStillReturns400() throws Exception {
        mockMvc.perform(get("/api/technicians").param("area", "DATABASE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_ACTIVE_USER"));
    }
}
