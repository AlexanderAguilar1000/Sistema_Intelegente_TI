package com.corporativoTI.SistemaInteligenteTI.users.controller;

import com.corporativoTI.SistemaInteligenteTI.users.dto.CreateTechnicianRequest;
import com.corporativoTI.SistemaInteligenteTI.users.dto.UserResponse;
import com.corporativoTI.SistemaInteligenteTI.users.service.TechnicianService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Alta de técnicos con su área (RF-2). */
@RestController
@RequestMapping("/api/technicians")
@RequiredArgsConstructor
public class TechnicianController {

    private final TechnicianService technicianService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createTechnician(@RequestBody CreateTechnicianRequest request) {
        return technicianService.createTechnician(request);
    }
}
