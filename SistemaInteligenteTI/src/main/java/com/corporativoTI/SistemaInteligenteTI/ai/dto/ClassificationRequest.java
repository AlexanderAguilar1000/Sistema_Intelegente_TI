package com.corporativoTI.SistemaInteligenteTI.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Petición de {@code POST /ai/v1/classifications} (RF-4). Sin datos de cuentas (RNF-4). */
public record ClassificationRequest(
        @JsonProperty("incident_id") Long incidentId,
        String title,
        String description,
        AiCatalog catalog) {}
