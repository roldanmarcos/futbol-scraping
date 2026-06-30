package com.futbol.scraping.web;

import com.futbol.scraping.dto.PortfolioDTO;
import com.futbol.scraping.dto.TransactionDTO;
import com.futbol.scraping.service.AuthorizationService;
import com.futbol.scraping.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "Consulta de portafolio y transacciones por usuario")
public class UserController {

    private final UserService userService;
    private final AuthorizationService authorizationService;

    @GetMapping("/{id}/portfolio")
    @Operation(summary = "Obtener portafolio", description = "Devuelve el resumen de posiciones, inversión y rendimiento del usuario.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PortfolioDTO> getPortfolio(
            @Parameter(description = "Identificador del usuario") @PathVariable Long id) {
        log.debug("GET /users/{}/portfolio", id);
        authorizationService.assertUserMatchesOrSuperuser(id);
        return ResponseEntity.ok(userService.getPortfolio(id));
    }

    @GetMapping("/{id}/transactions")
    @Operation(summary = "Obtener transacciones", description = "Devuelve el historial de operaciones realizadas por el usuario.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<TransactionDTO>> getTransactions(
            @Parameter(description = "Identificador del usuario") @PathVariable Long id) {
        log.debug("GET /users/{}/transactions", id);
        authorizationService.assertUserMatchesOrSuperuser(id);
        return ResponseEntity.ok(userService.getTransactions(id));
    }
}


