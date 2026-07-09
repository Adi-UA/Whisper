package dev.adi_ua.whisper.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages the SQLite connection and schema initialization.
 *
 * In-memory SQLite (:memory:) is ephemeral per-connection, so we keep a single
 * shared connection alive for the lifetime of the process. File-backed SQLite
 * (the default for local dev and prod) opens a new connection per call, which
 * is fine for multi-threaded use with WAL mode.
 */
@Component
public class DatabaseService {

    private final String dbUrl;
    private final boolean inMemory;
    private Connection sharedConnection;

    public DatabaseService(@Value("${whisper.db.url}") String dbUrl) {
        this.dbUrl = dbUrl;
        this.inMemory = dbUrl.contains(":memory:");
    }

    @PostConstruct
    void initSchema() {
        try {
            Connection conn = getConnection();
            Statement stmt = conn.createStatement();
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS groups (
                    id         TEXT PRIMARY KEY,
                    name       TEXT NOT NULL,
                    pin_hash   TEXT,
                    schedule   TEXT NOT NULL DEFAULT 'daily',
                    timezone   TEXT NOT NULL DEFAULT 'America/Chicago',
                    created_at TEXT NOT NULL
                )
                """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS members (
                    id        TEXT PRIMARY KEY,
                    group_id  TEXT NOT NULL REFERENCES groups(id),
                    name      TEXT NOT NULL,
                    channel   TEXT NOT NULL,
                    joined_at TEXT NOT NULL
                )
                """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS history (
                    group_id   TEXT NOT NULL REFERENCES groups(id),
                    phrase     TEXT NOT NULL,
                    rotated_at TEXT NOT NULL,
                    PRIMARY KEY (group_id, rotated_at)
                )
                """);
            stmt.close();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

    public Connection getConnection() throws SQLException {
        if (inMemory) {
            if (sharedConnection == null || sharedConnection.isClosed()) {
                sharedConnection = DriverManager.getConnection(dbUrl);
            }
            return sharedConnection;
        }
        return DriverManager.getConnection(dbUrl);
    }

    @PreDestroy
    void close() throws SQLException {
        if (sharedConnection != null && !sharedConnection.isClosed()) {
            sharedConnection.close();
        }
    }
}
