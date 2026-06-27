package com.futbol.scraping.config;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

// @TestComponent excluye este controller del component scan de @SpringBootTest
// (evita conflicto de URL con OrderController al levantar el contexto completo).
// @WebMvcTest(controllers = TestSecurityEndpointsController.class) lo carga igual
// porque lo referencia explícitamente.
@TestComponent
@RestController
public class TestSecurityEndpointsController {

    @GetMapping("/auth/ping")
    public ResponseEntity<String> authPing() {
        return ResponseEntity.ok("ok");
    }

    @PostMapping("/orders/buy")
    public ResponseEntity<String> buyOrder() {
        return ResponseEntity.ok("ok");
    }

    @PostMapping("/orders/sell")
    public ResponseEntity<String> sellOrder() {
        return ResponseEntity.ok("ok");
    }

    @GetMapping("/users/{id}/portfolio")
    public ResponseEntity<String> portfolio(@PathVariable("id") Long id) {
        return ResponseEntity.ok("portfolio-" + id);
    }
}
