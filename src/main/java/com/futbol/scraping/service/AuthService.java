package com.futbol.scraping.service;

import com.futbol.scraping.dto.AuthResponse;
import com.futbol.scraping.dto.RegisterRequest;
import com.futbol.scraping.exception.BusinessException;
import com.futbol.scraping.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        validateRequest(request);

        BigDecimal initialBalance = request.getInitialBalance() != null
                ? request.getInitialBalance()
                : BigDecimal.valueOf(10_000);

        User user = userService.createUser(
                request.getUsername().trim(),
                request.getEmail().trim(),
                request.getPassword(),
                initialBalance);

        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .token(token)
                .build();
    }

    private void validateRequest(RegisterRequest request) {
        if (request == null) {
            throw new BusinessException("Request body is required");
        }
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BusinessException("Username is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BusinessException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException("Password is required");
        }
        if (request.getPassword().length() < 6) {
            throw new BusinessException("Password must be at least 6 characters");
        }
    }
}
