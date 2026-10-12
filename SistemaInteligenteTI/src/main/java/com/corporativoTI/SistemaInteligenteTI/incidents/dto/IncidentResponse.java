package com.corporativoTI.SistemaInteligenteTI.incidents.dto;

import java.time.Instant;

/** Incidente tal como se devuelve al registrarlo (RF-3) y en el listado (RF-8). */
public record IncidentResponse(
        Long id,
        String title,
        String description,
        String status,
        String classificationStatus,
        String priority,
        Long createdBy,
        Instant createdAt) {}
