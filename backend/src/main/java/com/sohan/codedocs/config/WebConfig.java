package com.sohan.codedocs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (allowedOrigins.length == 0) {
            return; // production: nginx proxies /api, same-origin, no CORS needed
        }
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "DELETE")
                .allowedHeaders("Content-Type", "X-XSRF-TOKEN")
                // Session cookie + CSRF cookie both need to travel cross-origin
                // for auth to work at all when this path is actually in use.
                .allowCredentials(true)
                .maxAge(3600);
    }
}