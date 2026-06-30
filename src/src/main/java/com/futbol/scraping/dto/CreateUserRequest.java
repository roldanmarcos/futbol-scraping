package com.futbol.scraping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Datos para crear un usuario desde el endpoint administrativo")
public class CreateUserRequest {

    @Schema(description = "Nombre de usuario", example = "messi")
    private String username;

    @Schema(description = "Correo electrónico", example = "messi@example.com")
    private String email;

    @Schema(description = "Balance inicial opcional", example = "10000")
    private BigDecimal balance;

    public BigDecimal getBalanceOrDefault() {
        return balance != null ? balance : BigDecimal.valueOf(10000);
    }
}