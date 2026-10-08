package com.corporativoTI.SistemaInteligenteTI.users.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.corporativoTI.SistemaInteligenteTI.catalog.model.Area;
import com.corporativoTI.SistemaInteligenteTI.users.exception.RoleNotAllowedException;
import com.corporativoTI.SistemaInteligenteTI.users.model.Role;
import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import org.junit.jupiter.api.Test;

/** RF-1 (T-06): regla de dominio que decide si el rol del usuario activo permite la acción. */
class RoleGuardTest {

    private final RoleGuard roleGuard = new RoleGuard();

    private static User userWith(Role role, Area area) {
        User user = new User();
        user.setUsername("u." + role);
        user.setFullName("Usuario " + role);
        user.setRole(role);
        user.setArea(area);
        return user;
    }

    @Test
    void allowsSupervisor() {
        assertThatCode(() -> roleGuard.requireSupervisor(userWith(Role.SUPERVISOR, null)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsTechnicianWithSpanishMessage() {
        assertThatThrownBy(() -> roleGuard.requireSupervisor(userWith(Role.TECHNICIAN, Area.SECURITY)))
                .isInstanceOf(RoleNotAllowedException.class)
                .hasMessageContaining("supervisor");
    }
}
