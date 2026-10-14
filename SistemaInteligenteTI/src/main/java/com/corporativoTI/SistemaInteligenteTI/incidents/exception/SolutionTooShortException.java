package com.corporativoTI.SistemaInteligenteTI.incidents.exception;

/** La solución no alcanza la longitud mínima exigida para resolver (se traduce a 422). */
public class SolutionTooShortException extends RuntimeException {

    public SolutionTooShortException(int minLength) {
        super("La solución debe tener al menos " + minLength + " caracteres.");
    }
}
