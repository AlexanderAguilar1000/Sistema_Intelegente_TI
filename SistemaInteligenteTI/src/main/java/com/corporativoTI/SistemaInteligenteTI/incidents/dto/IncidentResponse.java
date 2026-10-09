package com.corporativoTI.SistemaInteligenteTI.incidents.dto;

import java.time.Instant;

/** Incidente tal como se devuelve al registrarlo (RF-3). */
public record IncidentResponse(
        Long id,
        String title,
        String description,
        String status,
        String classificationStatus,
        Long createdBy,
        Instant createdAt) {}
