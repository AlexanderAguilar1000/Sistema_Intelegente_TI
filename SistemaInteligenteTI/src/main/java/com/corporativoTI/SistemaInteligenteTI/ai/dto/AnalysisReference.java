package com.corporativoTI.SistemaInteligenteTI.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Caso resuelto usado como referencia de la recomendación. */
public record AnalysisReference(
        @JsonProperty("incident_id") Long incidentId, String title, double score) {}
