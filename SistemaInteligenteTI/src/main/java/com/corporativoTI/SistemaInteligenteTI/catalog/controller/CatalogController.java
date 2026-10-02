package com.corporativoTI.SistemaInteligenteTI.catalog.controller;

import com.corporativoTI.SistemaInteligenteTI.catalog.dto.CatalogItem;
import com.corporativoTI.SistemaInteligenteTI.catalog.dto.CatalogsResponse;
import com.corporativoTI.SistemaInteligenteTI.catalog.model.Area;
import com.corporativoTI.SistemaInteligenteTI.catalog.model.IncidentType;
import com.corporativoTI.SistemaInteligenteTI.catalog.model.Priority;
import java.util.Arrays;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Publica los catálogos cerrados de tipo, prioridad y área (RF-5). */
@RestController
@RequestMapping("/api/catalogs")
public class CatalogController {

    @GetMapping
    public CatalogsResponse getCatalogs() {
        //Catalog response es asignar datos  , aqui con CatalogsResponse estoy creando una 3 arreglos con los valores enum
        return new CatalogsResponse(
                Arrays.stream(IncidentType.values())//extrae los valores de un enum y lo convierte en una arreglo 
                         //el type.name reppresena el tipo de la constante  , ejemplo Infraestructure , etc  y el label el valor 
                        .map(type -> new CatalogItem(type.name(), type.getLabel()))
                        .toList(),
                Arrays.stream(Priority.values())
                        .map(priority -> new CatalogItem(priority.name(), priority.getLabel()))
                        .toList(),
                Arrays.stream(Area.values())
                        .map(area -> new CatalogItem(area.name(), area.getLabel()))
                        .toList());
    }
}
