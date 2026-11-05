package com.corporativoTI.SistemaInteligenteTI.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Petición de {@code POST /ai/v1/embeddings} (RF-13). */
public record EmbeddingRequest(
        @JsonProperty("incident_id") Long incidentId,
        String title,
        String description,
        String solution,
        String type,
        String area) {}
