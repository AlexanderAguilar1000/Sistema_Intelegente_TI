package com.corporativoTI.SistemaInteligenteTI.ai.dto;

import java.util.List;

/** Catálogo enviado al servicio de IA en cada clasificación (D4). */
public record AiCatalog(List<String> types, List<String> priorities, List<String> areas) {}
