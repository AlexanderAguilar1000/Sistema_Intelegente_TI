package com.corporativoTI.SistemaInteligenteTI.users.dto;

/**
 * Datos para dar de alta un técnico (RF-2). El área llega como texto para poder rechazar con un
 * mensaje propio un valor fuera del catálogo.
 */
public record CreateTechnicianRequest(String fullName, String username, String area) {}
