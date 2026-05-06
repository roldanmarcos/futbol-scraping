package com.futbol.scraping.exception;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;

import java.time.format.DateTimeParseException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleNotFound_Returns404() {
        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(
                new ResourceNotFoundException("Player not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "Player not found");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    void handleBusiness_Returns400() {
        ResponseEntity<Map<String, Object>> response = handler.handleBusiness(
                new BusinessException("Quantity must be positive"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Quantity must be positive");
    }

    @Test
    void handleAccessDenied_Returns403() {
        ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(
                new AccessDeniedException("forbidden"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("error", "forbidden");
    }

    @Test
    void handleMalformedJson_Returns400() {
        ResponseEntity<Map<String, Object>> response = handler.handleMalformedJson(
            new HttpMessageNotReadableException("malformed", null, null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Malformed JSON request");
    }

    @Test
    void handleDateTimeParseException_Returns400() {
        ResponseEntity<Map<String, Object>> response = handler.handleDateTimeParseException(
                new DateTimeParseException("bad date", "x", 0));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Invalid request parameter or format");
    }

    @Test
    void handleBindingAndConversionErrors_Returns400() {
        BindException bindException = new BindException(new Object(), "request");

        ResponseEntity<Map<String, Object>> response = handler.handleBindingAndConversionErrors(bindException);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Invalid request parameter or format");
    }

    @Test
    void handleServletException_Returns400_WhenWrappedBindingError() {
        ServletException servletException = new ServletException("wrapped", new IllegalArgumentException("bad"));

        ResponseEntity<Map<String, Object>> response = handler.handleServletException(servletException);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Invalid request parameter or format");
    }

    @Test
    void handleServletException_Returns500_WhenUnexpected() {
        ResponseEntity<Map<String, Object>> response = handler.handleServletException(
                new ServletException("unexpected"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("error", "Internal server error");
    }

    @Test
    void handleGeneral_Returns400_WhenConversionMessageIsPresent() {
        Exception ex = new RuntimeException("Failed to convert value of type java.lang.String");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Invalid request parameter or format");
    }

    @Test
    void handleGeneral_Returns500_WhenUnexpected() {
        Exception ex = new RuntimeException("unexpected");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("error", "Internal server error");
    }
}
