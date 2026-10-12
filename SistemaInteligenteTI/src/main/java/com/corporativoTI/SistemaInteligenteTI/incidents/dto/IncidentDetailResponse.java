package com.corporativoTI.SistemaInteligenteTI.incidents.dto;

import java.time.Instant;

/** Detalle completo de un incidente (RF-9). Los campos que aún no aplican viajan como nulos. */
public record IncidentDetailResponse(
        Long id,
        String title,
        String description,
        String status,
        String classificationStatus,
        String type,
        String priority,
        String area,
        Long createdBy,
        Long assignedTo,
        String solution,
        String closureReason,
        Instant createdAt,
        Instant assignedAt,
        Instant startedAt,
        Instant closedAt) {}
