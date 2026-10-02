package com.corporativoTI.SistemaInteligenteTI.users.service;

import com.corporativoTI.SistemaInteligenteTI.catalog.model.Area;
import com.corporativoTI.SistemaInteligenteTI.users.dto.CreateTechnicianRequest;
import com.corporativoTI.SistemaInteligenteTI.users.exception.InvalidAreaException;
import com.corporativoTI.SistemaInteligenteTI.users.exception.MissingFieldException;
import com.corporativoTI.SistemaInteligenteTI.users.exception.UsernameAlreadyExistsException;
import com.corporativoTI.SistemaInteligenteTI.users.dto.UserResponse;
import com.corporativoTI.SistemaInteligenteTI.users.model.Role;
import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import com.corporativoTI.SistemaInteligenteTI.users.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Casos de uso sobre técnicos: darlos de alta con su área (RF-2). */
@Service
public class TechnicianService {

    private final UserRepository userRepository;

    public TechnicianService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse createTechnician(CreateTechnicianRequest request) {
        String fullName = requireText(request.fullName(), "fullName");
        String username = requireText(request.username(), "username");
        String areaText = requireText(request.area(), "area");

        //si existe el área 
        Area area = parseArea(areaText);

        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException(username);
        }

        User technician = new User();
        technician.setFullName(fullName);
        technician.setUsername(username);
        technician.setRole(Role.TECHNICIAN);
        technician.setArea(area);
        User saved = userRepository.save(technician);

        return new UserResponse(
                saved.getId(), saved.getFullName(), saved.getRole().name(), saved.getArea().name());
    }


    //en el propio service esta la logica de negocio y las validaciones
    //cuando detexte un error llama a la clase que lo quehace es invocar el error , ese error
    //es pasado al endpoind y eso le da al fronted para que lo muestre
    /** Técnicos de un área, para ofrecer solo ellos al asignar un incidente (RF-7). */
    public List<UserResponse> listTechniciansByArea(String areaText) {
        Area area = parseArea(requireText(areaText, "area"));
        return userRepository.findByRoleAndAreaOrderByFullName(Role.TECHNICIAN, area).stream()
                .map(user -> new UserResponse(
                        user.getId(), user.getFullName(), user.getRole().name(), user.getArea().name()))
                .toList();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new MissingFieldException(field);
        }
        return value.trim();//devuelve el texto eliminando los espacios
    }

    //devuelve el area 
    private static Area parseArea(String value) {
        try {
            return Area.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new InvalidAreaException(value);
        }
    }
}
