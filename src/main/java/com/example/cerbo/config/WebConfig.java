package com.example.cerbo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configuration pour servir les fichiers uploadés
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/")
                .setCachePeriod(3600);
    }

    /**
     * Configuration des view controllers (pour Swagger UI par exemple)
     * Note: Cette méthode remplace l'ancienne méthode incorrecte
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Exemple pour Swagger UI
        registry.addRedirectViewController("/", "/swagger-ui.html");
        registry.addRedirectViewController("/api", "/swagger-ui.html");
    }
}