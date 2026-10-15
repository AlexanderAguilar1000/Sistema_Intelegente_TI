package com.corporativoTI.SistemaInteligenteTI.incidents.service;

import com.corporativoTI.SistemaInteligenteTI.audit.model.IncidentEventAction;
import com.corporativoTI.SistemaInteligenteTI.audit.service.AuditService;
import com.corporativoTI.SistemaInteligenteTI.catalog.model.Area;
import com.corporativoTI.SistemaInteligenteTI.catalog.model.IncidentType;
import com.corporativoTI.SistemaInteligenteTI.catalog.model.Priority;
import com.corporativoTI.SistemaInteligenteTI.incidents.dto.AssignIncidentRequest;
import com.corporativoTI.SistemaInteligenteTI.incidents.dto.ClassifyIncidentRequest;
import com.corporativoTI.SistemaInteligenteTI.incidents.dto.CloseIncidentRequest;
import com.corporativoTI.SistemaInteligenteTI.incidents.dto.CreateIncidentRequest;
import com.corporativoTI.SistemaInteligenteTI.incidents.dto.IncidentDetailResponse;
import com.corporativoTI.SistemaInteligenteTI.incidents.dto.IncidentListResponse;
import com.corporativoTI.SistemaInteligenteTI.incidents.dto.IncidentResponse;
import com.corporativoTI.SistemaInteligenteTI.incidents.dto.ResolveIncidentRequest;
import com.corporativoTI.SistemaInteligenteTI.incidents.exception.IncidentClosedException;
import com.corporativoTI.SistemaInteligenteTI.incidents.exception.IncidentNotFoundException;
import com.corporativoTI.SistemaInteligenteTI.incidents.exception.InvalidCatalogValueException;
import com.corporativoTI.SistemaInteligenteTI.incidents.exception.InvalidTechnicianException;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.ClassificationStatus;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.ClosureReason;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.Incident;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.IncidentAction;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.IncidentStateMachine;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.IncidentStatus;
import com.corporativoTI.SistemaInteligenteTI.incidents.repository.IncidentRepository;
import com.corporativoTI.SistemaInteligenteTI.users.exception.MissingFieldException;
import com.corporativoTI.SistemaInteligenteTI.users.exception.RoleNotAllowedException;
import com.corporativoTI.SistemaInteligenteTI.users.model.Role;
import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import com.corporativoTI.SistemaInteligenteTI.users.repository.UserRepository;
import com.corporativoTI.SistemaInteligenteTI.users.service.RoleGuard;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso sobre incidentes: registro (RF-3) y listado según el usuario activo (RF-8) detalle (RF-9) y clasificación manual (RF-6). */
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final IncidentStateMachine stateMachine;
    private final RoleGuard roleGuard;
    private final AuditService auditService;

    /**
     * Registra un incidente en estado Registrado y pendiente de clasificación. La clasificación
     * con IA se incorpora en T-30; hasta entonces todo incidente nace pendiente.
     */
    @Transactional
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
        auditService.record(saved.getId(), IncidentEventAction.CREATED, activeUser);

        return toResponse(saved);
    }

    /**
     * Lista los incidentes visibles: todos para el supervisor, solo los asignados al técnico. Con
     * {@code priority} filtra por esa prioridad; los pendientes de clasificar quedan fuera (RF-8).
     */
    @Transactional(readOnly = true)
    public IncidentListResponse listIncidents(User activeUser, Priority priority) {
        boolean supervisor = activeUser.getRole() == Role.SUPERVISOR;
        List<Incident> visible;
        if (priority == null) {
            //aca mira al supervisor , si es supervisor y no tiene prioridad le devuelve todos los incidentes del supervisor ,
            //si es un tecnico y no hay prioridad le devuelve incidentes asignados a un tecnico
            visible =
                    supervisor
                            ? incidentRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                            : incidentRepository.findByAssignedToOrderByCreatedAtDesc(activeUser);
        } else { 
            //aca mira si es supervisor y si tiene prioridad y devuelve la lista de incidentes que tiene el supervisor y filtrado prioridad     
            //si es tecnico filtra por prioridad y el rol que es tecnico 
            visible =
                    supervisor
                            ? incidentRepository.findByPriorityOrderByCreatedAtDesc(priority)
                            : incidentRepository.findByAssignedToAndPriorityOrderByCreatedAtDesc(
                                    activeUser, priority);
        }
        //aqui estoy llamando al static metodo de la clase  le pasa la lista de incidentes  y el map lo que hace es transformar cada objto de esa lista a un objeto to response  . 
        //to list lo pone en una lista
        return IncidentListResponse.of(visible.stream().map(IncidentService::toResponse).toList());
    }

    /**
     * Detalle de un incidente (RF-9). El supervisor puede ver cualquiera; el técnico solo los que
     * tiene asignados.
     */
    @Transactional(readOnly = true)
    public IncidentDetailResponse getIncident(User activeUser, Long id) {
        Incident incident =
                incidentRepository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
        //si el inciente no ha sido asignado aun tecnico  , es decir el incidente se encuentra e la cuenta del supervisor  y se
        //identifico que este usuario esta logeado y esta en la base de datos .Devuelve true  
        boolean ownIncident =//si el incidente no ha sido asignado aun tecnico
                incident.getAssignedTo() != null
                        && incident.getAssignedTo().getId().equals(activeUser.getId());
        //si el rol es diferente que el supervisor y el incidente le pertenece aun tecnico , hay que verificar 
        //si el usuario que tiene la sección actual es al que ha sido seleccionado para resolver el incidente , sino sale error 
        if (activeUser.getRole() != Role.SUPERVISOR && !ownIncident) {
            throw new RoleNotAllowedException("Solo puedes ver los incidentes que tienes asignados.");
        }

        return toDetailResponse(incident);
    }

    /**
     * Fija tipo, prioridad y área a mano, solo el supervisor (RF-6). Los tres valores son
     * obligatorios y deben pertenecer al catálogo.
     */
    @Transactional
    public IncidentDetailResponse classifyManually(
            User activeUser, Long id, ClassifyIncidentRequest request) {
        roleGuard.requireSupervisor(activeUser);//mira que es el supervisor 
        IncidentType type = parseCatalogValue(IncidentType.class, "type", request.type());//esto verifica que esta dentro del enum el valor que el 
        Priority priority = parseCatalogValue(Priority.class, "priority", request.priority());//usuario quiere cambiar 
        Area area = parseCatalogValue(Area.class, "area", request.area());

        Incident incident =
                incidentRepository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));//encontrar el incidente que se quiere cambiar 
        incident.classifyManually(type, priority, area);
        auditService.record(id, IncidentEventAction.MANUALLY_CLASSIFIED, activeUser);
        // Si el nuevo área excluye al técnico actual, el incidente vuelve a Registrado sin técnico.
        //Si cambia el area del incidente y el tecnico asignado no pertenece a esa area , se retira la asignacion y se cambia el estado del incidente a registrado
        if (incident.assignmentExcludedByArea()) {
            incident.withdrawAssignment(
                    stateMachine.transition(incident.getStatus(), IncidentAction.UNASSIGN_BY_AREA_CHANGE));
            auditService.record(id, IncidentEventAction.UNASSIGNED_BY_AREA_CHANGE, activeUser);
        }
        return toDetailResponse(incidentRepository.save(incident));//guarda en la base de datos  y se lo envia al todetailresponse y lo envia al enpoind 
    }

    /**
     * Asigna un incidente clasificado a un técnico de su área, solo el supervisor (RF-7). Registra
     * técnico, fecha y supervisor; el estado resultante lo fija la máquina de estados.
     */
    @Transactional
    public IncidentDetailResponse assignIncident(User activeUser, Long id, AssignIncidentRequest request) {
        roleGuard.requireSupervisor(activeUser);//mira que es el supervisor
        if (request.technicianId() == null) {//el id del tecnico tiene que estar registrado en la base de datos 
            throw new MissingFieldException("technicianId");
        }

        Incident incident =
                incidentRepository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));//busca el incidente
        User technician =
                userRepository
                        .findById(request.technicianId())//busca el id del tecnico
                        .filter(user -> user.getRole() == Role.TECHNICIAN)//mira que su rol sea tecnico 
                        .orElseThrow(() -> new InvalidTechnicianException(request.technicianId()));//si no es tecnico sale error 

        // Los estados finales y la clasificación pendiente se rechazan antes que la transición o asignación del incidente a un técnico.
        if (incident.getStatus().isFinal()) {
            throw new IncidentClosedException(id);
        }
        //state maching permite cambiar el estado del incidente de registrado a asignado , de asignado a en progreso , de en progreso a resuelto , etc
        //CAMBIA EL estado de un incidente de registrado a asignado
        IncidentAction action =
                incident.getStatus() == IncidentStatus.REGISTERED
                        ? IncidentAction.ASSIGN
                        : IncidentAction.REASSIGN;
        IncidentStatus target = stateMachine.transition(incident.getStatus(), action);
        incident.assignTo(technician, activeUser, target, Instant.now());//asgina el icidente
        auditService.record(
                id,
                action == IncidentAction.ASSIGN
                        ? IncidentEventAction.ASSIGNED
                        : IncidentEventAction.REASSIGNED,
                activeUser);
        return toDetailResponse(incidentRepository.save(incident));//lo guarda en la base de datos y lo devuelve al enpoind 
    }

    /**
     * Inicia el trabajo de un incidente (RF-10). Solo lo puede pedir el técnico asignado; el estado
     * resultante lo fija la máquina de estados.
     */
    @Transactional
    public IncidentDetailResponse startIncident(User activeUser, Long id) {
        Incident incident =
                incidentRepository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
        if (!incident.isAssignedTo(activeUser)) {//entonces mira incident.isAssignedTo(activeUser) esto si devuelve true significa que el tecnico asignado es igual al tencioc loggeado 
                                                //entonces "!" esto cambia de true a false entonces  solo si el boleano devuelve true lanza la excepción , sino no . 
            throw new RoleNotAllowedException("Solo el técnico asignado puede iniciar el incidente.");
        }
        if (incident.getStatus().isFinal()) {
            throw new IncidentClosedException(id);
        }
        //cambia el estado del incidente a empezado
        IncidentStatus target = stateMachine.transition(incident.getStatus(), IncidentAction.START);
        incident.start(target, Instant.now());
        auditService.record(id, IncidentEventAction.STARTED, activeUser);
        return toDetailResponse(incidentRepository.save(incident));
    }

    /**
     * Resuelve un incidente con su solución (RF-12). Solo lo puede pedir el técnico asignado; el
     * estado resultante lo fija la máquina de estados y la longitud mínima la exige el incidente.
     */
    @Transactional
    public IncidentDetailResponse resolveIncident(User activeUser, Long id, ResolveIncidentRequest request) {
        Incident incident =
                incidentRepository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
        if (!incident.isAssignedTo(activeUser)) {
            throw new RoleNotAllowedException("Solo el técnico asignado puede resolver el incidente.");
        }
        if (incident.getStatus().isFinal()) {
            throw new IncidentClosedException(id);
        }
        String solution = requireText(request.solution(), "solution");
        IncidentStatus target = stateMachine.transition(incident.getStatus(), IncidentAction.RESOLVE);
        incident.resolve(solution, target, Instant.now());//llama a este metodo para verificar si cumple con la cantidad minima de caracteres
        auditService.record(id, IncidentEventAction.RESOLVED, activeUser);
        return toDetailResponse(incidentRepository.save(incident));//lo guarda en la base de datos y lo convierte a to detailresponse para que el backend se lo pase a angular . 
    }

    /**
     * Cierra un incidente sin solución indicando el motivo (RF-14). Solo lo puede pedir el técnico
     * asignado; el estado resultante lo fija la máquina de estados.
     */
    @Transactional
    public IncidentDetailResponse closeWithoutSolution(
            User activeUser, Long id, CloseIncidentRequest request) {
        Incident incident =
                incidentRepository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
        //Lo que hace este "!" es convertir el booleano  asu valor contrario si devuelve true lo convierte y viceverda
        //entonces solo 
        if (!incident.isAssignedTo(activeUser)) {
            throw new RoleNotAllowedException("Solo el técnico asignado puede cerrar el incidente.");
        }
        if (incident.getStatus().isFinal()) {
            throw new IncidentClosedException(id);
        }
        ClosureReason reason = parseCatalogValue(ClosureReason.class, "reason", request.reason());
        IncidentStatus target =
                stateMachine.transition(incident.getStatus(), IncidentAction.CLOSE_WITHOUT_SOLUTION);
        incident.closeWithoutSolution(reason, target, Instant.now());
        auditService.record(id, IncidentEventAction.CLOSED_WITHOUT_SOLUTION, activeUser);
        return toDetailResponse(incidentRepository.save(incident));
    }

    private static <E extends Enum<E>> E parseCatalogValue(Class<E> catalog, String field, String value) {
        if (value == null || value.isBlank()) {
            throw new MissingFieldException(field);
        }
        try {
            return Enum.valueOf(catalog, value.trim());//si el valor esta dentro del enum  perfecto devuelve el valor  value , sino devuelve error
                                                       //diciendo que no se encuentra el valor dentro del enum  y lanza la excepcion InvalidCatalogValueException
        } catch (IllegalArgumentException ex) {
            throw new InvalidCatalogValueException(field, value);
        }
    }

    private static IncidentDetailResponse toDetailResponse(Incident incident) {
        return new IncidentDetailResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getStatus().name(),
                incident.getClassificationStatus().name(),
                incident.getType() == null ? null : incident.getType().name(),
                incident.getPriority() == null ? null : incident.getPriority().name(),
                incident.getArea() == null ? null : incident.getArea().name(),
                incident.getCreatedBy().getId(),
                incident.getAssignedTo() == null ? null : incident.getAssignedTo().getId(),
                incident.getAssignedBy() == null ? null : incident.getAssignedBy().getId(),
                incident.getSolution(),
                incident.getClosureReason() == null ? null : incident.getClosureReason().name(),
                incident.getCreatedAt(),
                incident.getAssignedAt(),
                incident.getStartedAt(),
                incident.getClosedAt());
    }

    private static IncidentResponse toResponse(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getStatus().name(),
                incident.getClassificationStatus().name(),
                incident.getPriority() == null ? null : incident.getPriority().name(),
                incident.getCreatedBy().getId(),
                incident.getCreatedAt());
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new MissingFieldException(field);
        }
        return value.trim();
    }
}
