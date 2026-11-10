package com.corporativoTI.SistemaInteligenteTI.incidents.exception;

/** Ya hay un análisis con IA en curso para el incidente (se traduce a 409). */
public class AnalysisInProgressException extends RuntimeException {

    public AnalysisInProgressException(Long id) {
        super("Ya hay un análisis en curso para el incidente " + id + ". Espera a que termine.");
    }
}
