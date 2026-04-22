package com.futbol.scraping.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private BigDecimal initialBalance;
}
