package com.corporativoTI.SistemaInteligenteTI.audit.model;

import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Evento de la traza de un incidente: qué acción, quién la hizo y cuándo (RNF-6). */
@Entity
@Table(name = "incident_events")
@Getter
@Setter
@NoArgsConstructor
public class IncidentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id", nullable = false, updatable = false)
    private Long incidentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 40)
    private IncidentEventAction action;

    /** Nulo cuando la acción la ejecuta el sistema y no una persona. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by", updatable = false)
    private User performedBy;

    @Column(name = "performed_at", nullable = false, updatable = false)
    private Instant performedAt;
}
