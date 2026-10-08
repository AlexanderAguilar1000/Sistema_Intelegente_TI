package com.corporativoTI.SistemaInteligenteTI.users.exception;

/** El rol del usuario activo no permite la acción solicitada (RF-1). */
public class RoleNotAllowedException extends RuntimeException {

    public RoleNotAllowedException(String message) {
        super(message);
    }
}
