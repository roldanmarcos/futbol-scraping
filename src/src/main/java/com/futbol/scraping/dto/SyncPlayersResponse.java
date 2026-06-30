package com.futbol.scraping.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta del proceso de sincronización de jugadores")
public record SyncPlayersResponse(
        @Schema(description = "Cantidad de jugadores sincronizados") int playersSync,
        @Schema(description = "Estado de la operación", example = "SUCCESS") String status) {
}