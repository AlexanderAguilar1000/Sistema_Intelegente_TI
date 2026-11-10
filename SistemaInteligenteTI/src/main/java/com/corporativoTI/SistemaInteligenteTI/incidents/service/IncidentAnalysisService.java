package com.corporativoTI.SistemaInteligenteTI.incidents.service;

import com.corporativoTI.SistemaInteligenteTI.ai.client.AiClient;
import com.corporativoTI.SistemaInteligenteTI.ai.dto.AnalysisRequest;
import com.corporativoTI.SistemaInteligenteTI.ai.dto.AnalysisResponse;
import com.corporativoTI.SistemaInteligenteTI.ai.model.AiResult;
import com.corporativoTI.SistemaInteligenteTI.audit.model.IncidentEventAction;
import com.corporativoTI.SistemaInteligenteTI.audit.service.AuditService;
import com.corporativoTI.SistemaInteligenteTI.incidents.dto.IncidentAnalysisResponse;
import com.corporativoTI.SistemaInteligenteTI.incidents.exception.AnalysisFailedException;
import com.corporativoTI.SistemaInteligenteTI.incidents.exception.AnalysisInProgressException;
import com.corporativoTI.SistemaInteligenteTI.incidents.exception.IncidentClosedException;
import com.corporativoTI.SistemaInteligenteTI.incidents.exception.IncidentNotFoundException;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.Incident;
import com.corporativoTI.SistemaInteligenteTI.incidents.repository.IncidentRepository;
import com.corporativoTI.SistemaInteligenteTI.users.exception.RoleNotAllowedException;
import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Caso de uso "Analizar con IA" (RF-11). La llamada a la IA puede tardar hasta 30 s, así que no se
 * mantiene una transacción abierta durante ella: primero se confirma la marca de análisis en curso
 * (D9), luego se llama a la IA y por último se libera la marca y se deja la traza. Un fallo de la IA
 * no altera el incidente (D11).
 */
@Service
public class IncidentAnalysisService {

    /** Casos similares que se piden al histórico (RF-11). */
    static final int TOP_K = 3;

    /** Umbral mínimo de parecido propuesto; se calibra con datos reales (plan, sección 9). */
    static final double MIN_SCORE = 0.75;

    private final IncidentRepository incidentRepository;
    private final AiClient aiClient;
    private final AuditService auditService;
    private final TransactionTemplate transaction;
    private final Duration analysisTimeout;

    public IncidentAnalysisService(
            IncidentRepository incidentRepository,
            AiClient aiClient,
            AuditService auditService,
            PlatformTransactionManager transactionManager,
            @Value("${ai.analysis-timeout:30s}") Duration analysisTimeout) {
        this.incidentRepository = incidentRepository;
        this.aiClient = aiClient;
        this.auditService = auditService;
        this.transaction = new TransactionTemplate(transactionManager);
        this.analysisTimeout = analysisTimeout;
    }

    /** Solo el técnico asignado puede analizar; el supervisor y otros técnicos reciben 403. */
    public IncidentAnalysisResponse analyze(User activeUser, Long id) {
        AnalysisRequest request = transaction.execute(status -> begin(activeUser, id));

//Aqui lo que hace es recomendar al usuario como resolver un incidente  . 
        AiResult<AnalysisResponse> result = aiClient.analyze(request);

        AnalysisResponse answer = result instanceof AiResult.Success<AnalysisResponse> success
                ? success.value()
                : null;
        boolean valid = hasStepWithText(answer);
        transaction.executeWithoutResult(
                status -> finish(activeUser, id, valid));

        if (valid) {
            return toResponse(answer);
        }
        throw result instanceof AiResult.Unavailable
                ? AnalysisFailedException.unavailable()
                : AnalysisFailedException.invalidResponse();
    }

    /** Valida permisos y estado, y deja confirmada la marca de análisis en curso. */
    private AnalysisRequest begin(User activeUser, Long id) {
        Incident incident =
                incidentRepository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
        if (!incident.isAssignedTo(activeUser)) {
            throw new RoleNotAllowedException("Solo el técnico asignado puede analizar el incidente.");
        }
        if (incident.getStatus().isFinal()) {
            throw new IncidentClosedException(id);
        }
        Instant now = Instant.now();
        if (incident.isAnalysisInProgress(now, analysisTimeout)) {
            throw new AnalysisInProgressException(id);
        }
        incident.startAnalysis(now);
        incidentRepository.saveAndFlush(incident);
        return new AnalysisRequest(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getType().name(),
                incident.getPriority().name(),
                incident.getArea().name(),
                TOP_K,
                MIN_SCORE);
    }

    /** Libera la marca de análisis y deja la traza del resultado. */
    private void finish(User activeUser, Long id, boolean succeeded) {
        incidentRepository
                .findById(id)
                .ifPresent(
                        incident -> {
                            incident.finishAnalysis();
                            incidentRepository.save(incident);
                        });
        auditService.record(
                id,
                succeeded ? IncidentEventAction.ANALYSIS_REQUESTED : IncidentEventAction.ANALYSIS_FAILED,
                activeUser);
    }

    private static boolean hasStepWithText(AnalysisResponse answer) {
        return answer != null
                && answer.steps() != null
                && answer.steps().stream().anyMatch(step -> step != null && !step.isBlank());
    }

    private static IncidentAnalysisResponse toResponse(AnalysisResponse answer) {
        List<String> steps =
                answer.steps().stream().filter(step -> step != null && !step.isBlank()).toList();
        List<IncidentAnalysisResponse.Reference> references =
                answer.references() == null
                        ? List.of()
                        : answer.references().stream()
                                .map(
                                        ref ->
                                                new IncidentAnalysisResponse.Reference(
                                                        ref.incidentId(), ref.title(), ref.score()))
                                .toList();
        return new IncidentAnalysisResponse(answer.grounded(), steps, references);
    }
}
