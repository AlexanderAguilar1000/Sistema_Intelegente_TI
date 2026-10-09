package com.corporativoTI.SistemaInteligenteTI.incidents.service;

import com.corporativoTI.SistemaInteligenteTI.incidents.dto.CreateIncidentRequest;
import com.corporativoTI.SistemaInteligenteTI.incidents.dto.IncidentResponse;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.ClassificationStatus;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.Incident;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.IncidentStatus;
import com.corporativoTI.SistemaInteligenteTI.incidents.repository.IncidentRepository;
import com.corporativoTI.SistemaInteligenteTI.users.exception.MissingFieldException;
import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import com.corporativoTI.SistemaInteligenteTI.users.service.RoleGuard;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Casos de uso sobre incidentes: registro (RF-3). */
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

        return new IncidentResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getStatus().name(),
                saved.getClassificationStatus().name(),
                activeUser.getId(),
                saved.getCreatedAt());
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new MissingFieldException(field);
        }
        return value.trim();
    }
}
