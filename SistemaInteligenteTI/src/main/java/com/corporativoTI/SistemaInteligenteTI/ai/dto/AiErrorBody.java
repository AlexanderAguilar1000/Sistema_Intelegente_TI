package com.corporativoTI.SistemaInteligenteTI.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Cuerpo de error homogéneo del servicio de IA (sección 3.2 del plan). */
public record AiErrorBody(@JsonProperty("error_code") String errorCode, String detail) {}
