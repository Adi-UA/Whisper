package dev.adi_ua.whisper.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * No-op security config for local dev and tests (default profile, no "oauth").
 * Permits all requests without authentication. In production on OCI, the
 * "oauth" profile activates {@link SecurityConfig} instead.
 */
@Configuration
@Profile("!oauth")
public class NoOpSecurityConfig {

    @Bean
    public SecurityFilterChain noOpFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
