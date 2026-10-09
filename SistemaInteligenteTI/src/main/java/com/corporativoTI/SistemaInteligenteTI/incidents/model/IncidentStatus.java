package com.corporativoTI.SistemaInteligenteTI.incidents.model;

/** Estados del ciclo de vida de un incidente (plan, sección 4). */
public enum IncidentStatus {
    REGISTERED,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED_WITHOUT_SOLUTION;

    /** Resuelto y Cerrado sin solución son estados finales. */
    public boolean isFinal() {
        return this == RESOLVED || this == CLOSED_WITHOUT_SOLUTION;
    }
}
