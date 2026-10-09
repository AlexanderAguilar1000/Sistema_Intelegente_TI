package com.corporativoTI.SistemaInteligenteTI.incidents.model;

/** Acciones que pueden cambiar el estado de un incidente (plan, sección 4). */
public enum IncidentAction {
    ASSIGN,
    REASSIGN,
    START,
    RESOLVE,
    CLOSE_WITHOUT_SOLUTION,
    UNASSIGN_BY_AREA_CHANGE
}
