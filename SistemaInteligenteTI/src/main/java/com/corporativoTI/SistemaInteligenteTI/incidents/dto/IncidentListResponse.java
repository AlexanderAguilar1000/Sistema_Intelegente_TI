package com.corporativoTI.SistemaInteligenteTI.incidents.dto;

import java.util.List;

/**
 * Una página del listado de incidentes visibles (RF-8). {@code empty} y {@code message} indican que
 * la página no trae incidentes; los totales se refieren a todas las páginas. Es un contrato propio
 * para no exponer el {@code Page} de Spring.
 */
public record IncidentListResponse(
        List<IncidentResponse> incidents,
        boolean empty,
        String message,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    private static final String EMPTY_MESSAGE = "No hay incidentes que mostrar.";

    //si la lista de incidentes esta vacia devuelve el mensaje de arriba , sino devuelve null .
    public static IncidentListResponse of(
            List<IncidentResponse> incidents, int page, int size, long totalElements, int totalPages) {
        boolean empty = incidents.isEmpty();
        return new IncidentListResponse(
                incidents,
                empty,
                empty ? EMPTY_MESSAGE : null,
                page,
                size,
                totalElements,
                totalPages);
    }
}
