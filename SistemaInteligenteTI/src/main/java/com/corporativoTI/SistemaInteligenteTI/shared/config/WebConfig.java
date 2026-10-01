package com.corporativoTI.SistemaInteligenteTI.shared.config;

import com.corporativoTI.SistemaInteligenteTI.users.web.ActiveUserArgumentResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registra los componentes transversales de Spring MVC (resolución del usuario activo). */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ActiveUserArgumentResolver activeUserArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(activeUserArgumentResolver);
    }
}
