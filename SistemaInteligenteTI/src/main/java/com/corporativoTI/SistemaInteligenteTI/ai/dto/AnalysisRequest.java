package com.corporativoTI.SistemaInteligenteTI.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Petición de {@code POST /ai/v1/analyses} (RF-11). */
public record AnalysisRequest(
        @JsonProperty("incident_id") Long incidentId,
        String title,
        String description,
        String type,
        String priority,
        String area,
        @JsonProperty("top_k") int topK,
        @JsonProperty("min_score") double minScore) {}
