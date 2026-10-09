package com.corporativoTI.SistemaInteligenteTI.incidents.controller;

import com.corporativoTI.SistemaInteligenteTI.incidents.dto.CreateIncidentRequest;
import com.corporativoTI.SistemaInteligenteTI.incidents.dto.IncidentResponse;
import com.corporativoTI.SistemaInteligenteTI.incidents.service.IncidentService;
import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import com.corporativoTI.SistemaInteligenteTI.users.web.ActiveUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Registro de incidentes (RF-3). */
@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentResponse registerIncident(
            @ActiveUser User activeUser, @RequestBody CreateIncidentRequest request) {
        return incidentService.registerIncident(activeUser, request);
    }
}
