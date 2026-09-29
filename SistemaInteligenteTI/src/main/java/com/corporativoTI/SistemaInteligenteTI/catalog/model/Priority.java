package com.corporativoTI.SistemaInteligenteTI.catalog.model;

/** Catálogo cerrado de prioridades de incidente (RF-5). */
public enum Priority {
    HIGH("Alta"),
    MEDIUM("Media"),
    LOW("Baja");

    private final String label;

    Priority(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
