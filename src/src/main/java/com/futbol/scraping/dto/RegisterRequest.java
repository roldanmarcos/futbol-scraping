package com.futbol.scraping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Datos de registro de un nuevo usuario")
public class RegisterRequest {
    @Schema(description = "Nombre de usuario", example = "messi")
    private String username;
    @Schema(description = "Correo electrónico", example = "messi@example.com")
    private String email;
    @Schema(description = "Contraseña", example = "secret123")
    private String password;
    @Schema(description = "Balance inicial opcional", example = "10000")
    private BigDecimal initialBalance;
}
