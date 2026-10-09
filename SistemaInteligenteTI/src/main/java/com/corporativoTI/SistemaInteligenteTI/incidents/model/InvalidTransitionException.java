package com.corporativoTI.SistemaInteligenteTI.incidents.model;

/** La acción pedida no es válida para el estado actual del incidente (se traduce a 409). */
public class InvalidTransitionException extends RuntimeException {

    public InvalidTransitionException(IncidentStatus from, IncidentAction action) {
        super("No se puede aplicar la acción " + action + " a un incidente en estado " + from + ".");
    }
}
