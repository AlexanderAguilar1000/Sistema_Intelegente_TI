package com.corporativoTI.SistemaInteligenteTI.incidents.exception;

/** El incidente ya está en un estado final y no admite cambios (se traduce a 409). */
public class IncidentClosedException extends RuntimeException {

    public IncidentClosedException(Long id) {
        super("El incidente " + id + " ya está cerrado y no admite cambios.");
    }
}
