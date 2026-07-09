package dev.adi_ua.whisper.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Handles user logout. Invalidates the HTTP session and clears the
 * security context so the user must re-authenticate via Google OAuth.
 */
@RestController
public class AuthController {

    @PostMapping("/api/logout")
    public Map<String, String> logout(HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(
                request, response, SecurityContextHolder.getContext().getAuthentication());
        return Map.of("status", "logged_out");
    }
}
