package com.corporativoTI.SistemaInteligenteTI.incidents.controller;

import com.corporativoTI.SistemaInteligenteTI.incidents.dto.CreateIncidentRequest;
import com.corporativoTI.SistemaInteligenteTI.incidents.dto.IncidentResponse;
import com.corporativoTI.SistemaInteligenteTI.incidents.service.IncidentService;
import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import com.corporativoTI.SistemaInteligenteTI.users.web.ActiveUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Registro (RF-3) y listado (RF-8) de incidentes. */
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

    @GetMapping
    public List<IncidentResponse> listIncidents(@ActiveUser User activeUser) {
        return incidentService.listIncidents(activeUser);
    }
}
