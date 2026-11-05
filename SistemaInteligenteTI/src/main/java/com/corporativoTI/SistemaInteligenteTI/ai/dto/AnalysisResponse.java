package com.corporativoTI.SistemaInteligenteTI.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Respuesta 200 de {@code POST /ai/v1/analyses}; {@code grounded=false} no es un error. */
public record AnalysisResponse(
        boolean grounded,
        List<String> steps,
        List<AnalysisReference> references,
        String model,
        @JsonProperty("elapsed_ms") long elapsedMs) {}
