package com.corporativoTI.SistemaInteligenteTI.incidents.exception;

/** El incidente ya está clasificado, así que no se puede reintentar la clasificación (se traduce a 409). */
public class ClassificationNotPendingException extends RuntimeException {

    public ClassificationNotPendingException(Long id) {
        super("El incidente " + id + " ya está clasificado: solo se reintenta la clasificación de los pendientes.");
    }
}
