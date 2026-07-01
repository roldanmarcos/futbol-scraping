package com.futbol.scraping.repository;

import com.futbol.scraping.annotation.FutbolJpaIT;
import com.futbol.scraping.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@FutbolJpaIT
class UserRepositoryIT {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        regularUser = userRepository.save(User.builder()
                .username("pepe")
                .email("pepe@test.com")
                .passwordHash("hash-pepe")
                .balance(new BigDecimal("1000.00"))
                .isSuperuser(false)
                .build());
        superuser = userRepository.save(User.builder()
                .username("admin")
                .email("admin@test.com")
                .passwordHash("hash-admin")
                .balance(new BigDecimal("9999999.00"))
                .isSuperuser(true)
                .build());
    }

    @Test
    void findByUsername_returnsUser() {
        Optional<User> found = userRepository.findByUsername("pepe");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("pepe@test.com");
    }

    @Test
    void findByUsername_withUnknownUsername_returnsEmpty() {
        assertThat(userRepository.findByUsername("inexistente")).isEmpty();
    }

    @Test
    void findByEmail_returnsUser() {
        Optional<User> found = userRepository.findByEmail("pepe@test.com");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("pepe");
    }

    @Test
    void findByEmail_withUnknownEmail_returnsEmpty() {
        assertThat(userRepository.findByEmail("noexiste@test.com")).isEmpty();
    }

    @Test
    void findByIsSuperuserTrue_returnsSuperuser() {
        Optional<User> found = userRepository.findByIsSuperuserTrue();

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("admin");
        assertThat(found.get().getIsSuperuser()).isTrue();
    }

    @Test
    void existsByUsername_returnsTrueWhenExists() {
        assertThat(userRepository.existsByUsername("pepe")).isTrue();
        assertThat(userRepository.existsByUsername("nadie")).isFalse();
    }

    @Test
    void existsByEmail_returnsTrueWhenExists() {
        assertThat(userRepository.existsByEmail("pepe@test.com")).isTrue();
        assertThat(userRepository.existsByEmail("nadie@test.com")).isFalse();
    }
}
