package com.futbol.scraping.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TestSecurityEndpointsController.class)
@Import({SecurityConfig.class, TestSecurityEndpointsController.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUpJwtAuthenticationFilterMock() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);

            try {
                String authorizationHeader = ((jakarta.servlet.http.HttpServletRequest) request)
                        .getHeader("Authorization");
                if ("Bearer test-token".equals(authorizationHeader)) {
                    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                            "mock-user",
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))));
                }
                chain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
            return null;
        }).when(jwtAuthenticationFilter).doFilter(
                ArgumentMatchers.any(ServletRequest.class),
                ArgumentMatchers.any(ServletResponse.class),
                ArgumentMatchers.any(FilterChain.class));
    }

    @Test
    void authEndpoints_ArePermittedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/auth/ping"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoints_Return401WithoutAuthentication() throws Exception {
        mockMvc.perform(post("/orders/buy"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoints_SucceedWithMockAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/orders/buy")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk());
    }

}
