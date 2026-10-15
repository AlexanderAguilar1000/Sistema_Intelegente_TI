package com.corporativoTI.SistemaInteligenteTI.audit.repository;

import com.corporativoTI.SistemaInteligenteTI.audit.model.IncidentEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentEventRepository extends JpaRepository<IncidentEvent, Long> {

    /** Traza de un incidente en el orden en que ocurrió (RNF-6). */
    List<IncidentEvent> findByIncidentIdOrderByIdAsc(Long incidentId);
}
