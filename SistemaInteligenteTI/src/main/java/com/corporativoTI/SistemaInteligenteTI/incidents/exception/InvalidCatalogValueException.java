package com.corporativoTI.SistemaInteligenteTI.incidents.exception;

/** Un valor recibido no pertenece al catálogo de tipos, prioridades o áreas (RF-5, RF-6). */
//reviso si el cambio del incidente esta dentro del catalogo de tipos, prioridades y áreas
public class InvalidCatalogValueException extends RuntimeException {

    public InvalidCatalogValueException(String field, String value) {
        super("El valor '" + value + "' del campo '" + field + "' no pertenece al catálogo.");
    }
}
