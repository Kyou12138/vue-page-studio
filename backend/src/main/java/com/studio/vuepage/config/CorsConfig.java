package com.studio.vuepage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * 基于 {@link StudioProperties#getCorsAllowedOrigins()} 的 CORS 配置。
 */
@Configuration
public class CorsConfig {

    private final StudioProperties studioProperties;

    public CorsConfig(StudioProperties studioProperties) {
        this.studioProperties = studioProperties;
    }

    @Bean
    public WebMvcConfigurer corsWebMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String raw = studioProperties.getCorsAllowedOrigins();
                String[] origins = Arrays.stream(raw == null ? new String[0] : raw.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toArray(String[]::new);

                registry.addMapping("/api/**")
                        .allowedOrigins(origins)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("*")
                        .allowCredentials(false)
                        .maxAge(3600);
            }
        };
    }
}
