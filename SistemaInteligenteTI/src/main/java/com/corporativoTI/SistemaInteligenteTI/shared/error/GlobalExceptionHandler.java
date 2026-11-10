package com.corporativoTI.SistemaInteligenteTI.shared.error;

import com.corporativoTI.SistemaInteligenteTI.incidents.exception.AnalysisFailedException;
import com.corporativoTI.SistemaInteligenteTI.incidents.exception.AnalysisInProgressException;
import com.corporativoTI.SistemaInteligenteTI.incidents.exception.ClassificationNotPendingException;
import com.corporativoTI.SistemaInteligenteTI.incidents.exception.IncidentClosedException;
import com.corporativoTI.SistemaInteligenteTI.incidents.exception.IncidentNotClassifiedException;
import com.corporativoTI.SistemaInteligenteTI.incidents.exception.IncidentNotFoundException;
import com.corporativoTI.SistemaInteligenteTI.incidents.exception.IncidentNotResolvedException;
import com.corporativoTI.SistemaInteligenteTI.incidents.exception.InvalidCatalogValueException;
import com.corporativoTI.SistemaInteligenteTI.incidents.exception.InvalidTechnicianException;
import com.corporativoTI.SistemaInteligenteTI.incidents.exception.SolutionTooShortException;
import com.corporativoTI.SistemaInteligenteTI.incidents.exception.TechnicianAreaMismatchException;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.InvalidTransitionException;
import com.corporativoTI.SistemaInteligenteTI.users.exception.InvalidAreaException;
import com.corporativoTI.SistemaInteligenteTI.users.exception.MissingActiveUserException;
import com.corporativoTI.SistemaInteligenteTI.users.exception.MissingFieldException;
import com.corporativoTI.SistemaInteligenteTI.users.exception.RoleNotAllowedException;
import com.corporativoTI.SistemaInteligenteTI.users.exception.UnknownActiveUserException;
import com.corporativoTI.SistemaInteligenteTI.users.exception.UsernameAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Traduce las excepciones de dominio a la respuesta de error homogénea de la API. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MissingActiveUserException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingActiveUser(MissingActiveUserException ex) {
        return new ErrorResponse("MISSING_ACTIVE_USER", ex.getMessage());
    }

    @ExceptionHandler(UnknownActiveUserException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnknownActiveUser(UnknownActiveUserException ex) {
        return new ErrorResponse("UNKNOWN_ACTIVE_USER", ex.getMessage());
    }

    @ExceptionHandler(RoleNotAllowedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleRoleNotAllowed(RoleNotAllowedException ex) {
        return new ErrorResponse("FORBIDDEN_ROLE", ex.getMessage());
    }

    @ExceptionHandler(IncidentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleIncidentNotFound(IncidentNotFoundException ex) {
        return new ErrorResponse("INCIDENT_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(IncidentClosedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleIncidentClosed(IncidentClosedException ex) {
        return new ErrorResponse("INCIDENT_CLOSED", ex.getMessage());
    }

    @ExceptionHandler(IncidentNotClassifiedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleIncidentNotClassified(IncidentNotClassifiedException ex) {
        return new ErrorResponse("INCIDENT_NOT_CLASSIFIED", ex.getMessage());
    }

    @ExceptionHandler(IncidentNotResolvedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleIncidentNotResolved(IncidentNotResolvedException ex) {
        return new ErrorResponse("INCIDENT_NOT_RESOLVED", ex.getMessage());
    }

    @ExceptionHandler(ClassificationNotPendingException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleClassificationNotPending(ClassificationNotPendingException ex) {
        return new ErrorResponse("CLASSIFICATION_NOT_PENDING", ex.getMessage());
    }

    @ExceptionHandler(AnalysisInProgressException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleAnalysisInProgress(AnalysisInProgressException ex) {
        return new ErrorResponse("ANALYSIS_IN_PROGRESS", ex.getMessage());
    }

    /** Fallo de la IA en el análisis (RF-11): 503 si no responde, 502 si responde algo inválido. */
    @ExceptionHandler(AnalysisFailedException.class)
    public ResponseEntity<ErrorResponse> handleAnalysisFailed(AnalysisFailedException ex) {
        HttpStatus status = ex.isUnavailable() ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY;
        String code = ex.isUnavailable() ? "AI_UNAVAILABLE" : "AI_INVALID_RESPONSE";
        return ResponseEntity.status(status).body(new ErrorResponse(code, ex.getMessage()));
    }

    @ExceptionHandler(TechnicianAreaMismatchException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleTechnicianAreaMismatch(TechnicianAreaMismatchException ex) {
        return new ErrorResponse("TECHNICIAN_AREA_MISMATCH", ex.getMessage());
    }

    @ExceptionHandler(InvalidTechnicianException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleInvalidTechnician(InvalidTechnicianException ex) {
        return new ErrorResponse("INVALID_TECHNICIAN", ex.getMessage());
    }

    @ExceptionHandler(InvalidCatalogValueException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleInvalidCatalogValue(InvalidCatalogValueException ex) {
        return new ErrorResponse("INVALID_CATALOG_VALUE", ex.getMessage());
    }

    @ExceptionHandler(MissingFieldException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleMissingField(MissingFieldException ex) {
        return new ErrorResponse("MISSING_FIELD", ex.getMessage());
    }

    @ExceptionHandler(SolutionTooShortException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleSolutionTooShort(SolutionTooShortException ex) {
        return new ErrorResponse("SOLUTION_TOO_SHORT", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleInvalidParameter(MethodArgumentTypeMismatchException ex) {
        return new ErrorResponse(
                "INVALID_PARAMETER",
                "Valor no válido para el parámetro '" + ex.getName() + "': " + ex.getValue());
    }

    @ExceptionHandler(InvalidAreaException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleInvalidArea(InvalidAreaException ex) {
        return new ErrorResponse("INVALID_AREA", ex.getMessage());
    }

    @ExceptionHandler(InvalidTransitionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleInvalidTransition(InvalidTransitionException ex) {
        return new ErrorResponse("INVALID_TRANSITION", ex.getMessage());
    }

    /** Bloqueo optimista (D8): otra acción confirmó un cambio sobre el mismo incidente antes que ésta. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConcurrentModification(ObjectOptimisticLockingFailureException ex) {
        return new ErrorResponse(
                "CONCURRENT_MODIFICATION",
                "El incidente fue modificado por otra acción al mismo tiempo. Recarga e inténtalo de nuevo.");
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleUsernameAlreadyExists(UsernameAlreadyExistsException ex) {
        return new ErrorResponse("USERNAME_ALREADY_EXISTS", ex.getMessage());
    }
}
