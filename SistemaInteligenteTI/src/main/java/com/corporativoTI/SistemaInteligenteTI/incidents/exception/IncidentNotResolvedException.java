package com.corporativoTI.SistemaInteligenteTI.incidents.exception;

/** Solo los incidentes resueltos entran en el histórico (se traduce a 409). */
public class IncidentNotResolvedException extends RuntimeException {

    public IncidentNotResolvedException(Long id) {
        super("El incidente " + id + " no está resuelto: solo se indexan los incidentes resueltos.");
    }
}
