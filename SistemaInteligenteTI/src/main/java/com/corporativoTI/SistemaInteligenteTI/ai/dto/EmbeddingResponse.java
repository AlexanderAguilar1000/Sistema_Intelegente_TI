package com.corporativoTI.SistemaInteligenteTI.ai.dto;

/** Respuesta 200 de {@code POST /ai/v1/embeddings}. */
public record EmbeddingResponse(boolean indexed, String model, int dimensions) {}
