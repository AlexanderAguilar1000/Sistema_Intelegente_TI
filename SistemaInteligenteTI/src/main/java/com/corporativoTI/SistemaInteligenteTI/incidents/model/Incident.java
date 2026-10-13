package com.corporativoTI.SistemaInteligenteTI.incidents.model;

import com.corporativoTI.SistemaInteligenteTI.catalog.model.Area;
import com.corporativoTI.SistemaInteligenteTI.catalog.model.IncidentType;
import com.corporativoTI.SistemaInteligenteTI.catalog.model.Priority;
import com.corporativoTI.SistemaInteligenteTI.incidents.exception.IncidentClosedException;
import com.corporativoTI.SistemaInteligenteTI.incidents.exception.IncidentNotClassifiedException;
import com.corporativoTI.SistemaInteligenteTI.incidents.exception.TechnicianAreaMismatchException;
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
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Incidente de TI (RF-3). Nace en estado Registrado y pendiente de clasificación. */
@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IncidentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification_status", nullable = false, length = 25)
    private ClassificationStatus classificationStatus;

    /** Clasificación (RF-4): los tres campos son nulos mientras esté pendiente. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private IncidentType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Area area;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    /** Supervisor que hizo la asignación vigente (RF-7). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    /** Solución escrita al resolver (RF-12). */
    @Column(columnDefinition = "TEXT")
    private String solution;

    /** Motivo de cierre sin solución (RF-14). */
    @Enumerated(EnumType.STRING)
    @Column(name = "closure_reason", length = 25)
    private ClosureReason closureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    /**
     * Fija la clasificación a mano y la deja como corregida manualmente (RF-6). Un incidente en
     * estado final no admite cambios.
     */
    public void classifyManually(IncidentType type, Priority priority, Area area) {
        //si el estado del incidente es final es decir ya se termino no se puede modificar el estado del incidente
        if (status.isFinal()) {
            throw new IncidentClosedException(id);
        }
        this.type = type;
        this.priority = priority;
        this.area = area;
        this.classificationStatus = ClassificationStatus.MANUALLY_CORRECTED;
    }

    /**
     * Asigna el incidente a un técnico de su área (RF-7). {@code newStatus} lo decide la máquina de
     * estados; aquí solo se comprueban las reglas propias del incidente.
     */
    public void assignTo(User technician, User supervisor, IncidentStatus newStatus, Instant now) {
        if (status.isFinal()) {
            throw new IncidentClosedException(id);
        }
        if (classificationStatus == ClassificationStatus.PENDING) {
            throw new IncidentNotClassifiedException(id);
        }
        if (technician.getArea() != area) {
            throw new TechnicianAreaMismatchException(technician.getArea(), area);
        }
        this.assignedTo = technician;
        this.assignedBy = supervisor;
        this.assignedAt = now;
        this.status = newStatus;
    }

    /** Bloqueo optimista para acciones concurrentes sobre el mismo incidente. */
    @Version
    private Long version;
}
