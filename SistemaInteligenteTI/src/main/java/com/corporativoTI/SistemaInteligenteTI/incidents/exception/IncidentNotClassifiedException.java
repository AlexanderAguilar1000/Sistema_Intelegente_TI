package com.corporativoTI.SistemaInteligenteTI.incidents.exception;

/** El incidente sigue pendiente de clasificación y no se puede asignar (se traduce a 409). */
public class IncidentNotClassifiedException extends RuntimeException {

    public IncidentNotClassifiedException(Long id) {
        super("El incidente " + id + " está pendiente de clasificación: clasifícalo antes de asignarlo.");
    }
}
