package com.corporativoTI.SistemaInteligenteTI.incidents.dto;

/** Clasificación manual de un incidente con valores de catálogo (RF-6). */
//clasificar un incidente según el tipo , prioridad  y área
public record ClassifyIncidentRequest(String type, String priority, String area) {}
