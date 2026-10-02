package com.corporativoTI.SistemaInteligenteTI.users.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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

    private ResultActions postTechnician(String json) throws Exception {
        return mockMvc.perform(
                post("/api/technicians").contentType(MediaType.APPLICATION_JSON).content(json));
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
