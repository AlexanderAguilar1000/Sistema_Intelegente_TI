package com.corporativoTI.SistemaInteligenteTI.users.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.corporativoTI.SistemaInteligenteTI.catalog.model.Area;
import com.corporativoTI.SistemaInteligenteTI.users.dto.UserResponse;
import com.corporativoTI.SistemaInteligenteTI.users.model.Role;
import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import com.corporativoTI.SistemaInteligenteTI.users.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * RF-1: {@code GET /api/users} debe ofrecer nombre, rol y área de cada
 * usuario precargado para que se pueda elegir como usuario activo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void returnsNameRoleAndAreaOfEachPreloadedUser() throws Exception {
        User supervisor = new User();
        supervisor.setUsername("ana.supervisora.t04");
        supervisor.setFullName("Ana Supervisora T04");
        supervisor.setRole(Role.SUPERVISOR);
        supervisor.setArea(null);
        userRepository.save(supervisor);

        User technician = new User();
        technician.setUsername("luis.tecnico.t04");
        technician.setFullName("Luis Técnico T04");
        technician.setRole(Role.TECHNICIAN);
        technician.setArea(Area.APPLICATIONS);
        userRepository.save(technician);

        var result =
                mockMvc.perform(get("/api/users").accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andReturn();

        List<UserResponse> users =
                objectMapper.readValue(
                        result.getResponse().getContentAsString(),
                        new TypeReference<List<UserResponse>>() {});

        assertThat(users)
                .anySatisfy(
                        user -> {
                            assertThat(user.fullName()).isEqualTo("Ana Supervisora T04");
                            assertThat(user.role()).isEqualTo("SUPERVISOR");
                            assertThat(user.area()).isNull();
                        });

        assertThat(users)
                .anySatisfy(
                        user -> {
                            assertThat(user.fullName()).isEqualTo("Luis Técnico T04");
                            assertThat(user.role()).isEqualTo("TECHNICIAN");
                            assertThat(user.area()).isEqualTo("APPLICATIONS");
                        });

        assertThat(users)
                .allSatisfy(
                        user -> {
                            assertThat(user.id()).isNotNull();
                            assertThat(user.fullName()).isNotBlank();
                            assertThat(user.role()).isIn("SUPERVISOR", "TECHNICIAN");
                            if ("SUPERVISOR".equals(user.role())) {
                                assertThat(user.area()).isNull();
                            } else {
                                assertThat(user.area()).isNotNull();
                            }
                        });
    }
}
