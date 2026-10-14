package com.corporativoTI.SistemaInteligenteTI.incidents.controller;

import com.corporativoTI.SistemaInteligenteTI.catalog.model.Priority;
import com.corporativoTI.SistemaInteligenteTI.incidents.dto.AssignIncidentRequest;
import com.corporativoTI.SistemaInteligenteTI.incidents.dto.ClassifyIncidentRequest;
import com.corporativoTI.SistemaInteligenteTI.incidents.dto.CloseIncidentRequest;
import com.corporativoTI.SistemaInteligenteTI.incidents.dto.CreateIncidentRequest;
import com.corporativoTI.SistemaInteligenteTI.incidents.dto.IncidentDetailResponse;
import com.corporativoTI.SistemaInteligenteTI.incidents.dto.IncidentListResponse;
import com.corporativoTI.SistemaInteligenteTI.incidents.dto.IncidentResponse;
import com.corporativoTI.SistemaInteligenteTI.incidents.dto.ResolveIncidentRequest;
import com.corporativoTI.SistemaInteligenteTI.incidents.service.IncidentService;
import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import com.corporativoTI.SistemaInteligenteTI.users.web.ActiveUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Registro (RF-3) y listado con filtro por prioridad (RF-8) detalle (RF-9) y clasificación manual (RF-6) de incidentes. */
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
    public IncidentListResponse listIncidents(
            @ActiveUser User activeUser, @RequestParam(required = false) Priority priority) {
        return incidentService.listIncidents(activeUser, priority);
    }

    @GetMapping("/{id}")
    public IncidentDetailResponse getIncident(@ActiveUser User activeUser, @PathVariable Long id) {
        return incidentService.getIncident(activeUser, id);
    }

    @PatchMapping("/{id}/classification")
    public IncidentDetailResponse classifyIncident(
            @ActiveUser User activeUser,
            @PathVariable Long id,//id del incidente 
            @RequestBody ClassifyIncidentRequest request) {//incidente cambio
        return incidentService.classifyManually(activeUser, id, request);
    }

    @PostMapping("/{id}/start")
    public IncidentDetailResponse startIncident(@ActiveUser User activeUser, @PathVariable Long id) {
        return incidentService.startIncident(activeUser, id);
    }

    @PostMapping("/{id}/resolution")
    public IncidentDetailResponse resolveIncident(
            @ActiveUser User activeUser, @PathVariable Long id, @RequestBody ResolveIncidentRequest request) {
        return incidentService.resolveIncident(activeUser, id, request);
    }

    @PostMapping("/{id}/closure")
    public IncidentDetailResponse closeIncident(
            @ActiveUser User activeUser, @PathVariable Long id, @RequestBody CloseIncidentRequest request) {
        return incidentService.closeWithoutSolution(activeUser, id, request);
    }

    @PutMapping("/{id}/assignment")
    public IncidentDetailResponse assignIncident(
            @ActiveUser User activeUser, @PathVariable Long id, @RequestBody AssignIncidentRequest request) {
        return incidentService.assignIncident(activeUser, id, request);
    }
}
