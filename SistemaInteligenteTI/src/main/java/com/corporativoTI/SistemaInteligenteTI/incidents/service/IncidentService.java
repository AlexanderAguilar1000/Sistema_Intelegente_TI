package com.corporativoTI.SistemaInteligenteTI.incidents.service;

import com.corporativoTI.SistemaInteligenteTI.incidents.dto.CreateIncidentRequest;
import com.corporativoTI.SistemaInteligenteTI.incidents.dto.IncidentResponse;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.ClassificationStatus;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.Incident;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.IncidentStatus;
import com.corporativoTI.SistemaInteligenteTI.incidents.repository.IncidentRepository;
import com.corporativoTI.SistemaInteligenteTI.users.exception.MissingFieldException;
import com.corporativoTI.SistemaInteligenteTI.users.model.Role;
import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import com.corporativoTI.SistemaInteligenteTI.users.service.RoleGuard;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso sobre incidentes: registro (RF-3) y listado según el usuario activo (RF-8). */
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final RoleGuard roleGuard;

    /**
     * Registra un incidente en estado Registrado y pendiente de clasificación. La clasificación
     * con IA se incorpora en T-30; hasta entonces todo incidente nace pendiente.
     */
    public IncidentResponse registerIncident(User activeUser, CreateIncidentRequest request) {
        roleGuard.requireSupervisor(activeUser);
        String title = requireText(request.title(), "title");
        String description = requireText(request.description(), "description");

        Incident incident = new Incident();
        incident.setTitle(title);
        incident.setDescription(description);
        incident.setStatus(IncidentStatus.REGISTERED);
        incident.setClassificationStatus(ClassificationStatus.PENDING);
        incident.setCreatedBy(activeUser);
        incident.setCreatedAt(Instant.now());
        Incident saved = incidentRepository.save(incident);

        return toResponse(saved);
    }

    /** Lista los incidentes visibles: todos para el supervisor, solo los asignados al técnico (RF-8). */
    @Transactional(readOnly = true)
    public List<IncidentResponse> listIncidents(User activeUser) {
        List<Incident> visible =
                activeUser.getRole() == Role.SUPERVISOR
                        ? incidentRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                        : incidentRepository.findByAssignedToOrderByCreatedAtDesc(activeUser);
        return visible.stream().map(IncidentService::toResponse).toList();
    }

    private static IncidentResponse toResponse(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getStatus().name(),
                incident.getClassificationStatus().name(),
                incident.getCreatedBy().getId(),
                incident.getCreatedAt());
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new MissingFieldException(field);
        }
        return value.trim();
    }
}
