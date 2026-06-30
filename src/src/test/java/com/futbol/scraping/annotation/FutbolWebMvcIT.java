package com.futbol.scraping.annotation;

import com.futbol.scraping.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Slice web para tests de controller: deshabilita filtros de seguridad (addFilters = false)
// e importa GlobalExceptionHandler para que los @ExceptionHandler sean procesados.
// Usar junto a @WebMvcTest(controllers = MiController.class) en cada clase,
// ya que el atributo controllers no puede generalizarse en una anotación compuesta.
//
// Para tests que requieren filtros activos (ej: seguridad), usar @FutbolSecurityIT.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("integration")
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
public @interface FutbolWebMvcIT {}
