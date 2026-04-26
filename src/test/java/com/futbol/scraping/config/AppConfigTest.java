package com.futbol.scraping.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class AppConfigTest {

    private final AppConfig appConfig = new AppConfig();

    @Test
    void restTemplate_UsesSimpleFactoryWithExpectedTimeouts() throws Exception {
        RestTemplate restTemplate = appConfig.restTemplate();

        ClientHttpRequestFactory requestFactory = restTemplate.getRequestFactory();
        assertThat(requestFactory).isInstanceOf(SimpleClientHttpRequestFactory.class);

        SimpleClientHttpRequestFactory simpleFactory = (SimpleClientHttpRequestFactory) requestFactory;
        assertThat(getIntField(simpleFactory, "connectTimeout")).isEqualTo(10_000);
        assertThat(getIntField(simpleFactory, "readTimeout")).isEqualTo(30_000);
    }

    private int getIntField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (int) field.get(target);
    }

    @Test
    void passwordEncoder_IsBCrypt_AndSupportsEncodeAndMatches() {
        PasswordEncoder passwordEncoder = appConfig.passwordEncoder();

        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);

        String rawPassword = "super-secret";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        assertThat(encodedPassword).isNotBlank().isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, encodedPassword)).isTrue();
        assertThat(passwordEncoder.matches("wrong-password", encodedPassword)).isFalse();
    }
}
