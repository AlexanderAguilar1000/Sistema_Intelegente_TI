package com.corporativoTI.SistemaInteligenteTI.users.web;

import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import com.corporativoTI.SistemaInteligenteTI.users.repository.UserRepository;
import com.corporativoTI.SistemaInteligenteTI.users.service.MissingActiveUserException;
import com.corporativoTI.SistemaInteligenteTI.users.service.UnknownActiveUserException;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Resuelve el parámetro {@link ActiveUser} leyendo la cabecera {@code X-User-Id} (RF-1). */
@Component
public class ActiveUserArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String ACTIVE_USER_HEADER = "X-User-Id";

    private final UserRepository userRepository;

    // @Lazy: los resolvers de argumentos se auto-registran en cualquier slice @WebMvcTest,
    // incluso en las que no cargan JPA; sin esto, un slice ajeno a los usuarios rompería
    // al no poder inyectar UserRepository aunque nunca use este resolver.
    public ActiveUserArgumentResolver(@Lazy UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override//si ve que le envio el parametro ActiveUser y el tipo de parametro es User , entonces entra a este metodo.
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(ActiveUser.class)
                && parameter.getParameterType().equals(User.class);
    }

    @Override
    public Object resolveArgument(//se asegura que se este madando la cabercera para identificar al usuario  , sino 
                                   //sale error . 
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        String headerValue = webRequest.getHeader(ACTIVE_USER_HEADER);
        if (headerValue == null || headerValue.isBlank()) {
            throw new MissingActiveUserException();
        }

        Long userId;
        try {
            userId = Long.parseLong(headerValue.trim());
        } catch (NumberFormatException ex) {
            throw new UnknownActiveUserException(headerValue);
        }

        return userRepository
                .findById(userId)
                .orElseThrow(() -> new UnknownActiveUserException(headerValue));//mandaste un id que no existe 
    }
}
