package com.corporativoTI.SistemaInteligenteTI.audit.service;

import com.corporativoTI.SistemaInteligenteTI.audit.model.IncidentEvent;
import com.corporativoTI.SistemaInteligenteTI.audit.model.IncidentEventAction;
import com.corporativoTI.SistemaInteligenteTI.audit.repository.IncidentEventRepository;
import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Registra en {@code incident_events} quién hace cada acción sobre un incidente y cuándo (RNF-6). */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final IncidentEventRepository eventRepository;

    /**
     * Deja el evento en la misma transacción que la acción auditada: si la acción falla, el evento
     * tampoco se guarda. {@code performedBy} es nulo cuando actúa el sistema.
     */
    public void record(Long incidentId, IncidentEventAction action, User performedBy) {
        IncidentEvent event = new IncidentEvent();
        event.setIncidentId(incidentId);
        event.setAction(action);
        event.setPerformedBy(performedBy);
        event.setPerformedAt(Instant.now());
        eventRepository.save(event);
    }
}
