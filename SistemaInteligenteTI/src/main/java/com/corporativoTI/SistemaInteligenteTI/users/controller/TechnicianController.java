package com.corporativoTI.SistemaInteligenteTI.users.controller;

import com.corporativoTI.SistemaInteligenteTI.users.dto.CreateTechnicianRequest;
import com.corporativoTI.SistemaInteligenteTI.users.dto.UserResponse;
import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import com.corporativoTI.SistemaInteligenteTI.users.service.TechnicianService;
import com.corporativoTI.SistemaInteligenteTI.users.web.ActiveUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Alta de técnicos con su área (RF-2) y consulta de técnicos por área (RF-7). */
@RestController
@RequestMapping("/api/technicians")
@RequiredArgsConstructor
public class TechnicianController {

    private final TechnicianService technicianService;

    @GetMapping
    public List<UserResponse> listTechniciansByArea(
            @ActiveUser User activeUser, @RequestParam(name = "area", required = false) String area) {
        return technicianService.listTechniciansByArea(activeUser, area);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createTechnician(
            @ActiveUser User activeUser, @RequestBody CreateTechnicianRequest request) {
        return technicianService.createTechnician(activeUser, request);
    }
}
