package com.corporativoTI.SistemaInteligenteTI.ai.model;

/**
 * Resultado de dominio de una llamada al servicio de IA (D11, RNF-8). El cliente nunca lanza
 * excepciones al llamador: cualquier fallo externo se traduce a {@link Rejected} o {@link
 * Unavailable}.
 */
public sealed interface AiResult<T> {

    /** El servicio respondió 200 con un cuerpo válido. */
    record Success<T>(T value) implements AiResult<T> {}

    /** El servicio rechazó la petición (4xx, p. ej. 422 por valor fuera de catálogo). */
    record Rejected<T>(String errorCode, String detail) implements AiResult<T> {}

    /** El servicio no está disponible: 5xx, plazo agotado, sin conexión o respuesta ilegible. */
    record Unavailable<T>(String reason) implements AiResult<T> {}
}
