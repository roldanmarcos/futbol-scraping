package com.futbol.scraping.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI futbolScrapingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Futbol Scraping API")
                        .description("API para sincronizacion de jugadores, cotizaciones y ordenes.")
                        .version("v1")
                        .contact(new Contact()
                                .name("Futbol Scraping Team")
                                .email("soporte@futbol.local"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Ingresa tu token JWT. Formato: Bearer <token>")));
    }
}
