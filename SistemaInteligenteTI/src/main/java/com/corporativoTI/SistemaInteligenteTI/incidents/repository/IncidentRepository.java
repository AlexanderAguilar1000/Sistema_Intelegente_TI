package com.corporativoTI.SistemaInteligenteTI.incidents.repository;

import com.corporativoTI.SistemaInteligenteTI.incidents.model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, Long> {}
