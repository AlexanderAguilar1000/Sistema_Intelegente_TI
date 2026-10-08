package com.corporativoTI.SistemaInteligenteTI.users.service;

import com.corporativoTI.SistemaInteligenteTI.users.exception.RoleNotAllowedException;
import com.corporativoTI.SistemaInteligenteTI.users.model.Role;
import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import org.springframework.stereotype.Component;

/**
 * Regla de dominio que comprueba el rol del usuario activo antes de cada caso de uso (RF-1).
 * No es autenticación: el MVP confía en la cabecera {@code X-User-Id}.
 */
@Component
public class RoleGuard { //Esta es una regla que permite que solo el supervisor pueda realizar algunas acciones . 

    public void requireSupervisor(User activeUser) {
        if (activeUser.getRole() != Role.SUPERVISOR) {//si otro rol que no es el supervisor realiza esa accion devuel el error RoleNotAllowedException
            throw new RoleNotAllowedException("Esta acción solo la puede realizar un supervisor.");
        }
    }
}
