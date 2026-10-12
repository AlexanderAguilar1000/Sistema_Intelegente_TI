package com.corporativoTI.SistemaInteligenteTI.incidents.exception;

/** No existe el incidente solicitado (RF-9). */
public class IncidentNotFoundException extends RuntimeException {

    public IncidentNotFoundException(Long id) {
        super("No existe el incidente con id " + id + ".");
    }
}
