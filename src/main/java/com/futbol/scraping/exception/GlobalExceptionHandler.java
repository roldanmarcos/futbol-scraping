package com.futbol.scraping.exception;

import jakarta.servlet.ServletException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentConversionNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage(), "timestamp", LocalDateTime.now().toString()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex) {
        log.warn("Business error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage(), "timestamp", LocalDateTime.now().toString()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", ex.getMessage(), "timestamp", LocalDateTime.now().toString()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedJson(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Malformed JSON request", "timestamp", LocalDateTime.now().toString()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Invalid request parameter or format: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Invalid request parameter or format", "timestamp", LocalDateTime.now().toString()));
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<Map<String, Object>> handleDateTimeParseException(DateTimeParseException ex) {
        log.warn("Invalid date time format: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Invalid request parameter or format", "timestamp", LocalDateTime.now().toString()));
    }

    @ExceptionHandler({
            MethodArgumentConversionNotSupportedException.class,
            ConversionFailedException.class,
            ConversionException.class,
            TypeMismatchException.class,
            BindException.class,
            MethodArgumentNotValidException.class
    })
    public ResponseEntity<Map<String, Object>> handleBindingAndConversionErrors(Exception ex) {
        log.warn("Invalid request parameter or payload: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Invalid request parameter or format", "timestamp", LocalDateTime.now().toString()));
    }

    @ExceptionHandler(ServletException.class)
    public ResponseEntity<Map<String, Object>> handleServletException(ServletException ex) {
        if (isBindingOrConversionError(ex)) {
            log.warn("Invalid request parameter or payload (wrapped): {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid request parameter or format", "timestamp", LocalDateTime.now().toString()));
        }

        log.error("Unexpected servlet error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "timestamp", LocalDateTime.now().toString()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        if (isBindingOrConversionError(ex)) {
            log.warn("Invalid request parameter or payload: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid request parameter or format", "timestamp", LocalDateTime.now().toString()));
        }

        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error", "timestamp", LocalDateTime.now().toString()));
    }

    private boolean isBindingOrConversionError(Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null) {
            if (cause instanceof MethodArgumentTypeMismatchException
                    || cause instanceof MethodArgumentConversionNotSupportedException
                    || cause instanceof ConversionFailedException
                    || cause instanceof ConversionException
                    || cause instanceof TypeMismatchException
                    || cause instanceof BindException
                    || cause instanceof MethodArgumentNotValidException
                    || cause instanceof DateTimeParseException
                    || cause instanceof IllegalArgumentException) {
                return true;
            }
            cause = cause.getCause();
        }

        String message = throwable.getMessage();
        if (message == null) {
            return false;
        }

        return message.contains("Failed to convert value of type")
                || message.contains("Failed to convert from type")
                || message.contains("MethodArgumentTypeMismatchException")
                || message.contains("Parse attempt failed");
    }
}
