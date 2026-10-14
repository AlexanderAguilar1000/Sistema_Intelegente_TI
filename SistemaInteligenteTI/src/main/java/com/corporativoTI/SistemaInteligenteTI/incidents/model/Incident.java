package com.corporativoTI.SistemaInteligenteTI.incidents.model;

import com.corporativoTI.SistemaInteligenteTI.catalog.model.Area;
import com.corporativoTI.SistemaInteligenteTI.catalog.model.IncidentType;
import com.corporativoTI.SistemaInteligenteTI.catalog.model.Priority;
import com.corporativoTI.SistemaInteligenteTI.incidents.exception.IncidentClosedException;
import com.corporativoTI.SistemaInteligenteTI.incidents.exception.IncidentNotClassifiedException;
import com.corporativoTI.SistemaInteligenteTI.incidents.exception.SolutionTooShortException;
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

    /** Longitud mínima de la solución para resolver un incidente (RF-12). */
    public static final int MIN_SOLUTION_LENGTH = 30;

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

    /** Indica si {@code user} es el técnico asignado vigente del incidente (RF-9, RF-10). */
    //si hay algun asignado 
    //assignedTo es objeto de la clase User , entonces dice si no hay ninguna persona asignada al incidente  y si hay un asignado , pero el id del asignado no  o concuerda con el usuario que esta logeado actualmente
    //el sistema lo detecta y devuelve error ( Si la persona asignada al incidente es la misma que la persona que esta logeada devuelve true )
    public boolean isAssignedTo(User user) {
        return assignedTo != null && assignedTo.getId().equals(user.getId());
    }

    /** Inicia el trabajo (RF-10). {@code newStatus} lo decide la máquina de estados. */
    public void start(IncidentStatus newStatus, Instant now) {
        this.startedAt = now;
        this.status = newStatus;
    }

    /**
     * Resuelve el incidente con su solución (RF-12). {@code newStatus} lo decide la máquina de
     * estados; aquí se exige la longitud mínima de la solución y se registra la fecha de cierre. El
     * técnico responsable es el asignado vigente.
     */
    public void resolve(String solution, IncidentStatus newStatus, Instant now) {
        if (solution.length() < MIN_SOLUTION_LENGTH) {//si la solución es menor de 30 caracteres aparece la oslución deb tener minimo esta cantidad de caracteres . 
            throw new SolutionTooShortException(MIN_SOLUTION_LENGTH);
        }
        this.solution = solution;
        this.closedAt = now;
        this.status = newStatus;
    }

    /**
     * Cierra el incidente sin solución (RF-14). {@code newStatus} lo decide la máquina de estados;
     * aquí se registran el motivo y la fecha de cierre, sin exigir texto de solución.
     */
    public void closeWithoutSolution(ClosureReason reason, IncidentStatus newStatus, Instant now) {
        this.closureReason = reason;
        this.closedAt = now;
        this.status = newStatus;
    }

    /** Indica si el técnico asignado deja de pertenecer al área vigente del incidente (RF-6). */
    public boolean assignmentExcludedByArea() {
        return assignedTo != null && assignedTo.getArea() != area;
    }

    /**
     * Retira la asignación al cambiar el área (RF-6). {@code newStatus} lo decide la máquina de
     * estados; si el trabajo había empezado, se descarta su fecha de inicio.
     */
    public void withdrawAssignment(IncidentStatus newStatus) {
        this.assignedTo = null;
        this.assignedBy = null;
        this.assignedAt = null;
        this.startedAt = null;
        this.status = newStatus;
    }

    /**
     * Asigna o reasigna el incidente a un técnico de su área (RF-7). {@code newStatus} lo decide la
     * máquina de estados; aquí solo se comprueban las reglas propias del incidente. Al reasignar un
     * incidente en progreso vuelve a Asignado y se descarta su fecha de inicio.
     */
    public void assignTo(User technician, User supervisor, IncidentStatus newStatus, Instant now) {
        if (status.isFinal()) {//no se puede modificar un incidente que ya esta cerrado
            throw new IncidentClosedException(id);
        }
        if (classificationStatus == ClassificationStatus.PENDING) { //antes de asignar un incidente a una persona el sistema debe de clasificarlo automaticamente 
            throw new IncidentNotClassifiedException(id);
        }
        if (technician.getArea() != area) { //solo se puede asignar a un técnico de su área
            throw new TechnicianAreaMismatchException(technician.getArea(), area);
        }
        this.assignedTo = technician;///cumple todo esto y guarda los valores 
        this.assignedBy = supervisor;
        this.assignedAt = now;
        this.startedAt = null;
        this.status = newStatus;
    }

    /** Bloqueo optimista para acciones concurrentes sobre el mismo incidente. */
    @Version
    private Long version;
}
