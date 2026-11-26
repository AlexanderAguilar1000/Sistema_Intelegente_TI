package com.corporativoTI.SistemaInteligenteTI.incidents.repository;

import com.corporativoTI.SistemaInteligenteTI.catalog.model.Priority;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.Incident;
import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Los listados son paginados (RF-8) y el orden lo fija el {@code Pageable}. Cargan el técnico
 * asignado junto al incidente para no consultarlo uno a uno.
 */
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    /** Todos los incidentes con su técnico asignado (listado del supervisor, RF-8). */
    @Override
    @EntityGraph(attributePaths = "assignedTo")
    Page<Incident> findAll(Pageable pageable);

    /** Incidentes asignados a un técnico (RF-8). */
    @EntityGraph(attributePaths = "assignedTo")
    Page<Incident> findByAssignedTo(User assignedTo, Pageable pageable);

    /** Todos los incidentes de una prioridad; los pendientes de clasificar no tienen prioridad (RF-8). */
    @EntityGraph(attributePaths = "assignedTo")
    Page<Incident> findByPriority(Priority priority, Pageable pageable);

    /** Incidentes de un técnico filtrados por prioridad (RF-8). */
    @EntityGraph(attributePaths = "assignedTo")
    Page<Incident> findByAssignedToAndPriority(User assignedTo, Priority priority, Pageable pageable);
}
