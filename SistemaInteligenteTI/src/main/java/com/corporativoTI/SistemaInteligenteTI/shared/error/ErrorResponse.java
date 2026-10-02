package com.corporativoTI.SistemaInteligenteTI.shared.error;

/** Forma homogénea de todo error de la API: código de dominio y mensaje en español. */
public record ErrorResponse(String errorCode, String message) {}
