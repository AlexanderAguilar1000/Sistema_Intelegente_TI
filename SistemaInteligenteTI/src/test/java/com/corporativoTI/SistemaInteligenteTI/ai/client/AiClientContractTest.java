package com.corporativoTI.SistemaInteligenteTI.ai.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.corporativoTI.SistemaInteligenteTI.ai.dto.AiCatalog;
import com.corporativoTI.SistemaInteligenteTI.ai.dto.AnalysisRequest;
import com.corporativoTI.SistemaInteligenteTI.ai.dto.AnalysisResponse;
import com.corporativoTI.SistemaInteligenteTI.ai.dto.ClassificationRequest;
import com.corporativoTI.SistemaInteligenteTI.ai.dto.ClassificationResponse;
import com.corporativoTI.SistemaInteligenteTI.ai.dto.EmbeddingRequest;
import com.corporativoTI.SistemaInteligenteTI.ai.dto.EmbeddingResponse;
import com.corporativoTI.SistemaInteligenteTI.ai.model.AiResult;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.json.JsonMapper;

/**
 * Test de contrato del cliente de IA (RF-4, RF-11, RF-13, RNF-1, RNF-2, RNF-8) contra un servidor
 * HTTP simulado del JDK: 200, 422, 503 y plazo agotado en los tres endpoints de la sección 3.2 del
 * plan. En ningún caso debe subir una excepción al llamador (D11).
 */
class AiClientContractTest {

    private static final Duration SHORT_TIMEOUT = Duration.ofMillis(300);
    private static final long SLOW_RESPONSE_MS = 1500;

    private HttpServer server;
    private AiClient client;
    private final AtomicReference<String> lastPath = new AtomicReference<>();
    private final AtomicReference<String> lastToken = new AtomicReference<>();
    private final AtomicReference<String> lastRequestId = new AtomicReference<>();
    private final AtomicReference<String> lastBody = new AtomicReference<>();

    /** Respuesta que el servidor simulado devolverá en la siguiente llamada. */
    private volatile int stubStatus;

    private volatile String stubBody;
    private volatile long stubDelayMs;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/",
                exchange -> {
                    lastPath.set(exchange.getRequestURI().getPath());
                    lastToken.set(exchange.getRequestHeaders().getFirst("X-Service-Token"));
                    lastRequestId.set(exchange.getRequestHeaders().getFirst("X-Request-Id"));
                    lastBody.set(
                            new String(
                                    exchange.getRequestBody().readAllBytes(),
                                    StandardCharsets.UTF_8));
                    try {
                        if (stubDelayMs > 0) {
                            Thread.sleep(stubDelayMs);
                        }
                        byte[] bytes = stubBody.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().add("Content-Type", "application/json");
                        exchange.sendResponseHeaders(stubStatus, bytes.length);
                        exchange.getResponseBody().write(bytes);
                    } catch (Exception ignored) {
                        // el cliente ya cerró la conexión por plazo agotado
                    } finally {
                        exchange.close();
                    }
                });
        server.start();
        client = newClient("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private static AiClient newClient(String baseUrl) {
        return new AiClient(
                JsonMapper.builder().build(),
                baseUrl,
                "secret-token",
                SHORT_TIMEOUT,
                SHORT_TIMEOUT,
                SHORT_TIMEOUT);
    }

    private void stub(int status, String body) {
        stubStatus = status;
        stubBody = body;
        stubDelayMs = 0;
    }

    // ---- Los tres endpoints como casos parametrizados -----------------------------------------

    private record Endpoint(
            String name,
            String path,
            String okBody,
            Function<AiClient, AiResult<?>> call,
            Function<Object, Boolean> checkOk) {
        @Override
        public String toString() {
            return name;
        }
    }

    private static final AiCatalog CATALOG =
            new AiCatalog(
                    List.of("BUG", "ACCESS"),
                    List.of("HIGH", "LOW"),
                    List.of("APPLICATIONS", "DATABASE"));

    private static Endpoint classification() {
        return new Endpoint(
                "classifications",
                "/ai/v1/classifications",
                "{\"type\":\"BUG\",\"priority\":\"HIGH\",\"area\":\"APPLICATIONS\","
                        + "\"model\":\"llama\",\"elapsed_ms\":1840}",
                c ->
                        c.classify(
                                new ClassificationRequest(42L, "Falla", "No abre la app", CATALOG)),
                r -> {
                    ClassificationResponse v = (ClassificationResponse) r;
                    return v.type().equals("BUG")
                            && v.priority().equals("HIGH")
                            && v.area().equals("APPLICATIONS")
                            && v.elapsedMs() == 1840;
                });
    }

    private static Endpoint analysis() {
        return new Endpoint(
                "analyses",
                "/ai/v1/analyses",
                "{\"grounded\":true,\"steps\":[\"Revisar el log\",\"Reiniciar el servicio\"],"
                        + "\"references\":[{\"incident_id\":7,\"title\":\"Caso previo\",\"score\":0.83}],"
                        + "\"model\":\"llama\",\"elapsed_ms\":9120}",
                c ->
                        c.analyze(
                                new AnalysisRequest(
                                        42L,
                                        "Falla",
                                        "No abre la app",
                                        "BUG",
                                        "HIGH",
                                        "APPLICATIONS",
                                        3,
                                        0.75)),
                r -> {
                    AnalysisResponse v = (AnalysisResponse) r;
                    return v.grounded()
                            && v.steps().size() == 2
                            && v.references().get(0).incidentId() == 7L
                            && v.references().get(0).score() == 0.83;
                });
    }

    private static Endpoint embeddings() {
        return new Endpoint(
                "embeddings",
                "/ai/v1/embeddings",
                "{\"indexed\":true,\"model\":\"minilm\",\"dimensions\":384}",
                c ->
                        c.index(
                                new EmbeddingRequest(
                                        42L,
                                        "Falla",
                                        "No abre la app",
                                        "Se reinició el servicio de aplicaciones",
                                        "BUG",
                                        "APPLICATIONS")),
                r -> {
                    EmbeddingResponse v = (EmbeddingResponse) r;
                    return v.indexed() && v.dimensions() == 384;
                });
    }

    static Stream<Arguments> endpoints() {
        return Stream.of(
                Arguments.of(classification()),
                Arguments.of(analysis()),
                Arguments.of(embeddings()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    void status200ReturnsSuccessWithParsedBody(Endpoint endpoint) {
        stub(200, endpoint.okBody());

        AiResult<?> result = endpoint.call().apply(client);

        assertThat(result).isInstanceOf(AiResult.Success.class);
        Object value = ((AiResult.Success<?>) result).value();
        assertThat(endpoint.checkOk().apply(value)).isTrue();
        assertThat(lastPath.get()).isEqualTo(endpoint.path());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    void status422BecomesRejectedWithErrorCode(Endpoint endpoint) {
        stub(
                422,
                "{\"error_code\":\"INVALID_CLASSIFICATION\",\"detail\":\"valor fuera de catálogo\"}");

        AiResult<?> result = endpoint.call().apply(client);

        assertThat(result).isInstanceOf(AiResult.Rejected.class);
        AiResult.Rejected<?> rejected = (AiResult.Rejected<?>) result;
        assertThat(rejected.errorCode()).isEqualTo("INVALID_CLASSIFICATION");
        assertThat(rejected.detail()).isEqualTo("valor fuera de catálogo");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    void status503BecomesUnavailable(Endpoint endpoint) {
        stub(503, "{\"error_code\":\"AI_UNAVAILABLE\",\"detail\":\"Groq no responde\"}");

        AiResult<?> result = endpoint.call().apply(client);

        assertThat(result).isInstanceOf(AiResult.Unavailable.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    void timeoutBecomesUnavailableWithoutThrowing(Endpoint endpoint) {
        stub(200, endpoint.okBody());
        stubDelayMs = SLOW_RESPONSE_MS;

        AtomicReference<AiResult<?>> holder = new AtomicReference<>();
        assertThatCode(() -> holder.set(endpoint.call().apply(client)))
                .doesNotThrowAnyException();

        assertThat(holder.get()).isInstanceOf(AiResult.Unavailable.class);
    }

    // ---- Casos límite que también deben quedar como resultado de dominio ----------------------

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    void unreadableErrorBodyDoesNotBreakTheClient(Endpoint endpoint) {
        stub(422, "esto no es json");

        AiResult<?> result = endpoint.call().apply(client);

        assertThat(result).isInstanceOf(AiResult.Rejected.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    void malformedSuccessBodyBecomesUnavailable(Endpoint endpoint) {
        stub(200, "<html>no es json</html>");

        AiResult<?> result = endpoint.call().apply(client);

        assertThat(result).isInstanceOf(AiResult.Unavailable.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    void unreachableServerBecomesUnavailable(Endpoint endpoint) {
        AiClient unreachable = newClient("http://127.0.0.1:1");

        AiResult<?> result = endpoint.call().apply(unreachable);

        assertThat(result).isInstanceOf(AiResult.Unavailable.class);
    }

    // ---- Reglas transversales del contrato (sección 3.3) --------------------------------------

    @Test
    void sendsServiceTokenRequestIdAndSnakeCaseBody() {
        Endpoint endpoint = classification();
        stub(200, endpoint.okBody());

        endpoint.call().apply(client);

        assertThat(lastToken.get()).isEqualTo("secret-token");
        assertThat(lastRequestId.get()).isNotBlank();
        assertThat(lastBody.get())
                .contains("\"incident_id\":42")
                .contains("\"catalog\"")
                .contains("\"types\":[\"BUG\",\"ACCESS\"]");
    }

    @Test
    void analysisBodyUsesSnakeCaseNames() {
        Endpoint endpoint = analysis();
        stub(200, endpoint.okBody());

        endpoint.call().apply(client);

        assertThat(lastBody.get()).contains("\"top_k\":3").contains("\"min_score\":0.75");
    }

    @Test
    void groundedFalseIsASuccessNotAnError() {
        stub(
                200,
                "{\"grounded\":false,\"steps\":[\"Revisar\"],\"references\":[],"
                        + "\"model\":\"m\",\"elapsed_ms\":4100}");

        AiResult<?> result = analysis().call().apply(client);

        assertThat(result).isInstanceOf(AiResult.Success.class);
        AnalysisResponse value = (AnalysisResponse) ((AiResult.Success<?>) result).value();
        assertThat(value.grounded()).isFalse();
        assertThat(value.references()).isEmpty();
    }
}
