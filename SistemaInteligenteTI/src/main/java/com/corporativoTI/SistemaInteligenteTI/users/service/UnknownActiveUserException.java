package com.corporativoTI.SistemaInteligenteTI.users.service;

/** La cabecera {@code X-User-Id} no corresponde a ningún usuario existente (RF-1). */
public class UnknownActiveUserException extends RuntimeException {

    public UnknownActiveUserException(String userId) {
        super("No existe ningún usuario activo con id " + userId + ".");
    }
}
