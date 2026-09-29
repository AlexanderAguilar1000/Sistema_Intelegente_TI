package com.corporativoTI.SistemaInteligenteTI.catalog.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * RF-5: los catálogos son cerrados. Si alguien añade o quita un valor a
 * {@link IncidentType}, {@link Priority} o {@link Area}, estos tests deben fallar.
 */
class CatalogEnumsTest {

    @Test
    void incidentTypeHasExactlyTheValuesOfRf5() {
        assertThat(IncidentType.values())
                .extracting(Enum::name)
                .containsExactlyInAnyOrder(
                        "BUG", "PERFORMANCE", "ACCESS", "HARDWARE", "NETWORK", "CONFIGURATION");
    }

    @Test
    void priorityHasExactlyTheValuesOfRf5() {
        assertThat(Priority.values())
                .extracting(Enum::name)
                .containsExactlyInAnyOrder("HIGH", "MEDIUM", "LOW");
    }

    @Test
    void areaHasExactlyTheValuesOfRf5() {
        assertThat(Area.values())
                .extracting(Enum::name)
                .containsExactlyInAnyOrder(
                        "INFRASTRUCTURE", "APPLICATIONS", "DATABASE", "SECURITY", "USER_SUPPORT");
    }

    @Test
    void incidentTypeLabelsAreInSpanish() {
        assertThat(IncidentType.BUG.getLabel()).isEqualTo("Bug");
        assertThat(IncidentType.PERFORMANCE.getLabel()).isEqualTo("Rendimiento");
        assertThat(IncidentType.ACCESS.getLabel()).isEqualTo("Acceso");
        assertThat(IncidentType.HARDWARE.getLabel()).isEqualTo("Hardware");
        assertThat(IncidentType.NETWORK.getLabel()).isEqualTo("Red");
        assertThat(IncidentType.CONFIGURATION.getLabel()).isEqualTo("Configuración");
    }

    @Test
    void priorityLabelsAreInSpanish() {
        assertThat(Priority.HIGH.getLabel()).isEqualTo("Alta");
        assertThat(Priority.MEDIUM.getLabel()).isEqualTo("Media");
        assertThat(Priority.LOW.getLabel()).isEqualTo("Baja");
    }

    @Test
    void areaLabelsAreInSpanish() {
        assertThat(Area.INFRASTRUCTURE.getLabel()).isEqualTo("Infraestructura");
        assertThat(Area.APPLICATIONS.getLabel()).isEqualTo("Aplicaciones");
        assertThat(Area.DATABASE.getLabel()).isEqualTo("Base de datos");
        assertThat(Area.SECURITY.getLabel()).isEqualTo("Seguridad");
        assertThat(Area.USER_SUPPORT.getLabel()).isEqualTo("Soporte a usuario");
    }
}
