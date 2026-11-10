package com.corporativoTI.SistemaInteligenteTI.incidents.exception;

/**
 * El análisis con IA falló sin alterar el incidente. {@code unavailable} distingue "la IA no
 * responde" (503) de "la IA respondió algo inválido" (502); en ambos casos se puede reintentar.
 */
public class AnalysisFailedException extends RuntimeException {

    private final boolean unavailable;

    private AnalysisFailedException(String message, boolean unavailable) {
        super(message);
        this.unavailable = unavailable;
    }

    public static AnalysisFailedException unavailable() {
        return new AnalysisFailedException(
                "El servicio de IA no está disponible en este momento. Inténtalo de nuevo.", true);
    }

    public static AnalysisFailedException invalidResponse() {
        return new AnalysisFailedException(
                "La IA devolvió una recomendación no válida. Inténtalo de nuevo.", false);
    }

    public boolean isUnavailable() {
        return unavailable;
    }
}
