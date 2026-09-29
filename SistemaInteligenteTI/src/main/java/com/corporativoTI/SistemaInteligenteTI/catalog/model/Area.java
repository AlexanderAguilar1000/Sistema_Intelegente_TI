package com.corporativoTI.SistemaInteligenteTI.catalog.model;

/** Catálogo cerrado de áreas técnicas responsables de un incidente (RF-5). */
//Area es el tipo de Enum 
//INFRAESTRUCTURE es una constante de este tipo 
public enum Area {
    //lo que esta dentro del parentesis asi se inicializa este valor para que esta constante tenga ese valor es como x="informacion"
    INFRASTRUCTURE("Infraestructura"),
    APPLICATIONS("Aplicaciones"),
    DATABASE("Base de datos"),
    SECURITY("Seguridad"),
    USER_SUPPORT("Soporte a usuario");

    private final String label;

    Area(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
