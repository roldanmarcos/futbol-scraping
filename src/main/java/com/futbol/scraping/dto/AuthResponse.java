package com.futbol.scraping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Schema(description = "Respuesta de autenticación y registro")
public class AuthResponse {
    @Schema(description = "Identificador del usuario", example = "42")
    Long id;
    @Schema(description = "Nombre de usuario", example = "messi")
    String username;
    @Schema(description = "Correo electrónico", example = "messi@example.com")
    String email;
    @Schema(description = "JWT emitido por el servidor", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJtZXNzaSJ9.signature")
    String token;
}
