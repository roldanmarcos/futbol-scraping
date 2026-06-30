package com.futbol.scraping.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Respuesta de creación de usuario")
public record UserCreationResponse(
        @Schema(description = "Identificador generado") Long id,
        @Schema(description = "Nombre de usuario") String username,
        @Schema(description = "Correo electrónico") String email,
        @Schema(description = "Balance actual") BigDecimal balance) {
}