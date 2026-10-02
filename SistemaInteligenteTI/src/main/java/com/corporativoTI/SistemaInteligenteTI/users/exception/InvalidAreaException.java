package com.corporativoTI.SistemaInteligenteTI.users.exception;

/** El área indicada no pertenece al catálogo de áreas (RF-2, RF-5). */
public class InvalidAreaException extends RuntimeException {

    public InvalidAreaException(String area) {
        super("El área '" + area + "' no pertenece al catálogo de áreas.");
    }
}
