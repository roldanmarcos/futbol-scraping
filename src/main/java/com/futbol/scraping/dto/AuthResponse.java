package com.futbol.scraping.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthResponse {
    Long id;
    String username;
    String email;
    String token;
}
