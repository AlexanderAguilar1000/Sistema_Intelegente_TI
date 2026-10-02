package com.corporativoTI.SistemaInteligenteTI.users.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.corporativoTI.SistemaInteligenteTI.users.model.Role;
import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import com.corporativoTI.SistemaInteligenteTI.users.repository.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RF-1: toda petición protegida por {@link ActiveUser} debe resolver el usuario activo a
 * partir de la cabecera {@code X-User-Id}: 400 si falta, 400 si no existe, y atribución al
 * usuario cuando el id es válido. Se usa un controlador mínimo de prueba porque el módulo de
 * incidentes (que consumirá este mecanismo) todavía no existe (T-09 en adelante).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ActiveUserArgumentResolverTest.PingController.class)
@Transactional
class ActiveUserArgumentResolverTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;

    @Test
    void requestWithoutHeaderReturns400() throws Exception {
        mockMvc.perform(get("/api/test/active-user"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_ACTIVE_USER"));
    }

    @Test
    void requestWithUnknownIdReturns400() throws Exception {
        mockMvc.perform(get("/api/test/active-user").header("X-User-Id", "999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("UNKNOWN_ACTIVE_USER"));
    }

    @Test
    void requestWithValidIdIsAttributedToThatUser() throws Exception {
        User supervisor = new User();
        supervisor.setUsername("activo.t05");
        supervisor.setFullName("Activo T05");
        supervisor.setRole(Role.SUPERVISOR);
        supervisor.setArea(null);
        supervisor = userRepository.save(supervisor);

        mockMvc.perform(
                        get("/api/test/active-user")
                                .header("X-User-Id", supervisor.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(supervisor.getId()))
                .andExpect(jsonPath("$.role").value("SUPERVISOR"));
    }

    @RestController
    @RequestMapping("/api/test/active-user")
    static class PingController {

        @GetMapping
        public Map<String, Object> whoAmI(@ActiveUser User activeUser) {
            return Map.of("id", activeUser.getId(), "role", activeUser.getRole().name());
        }
    }
}
