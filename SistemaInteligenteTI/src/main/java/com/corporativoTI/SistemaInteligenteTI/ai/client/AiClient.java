package com.corporativoTI.SistemaInteligenteTI.ai.client;

import com.corporativoTI.SistemaInteligenteTI.ai.dto.AiErrorBody;
import com.corporativoTI.SistemaInteligenteTI.ai.dto.AnalysisRequest;
import com.corporativoTI.SistemaInteligenteTI.ai.dto.AnalysisResponse;
import com.corporativoTI.SistemaInteligenteTI.ai.dto.ClassificationRequest;
import com.corporativoTI.SistemaInteligenteTI.ai.dto.ClassificationResponse;
import com.corporativoTI.SistemaInteligenteTI.ai.dto.EmbeddingRequest;
import com.corporativoTI.SistemaInteligenteTI.ai.dto.EmbeddingResponse;
import com.corporativoTI.SistemaInteligenteTI.ai.model.AiResult;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Cliente HTTP del servicio de IA (sección 3.2 del plan). Spring Boot es siempre el cliente (D13).
 * Traduce 200 a {@link AiResult.Success}, 4xx a {@link AiResult.Rejected} y 5xx, plazo agotado, falta
 * de conexión o cuerpo ilegible a {@link AiResult.Unavailable}: nunca lanza al llamador (D11,
 * RNF-8). Plazos: clasificación 10 s (RNF-1), análisis 30 s (RNF-2).
 */

//Permite conectar spring boot con IA 
@Component
public class AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClient.class);

    private static final String CLASSIFICATIONS = "/ai/v1/classifications";
    private static final String ANALYSES = "/ai/v1/analyses";
    private static final String EMBEDDINGS = "/ai/v1/embeddings";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String serviceToken;
    private final Duration classificationTimeout;
    private final Duration analysisTimeout;
    private final Duration embeddingTimeout;

    public AiClient(
            ObjectMapper objectMapper,
            @Value("${ai.base-url:http://localhost:8001}") String baseUrl,
            @Value("${ai.service-token:}") String serviceToken,
            @Value("${ai.classification-timeout:10s}") Duration classificationTimeout,
            @Value("${ai.analysis-timeout:30s}") Duration analysisTimeout,
            @Value("${ai.embedding-timeout:30s}") Duration embeddingTimeout) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.serviceToken = serviceToken;
        this.classificationTimeout = classificationTimeout;
        this.analysisTimeout = analysisTimeout;
        this.embeddingTimeout = embeddingTimeout;
        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(classificationTimeout)
                        .version(HttpClient.Version.HTTP_1_1)
                        .build();
    }

    /** Clasifica un incidente (RF-4, RNF-1). */
    public AiResult<ClassificationResponse> classify(ClassificationRequest request) {
        return post(CLASSIFICATIONS, request, classificationTimeout, ClassificationResponse.class);
    }

    /** Pide la recomendación paso a paso (RF-11, RNF-2). */
    public AiResult<AnalysisResponse> analyze(AnalysisRequest request) {
        return post(ANALYSES, request, analysisTimeout, AnalysisResponse.class);
    }

    /** Indexa un incidente resuelto en el histórico (RF-13). */
    public AiResult<EmbeddingResponse> index(EmbeddingRequest request) {
        return post(EMBEDDINGS, request, embeddingTimeout, EmbeddingResponse.class);
    }

    private <T> AiResult<T> post(String path, Object body, Duration timeout, Class<T> responseType) {
        String requestId = UUID.randomUUID().toString();
        try {
            HttpRequest.Builder builder =
                    HttpRequest.newBuilder(URI.create(baseUrl + path))
                            .timeout(timeout)
                            .header("Content-Type", "application/json")
                            .header("Accept", "application/json")
                            .header("X-Request-Id", requestId)
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            objectMapper.writeValueAsString(body)));
            if (serviceToken != null && !serviceToken.isBlank()) {
                builder.header("X-Service-Token", serviceToken);
            }
            HttpResponse<String> response =
                    httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return translate(path, requestId, response, responseType);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return unavailable(path, requestId, "llamada interrumpida", e);
        } catch (Exception e) {
            // Plazo agotado, sin conexión, URL inválida o cuerpo no serializable: todo es dominio.
            return unavailable(path, requestId, e.getClass().getSimpleName(), e);
        }
    }

    private <T> AiResult<T> translate(
            String path, String requestId, HttpResponse<String> response, Class<T> responseType) {
        int status = response.statusCode();
        if (status == 200) {
            try {
                return new AiResult.Success<>(
                        objectMapper.readValue(response.body(), responseType));
            } catch (Exception e) {
                return unavailable(path, requestId, "respuesta ilegible del servicio de IA", e);
            }
        }
        if (status >= 400 && status < 500) {
            AiErrorBody error = readError(response.body());
            log.warn(
                    "IA rechazó {} (requestId={}, status={}, code={})",
                    path,
                    requestId,
                    status,
                    error.errorCode());
            return new AiResult.Rejected<>(error.errorCode(), error.detail());
        }
        return unavailable(path, requestId, "estado HTTP " + status, null);
    }

    private AiErrorBody readError(String body) {
        try {
            AiErrorBody error = objectMapper.readValue(body, AiErrorBody.class);
            if (error != null && error.errorCode() != null) {
                return error;
            }
        } catch (Exception ignored) {
            // cuerpo no JSON: se informa con un código genérico
        }
        return new AiErrorBody("AI_REJECTED", "El servicio de IA rechazó la petición");
    }

    private <T> AiResult<T> unavailable(
            String path, String requestId, String reason, Exception cause) {
        log.warn("IA no disponible en {} (requestId={}): {}", path, requestId, reason, cause);
        return new AiResult.Unavailable<>(reason);
    }
}
