package com.corporativoTI.SistemaInteligenteTI.incidents.dto;

import java.util.List;

/**
 * Recomendación paso a paso para el técnico (RF-11). {@code grounded=false} indica que no se basa
 * en casos previos y no es un error.
 */
public record IncidentAnalysisResponse(
        boolean grounded, List<String> steps, List<Reference> references) {

    /** Incidente similar usado como referencia. */
    public record Reference(Long incidentId, String title, double score) {}
}
