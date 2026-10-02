package com.corporativoTI.SistemaInteligenteTI.users.service;

import com.corporativoTI.SistemaInteligenteTI.users.dto.UserResponse;
import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import com.corporativoTI.SistemaInteligenteTI.users.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Casos de uso sobre usuarios: hoy, listarlos para el selector de usuario activo (RF-1). */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream().map(UserService::toResponse).toList();
        //aca lo que hace por cada fila de usuarios .
        //cada usuario de esa lista lo convierte en un objeto user response
        //stream lo convierte en lista
        //map lo que hace es procesar cada objeto de esa lista y hace que cada elemento llame a la función to response
        //que convierte esta lista en una lista de usuarios dto Userresponse 
    }

    //metodo que permite convertir un objeto  User en un objeto deto user repsonse
    private static UserResponse toResponse(User user) {
        String area = user.getArea() == null ? null : user.getArea().name();
        return new UserResponse(user.getId(), user.getFullName(), user.getRole().name(), area);
    }
}
