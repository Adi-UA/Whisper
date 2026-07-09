package dev.adi_ua.whisper.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages the database connection pool and schema initialization.
 *
 * <p>Uses HikariCP for connection pooling in file-backed mode (local dev and
 * production). In-memory SQLite ({@code :memory:}) pins a single connection
 * inside the pool ({@code maximumPoolSize=1}) so all callers share the same
 * in-process database, which is required because SQLite in-memory databases
 * are scoped to a single connection.
 *
 * <p>The JDBC URL is configured via {@code whisper.db.url}. Override with
 * {@code WHISPER_DB_URL} environment variable for production (Turso uses a
 * libsql-compatible JDBC URL).
 */
@Component
public class DatabaseService {

    private final String dbUrl;
    private final int maxPoolSize;
    private final int minIdle;
    private HikariDataSource dataSource;

    /**
     * @param dbUrl      JDBC URL for SQLite (file path or {@code :memory:}).
     * @param maxPoolSize Maximum connections in the pool.
     * @param minIdle    Minimum idle connections to keep warm.
     */
    public DatabaseService(
            @Value("${whisper.db.url}") String dbUrl,
            @Value("${whisper.db.pool.max-size:10}") int maxPoolSize,
            @Value("${whisper.db.pool.min-idle:2}") int minIdle) {
        this.dbUrl = dbUrl;
        this.maxPoolSize = dbUrl.contains(":memory:") ? 1 : maxPoolSize;
        this.minIdle = dbUrl.contains(":memory:") ? 1 : minIdle;
    }

    /** Initializes the connection pool and creates tables if they don't exist. */
    @PostConstruct
    void init() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(5_000);
        config.setIdleTimeout(300_000);
        config.setMaxLifetime(600_000);
        // SQLite-specific: enable WAL mode for concurrent readers.
        if (!dbUrl.contains(":memory:")) {
            config.setConnectionInitSql("PRAGMA journal_mode=WAL; PRAGMA foreign_keys=ON;");
        } else {
            config.setConnectionInitSql("PRAGMA foreign_keys=ON;");
        }
        dataSource = new HikariDataSource(config);
        createSchema();
    }

    private void createSchema() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
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
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

    /**
     * Borrow a connection from the pool. The caller must close it (try-with-resources).
     *
     * @return a JDBC connection from the pool
     * @throws SQLException if no connection is available within the timeout
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /** Closes the connection pool. Called on application shutdown. */
    @PreDestroy
    void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
