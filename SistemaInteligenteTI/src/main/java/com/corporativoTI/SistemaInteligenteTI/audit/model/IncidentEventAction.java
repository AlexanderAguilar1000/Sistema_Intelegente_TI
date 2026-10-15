package com.corporativoTI.SistemaInteligenteTI.audit.model;

/**
 * Acciones que dejan traza en {@code incident_events} (RNF-6). Debe coincidir con la restricción
 * {@code incident_events_action_valid} de la migración V1.
 */
public enum IncidentEventAction {
    CREATED,
    AI_CLASSIFIED,
    CLASSIFICATION_FAILED,
    CLASSIFICATION_RETRIED,
    MANUALLY_CLASSIFIED,
    ASSIGNED,
    REASSIGNED,
    UNASSIGNED_BY_AREA_CHANGE,
    STARTED,
    ANALYSIS_REQUESTED,
    ANALYSIS_FAILED,
    RESOLVED,
    CLOSED_WITHOUT_SOLUTION,
    INDEXED,
    INDEXING_FAILED
}
