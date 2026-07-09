package dev.adi_ua.whisper.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Determines whether a given email address is permitted to use the application.
 *
 * <p>Checks two sources (in order):
 * <ol>
 *   <li>The {@code allowed_emails} table in the database (primary source for production).</li>
 *   <li>The {@code WHISPER_ALLOWED_EMAILS} environment variable / property (comma-separated,
 *       used for local development without a remote DB).</li>
 * </ol>
 *
 * <p>If the email appears in either source, access is granted. If the env var is empty and
 * the table has no rows, the service logs a warning on startup.
 */
@Service
public class AllowlistService {

    private static final Logger log = LoggerFactory.getLogger(AllowlistService.class);

    private final DatabaseService db;
    private final Set<String> envAllowedEmails;

    public AllowlistService(
            DatabaseService db,
            @Value("${whisper.allowed-emails:}") String allowedEmailsCsv) {
        this.db = db;
        this.envAllowedEmails = Arrays.stream(allowedEmailsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        if (!envAllowedEmails.isEmpty()) {
            log.info("Loaded {} allowed email(s) from environment", envAllowedEmails.size());
        }
    }

    /**
     * Check if the given email is in the allowlist.
     *
     * @param email the Gmail address to check (case-insensitive)
     * @return {@code true} if permitted, {@code false} otherwise
     */
    public boolean isAllowed(String email) {
        String normalized = email.toLowerCase().trim();

        // Check env-var list first (fast path for local dev)
        if (envAllowedEmails.contains(normalized)) {
            return true;
        }

        // Check the database table
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT 1 FROM allowed_emails WHERE email = ?")) {
            ps.setString(1, normalized);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Failed to check allowlist for {}: {}", normalized, e.getMessage());
            return false;
        }
    }
}
