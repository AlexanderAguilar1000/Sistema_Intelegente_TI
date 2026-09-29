package com.corporativoTI.SistemaInteligenteTI.catalog.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * RF-5: {@code GET /api/catalogs} debe devolver exactamente los valores del
 * catálogo cerrado de tipo, prioridad y área.
 */
@WebMvcTest(CatalogController.class)
class CatalogControllerTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void returnsExactlyTheThreeClosedCatalogsOfRf5() throws Exception {
        mockMvc.perform(get("/api/catalogs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.types.length()").value(6))
                .andExpect(
                        jsonPath("$.types[*].value")
                                .value(
                                        containsInAnyOrder(
                                                "BUG",
                                                "PERFORMANCE",
                                                "ACCESS",
                                                "HARDWARE",
                                                "NETWORK",
                                                "CONFIGURATION")))
                .andExpect(
                        jsonPath("$.types[*].label")
                                .value(
                                        containsInAnyOrder(
                                                "Bug",
                                                "Rendimiento",
                                                "Acceso",
                                                "Hardware",
                                                "Red",
                                                "Configuración")))
                .andExpect(jsonPath("$.priorities.length()").value(3))
                .andExpect(
                        jsonPath("$.priorities[*].value")
                                .value(containsInAnyOrder("HIGH", "MEDIUM", "LOW")))
                .andExpect(
                        jsonPath("$.priorities[*].label")
                                .value(containsInAnyOrder("Alta", "Media", "Baja")))
                .andExpect(jsonPath("$.areas.length()").value(5))
                .andExpect(
                        jsonPath("$.areas[*].value")
                                .value(
                                        containsInAnyOrder(
                                                "INFRASTRUCTURE",
                                                "APPLICATIONS",
                                                "DATABASE",
                                                "SECURITY",
                                                "USER_SUPPORT")))
                .andExpect(
                        jsonPath("$.areas[*].label")
                                .value(
                                        containsInAnyOrder(
                                                "Infraestructura",
                                                "Aplicaciones",
                                                "Base de datos",
                                                "Seguridad",
                                                "Soporte a usuario")));
    }
}
