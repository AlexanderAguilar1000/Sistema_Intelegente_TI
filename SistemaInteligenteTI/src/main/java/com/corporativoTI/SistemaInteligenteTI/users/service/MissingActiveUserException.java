package com.corporativoTI.SistemaInteligenteTI.users.service;

/** La petición no trae la cabecera {@code X-User-Id} del usuario activo (RF-1). */
public class MissingActiveUserException extends RuntimeException {

    public MissingActiveUserException() {
        super("Falta la cabecera X-User-Id con el usuario activo.");
    }
}
