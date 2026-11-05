package com.corporativoTI.SistemaInteligenteTI.incidents.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.corporativoTI.SistemaInteligenteTI.ai.client.AiClient;
import com.corporativoTI.SistemaInteligenteTI.ai.dto.ClassificationRequest;
import com.corporativoTI.SistemaInteligenteTI.ai.dto.ClassificationResponse;
import com.corporativoTI.SistemaInteligenteTI.ai.model.AiResult;
import com.corporativoTI.SistemaInteligenteTI.catalog.model.Area;
import com.corporativoTI.SistemaInteligenteTI.catalog.model.IncidentType;
import com.corporativoTI.SistemaInteligenteTI.catalog.model.Priority;
import com.corporativoTI.SistemaInteligenteTI.incidents.dto.CreateIncidentRequest;
import com.corporativoTI.SistemaInteligenteTI.incidents.dto.IncidentResponse;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.ClassificationStatus;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.Incident;
import com.corporativoTI.SistemaInteligenteTI.incidents.model.IncidentStatus;
import com.corporativoTI.SistemaInteligenteTI.incidents.repository.IncidentRepository;
import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import com.corporativoTI.SistemaInteligenteTI.users.repository.UserRepository;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF-3, RF-4, RNF-1 (T-30): al registrar un incidente se clasifica con la IA en el mismo registro;
 * si la IA no contesta a tiempo, falla o responde algo inválido, el incidente se guarda igualmente
 * como pendiente. El cliente de IA está simulado (RNF-9): su plazo real de 10 s ya lo cubre T-29.
 */
@SpringBootTest
@Transactional
class IncidentRegistrationClassificationTest {

    @Autowired private IncidentService incidentService;
    @Autowired private UserRepository userRepository;
    @Autowired private IncidentRepository incidentRepository;

    @MockitoBean private AiClient aiClient;

    private User supervisor() {
        return userRepository.findByUsername("laura.supervisora").orElseThrow();
    }

    private IncidentResponse register() {
        return incidentService.registerIncident(
                supervisor(),
                new CreateIncidentRequest(
                        "El correo no sincroniza", "Outlook no descarga mensajes nuevos desde ayer."));
    }

    private static ClassificationResponse classification(String type, String priority, String area) {
        return new ClassificationResponse(type, priority, area, "modelo-simulado", 1200);
    }

    @Test
    void classifiesTheIncidentInTheSameRecordWhenTheAiAnswers() {
        when(aiClient.classify(any()))
                .thenReturn(new AiResult.Success<>(classification("ACCESS", "HIGH", "SECURITY")));

        IncidentResponse response = register();

        assertThat(response.classificationStatus()).isEqualTo("AI_CLASSIFIED");
        assertThat(response.type()).isEqualTo("ACCESS");
        assertThat(response.priority()).isEqualTo("HIGH");
        assertThat(response.area()).isEqualTo("SECURITY");
        assertThat(response.status()).isEqualTo("REGISTERED");

        Incident saved = incidentRepository.findById(response.id()).orElseThrow();
        assertThat(saved.getClassificationStatus()).isEqualTo(ClassificationStatus.AI_CLASSIFIED);
        assertThat(saved.getType()).isEqualTo(IncidentType.ACCESS);
        assertThat(saved.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(saved.getArea()).isEqualTo(Area.SECURITY);
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.REGISTERED);
    }

    @Test
    void sendsTheIncidentTextAndTheWholeCatalogToTheAi() {
        when(aiClient.classify(any()))
                .thenReturn(new AiResult.Success<>(classification("BUG", "LOW", "APPLICATIONS")));

        IncidentResponse response = register();

        ArgumentCaptor<ClassificationRequest> captor =
                ArgumentCaptor.forClass(ClassificationRequest.class);
        verify(aiClient).classify(captor.capture());
        ClassificationRequest sent = captor.getValue();
        assertThat(sent.incidentId()).isEqualTo(response.id());
        assertThat(sent.title()).isEqualTo("El correo no sincroniza");
        assertThat(sent.description()).isEqualTo("Outlook no descarga mensajes nuevos desde ayer.");
        assertThat(sent.catalog().types())
                .containsExactlyInAnyOrderElementsOf(
                        Arrays.stream(IncidentType.values()).map(Enum::name).toList());
        assertThat(sent.catalog().priorities())
                .containsExactlyInAnyOrderElementsOf(
                        Arrays.stream(Priority.values()).map(Enum::name).toList());
        assertThat(sent.catalog().areas())
                .containsExactlyInAnyOrderElementsOf(
                        Arrays.stream(Area.values()).map(Enum::name).toList());
    }

    @Test
    void savesTheIncidentAsPendingWhenTheAiIsUnavailableOrTimesOut() {
        when(aiClient.classify(any())).thenReturn(new AiResult.Unavailable<>("HttpTimeoutException"));

        IncidentResponse response = register();

        assertSavedAsPending(response);
    }

    @Test
    void savesTheIncidentAsPendingWhenTheAiRejectsTheRequest() {
        when(aiClient.classify(any()))
                .thenReturn(new AiResult.Rejected<>("INVALID_CLASSIFICATION", "valor fuera de catálogo"));

        IncidentResponse response = register();

        assertSavedAsPending(response);
    }

    @Test
    void savesTheIncidentAsPendingWhenTheAiAnswersOutsideTheCatalog() {
        when(aiClient.classify(any()))
                .thenReturn(new AiResult.Success<>(classification("MAGIA", "HIGH", "SECURITY")));

        IncidentResponse response = register();

        assertSavedAsPending(response);
    }

    @Test
    void savesTheIncidentAsPendingWhenTheAiAnswerIsIncomplete() {
        when(aiClient.classify(any()))
                .thenReturn(new AiResult.Success<>(classification("ACCESS", null, "SECURITY")));

        IncidentResponse response = register();

        assertSavedAsPending(response);
    }

    private void assertSavedAsPending(IncidentResponse response) {
        assertThat(response.classificationStatus()).isEqualTo("PENDING");
        assertThat(response.type()).isNull();
        assertThat(response.priority()).isNull();
        assertThat(response.area()).isNull();

        Incident saved = incidentRepository.findById(response.id()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.REGISTERED);
        assertThat(saved.getClassificationStatus()).isEqualTo(ClassificationStatus.PENDING);
        assertThat(saved.getType()).isNull();
        assertThat(saved.getPriority()).isNull();
        assertThat(saved.getArea()).isNull();
    }
}
