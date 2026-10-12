package com.corporativoTI.SistemaInteligenteTI.incidents.dto;

import java.util.List;

/** Listado de incidentes visibles; {@code empty} y {@code message} indican la lista vacía (RF-8). */
public record IncidentListResponse(List<IncidentResponse> incidents, boolean empty, String message) {

    private static final String EMPTY_MESSAGE = "No hay incidentes que mostrar.";
    //si la lista de incidentes esta vacia devuelve el mensaje de arriba , sino devuelve null .
    public static IncidentListResponse of(List<IncidentResponse> incidents) {
        boolean empty = incidents.isEmpty();
        return new IncidentListResponse(incidents, empty, empty ? EMPTY_MESSAGE : null);
    }
}
