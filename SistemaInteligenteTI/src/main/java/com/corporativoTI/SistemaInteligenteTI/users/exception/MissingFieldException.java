package com.corporativoTI.SistemaInteligenteTI.users.exception;

/** Falta un dato obligatorio en el alta de un técnico (RF-2). */
public class MissingFieldException extends RuntimeException {

    public MissingFieldException(String field) {
        super("Falta el campo obligatorio '" + field + "'.");
    }
}
