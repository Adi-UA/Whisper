package dev.adi_ua.whisper.config;

import dev.adi_ua.whisper.service.AllowlistService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for Google OAuth2 login with email allowlist.
 *
 * <p>Active only in the "oauth" profile. In production on OCI, set
 * {@code SPRING_PROFILES_ACTIVE=oauth}. Locally and in CI, the default
 * profile uses {@link NoOpSecurityConfig} (permits all, no OAuth).
 */
@Configuration
@EnableWebSecurity
@Profile("oauth")
public class SecurityConfig {

    private final AllowlistService allowlistService;

    public SecurityConfig(AllowlistService allowlistService) {
        this.allowlistService = allowlistService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/api/rotate").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth -> oauth
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(allowlistCheckingUserService())
                )
                .defaultSuccessUrl("/", true)
            );
        return http.build();
    }

    private OAuth2UserService<OAuth2UserRequest, OAuth2User> allowlistCheckingUserService() {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        return request -> {
            OAuth2User user = delegate.loadUser(request);
            String email = user.getAttribute("email");
            if (email == null || !allowlistService.isAllowed(email)) {
                throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException(
                    "Email not in allowlist: " + email);
            }
            return user;
        };
    }
}
