package com.corporativoTI.SistemaInteligenteTI.incidents.exception;

/** El número o el tamaño de página pedido en el listado no es válido (RF-8). */
public class InvalidPageParameterException extends RuntimeException {

    public InvalidPageParameterException(String parameter, int value, String rule) {
        super("Valor no válido para el parámetro '" + parameter + "': " + value + ". " + rule);
    }
}
