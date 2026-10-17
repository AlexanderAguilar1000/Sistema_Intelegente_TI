package com.corporativoTI.SistemaInteligenteTI.incidents.dto;

import java.time.Instant;

/**
 * Incidente tal como se devuelve al registrarlo (RF-3) y en el listado (RF-8). Tipo, área y técnico
 * viajan nulos mientras el incidente esté pendiente de clasificar o sin asignar.
 */
public record IncidentResponse(
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
        String assignedToName,
        Instant createdAt) {}
