package com.corporativoTI.SistemaInteligenteTI.incidents.repository;

import com.corporativoTI.SistemaInteligenteTI.catalog.model.Priority;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.Incident;
import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    /** Incidentes asignados a un técnico (RF-8). */
    List<Incident> findByAssignedToOrderByCreatedAtDesc(User assignedTo);

    /** Todos los incidentes de una prioridad; los pendientes de clasificar no tienen prioridad (RF-8). */
    List<Incident> findByPriorityOrderByCreatedAtDesc(Priority priority);

    /** Incidentes de un técnico filtrados por prioridad (RF-8). */
    List<Incident> findByAssignedToAndPriorityOrderByCreatedAtDesc(User assignedTo, Priority priority);
}
