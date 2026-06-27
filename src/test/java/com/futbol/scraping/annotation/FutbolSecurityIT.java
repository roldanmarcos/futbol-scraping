package com.futbol.scraping.annotation;

import com.futbol.scraping.config.SecurityConfig;
import com.futbol.scraping.config.TestSecurityEndpointsController;
import org.junit.jupiter.api.Tag;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Slice web para tests de seguridad: mantiene los filtros activos (sin addFilters = false)
// e importa SecurityConfig y el controller de prueba para verificar reglas de acceso.
// Usar junto a @WebMvcTest(controllers = TestSecurityEndpointsController.class).
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("integration")
@Import({SecurityConfig.class, TestSecurityEndpointsController.class})
public @interface FutbolSecurityIT {}
