package com.corporativoTI.SistemaInteligenteTI.catalog.model;

/** Catálogo cerrado de tipos de incidente (RF-5). */
public enum IncidentType {
    BUG("Bug"),
    PERFORMANCE("Rendimiento"),
    ACCESS("Acceso"),
    HARDWARE("Hardware"),
    NETWORK("Red"),
    CONFIGURATION("Configuración");

    private final String label;

    IncidentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
