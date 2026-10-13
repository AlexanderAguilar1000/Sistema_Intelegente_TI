package com.corporativoTI.SistemaInteligenteTI.incidents.exception;

/** El usuario indicado no existe o no es un técnico (se traduce a 422). */
public class InvalidTechnicianException extends RuntimeException {

    public InvalidTechnicianException(Long technicianId) {
        super("El usuario " + technicianId + " no existe o no es un técnico.");
    }
}
