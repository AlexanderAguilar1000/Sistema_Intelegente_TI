package com.corporativoTI.SistemaInteligenteTI.users.controller;

import com.corporativoTI.SistemaInteligenteTI.users.dto.UserResponse;
import com.corporativoTI.SistemaInteligenteTI.users.service.UserService;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Publica la lista de usuarios para elegir el usuario activo (RF-1). */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
    public UserController(UserService userService) {
        this.userService = userService;
    }**/

    @GetMapping
    public List<UserResponse> getUsers() {
        return userService.listUsers();
    }
}
