package com.corporativoTI.SistemaInteligenteTI.incidents.repository;

import com.corporativoTI.SistemaInteligenteTI.catalog.model.Priority;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.Incident;
import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Los listados cargan el técnico asignado junto al incidente para no consultarlo uno a uno. */
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    /** Todos los incidentes con su técnico asignado (listado del supervisor, RF-8). */
    @Override
    @EntityGraph(attributePaths = "assignedTo")
    List<Incident> findAll(Sort sort);

    /** Incidentes asignados a un técnico (RF-8). */
    @EntityGraph(attributePaths = "assignedTo")
    List<Incident> findByAssignedToOrderByCreatedAtDesc(User assignedTo);

    /** Todos los incidentes de una prioridad; los pendientes de clasificar no tienen prioridad (RF-8). */
    @EntityGraph(attributePaths = "assignedTo")
    List<Incident> findByPriorityOrderByCreatedAtDesc(Priority priority);

    /** Incidentes de un técnico filtrados por prioridad (RF-8). */
    @EntityGraph(attributePaths = "assignedTo")
    List<Incident> findByAssignedToAndPriorityOrderByCreatedAtDesc(User assignedTo, Priority priority);
}
