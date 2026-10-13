package com.corporativoTI.SistemaInteligenteTI.incidents.exception;

import com.corporativoTI.SistemaInteligenteTI.catalog.model.Area;

/** El técnico no pertenece al área del incidente (se traduce a 409). */
public class TechnicianAreaMismatchException extends RuntimeException {

    public TechnicianAreaMismatchException(Area technicianArea, Area incidentArea) {
        super(
                "El técnico es del área "
                        + technicianArea
                        + " y el incidente pertenece al área "
                        + incidentArea
                        + ".");
    }
}
