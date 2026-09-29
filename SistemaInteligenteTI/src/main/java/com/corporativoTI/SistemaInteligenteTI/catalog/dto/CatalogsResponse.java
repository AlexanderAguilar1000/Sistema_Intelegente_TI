package com.corporativoTI.SistemaInteligenteTI.catalog.dto;

import java.util.List;

/** Respuesta de {@code GET /api/catalogs}: los tres catálogos cerrados de RF-5. */
public record CatalogsResponse(
        List<CatalogItem> types, List<CatalogItem> priorities, List<CatalogItem> areas) {}
