package com.corporativoTI.SistemaInteligenteTI.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Respuesta 200 de {@code POST /ai/v1/classifications}. */
public record ClassificationResponse(
        String type,
        String priority,
        String area,
        String model,
        @JsonProperty("elapsed_ms") long elapsedMs) {}
