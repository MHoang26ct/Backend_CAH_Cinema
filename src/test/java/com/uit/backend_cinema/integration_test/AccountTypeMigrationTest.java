package com.uit.backend_cinema.integration_test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

/** Executes actual SQL only against an ephemeral PostgreSQL, never application/.env DB. */
@Testcontainers
class AccountTypeMigrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private Connection connection;

    @BeforeEach
    void setup() throws Exception {
        connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        String schema = "migration_" + UUID.randomUUID().toString().replace("-", "");
        execute("CREATE SCHEMA " + schema);
        execute("SET search_path TO " + schema);
    }

    @AfterEach
    void close() throws Exception {
        if (connection != null) connection.close();
    }

    @Test
    void migrationPreservesExistingUsersAndKeepsOldInsertsCompatible() throws Exception {
        legacySchema();
        execute("INSERT INTO users (name, email, password, auth_provider, total_paid, total_point, rank_level, is_deleted) "
                + "VALUES ('Existing', 'existing@example.com', 'unchanged-hash', 'EMAIL', 20200000, 505, 'GOLD', true)");
        migrate();
        try (var statement = connection.createStatement(); var row = statement.executeQuery("SELECT * FROM users")) {
            assertTrue(row.next());
            assertEquals(1, row.getLong("user_id"));
            assertEquals("REGISTERED", row.getString("account_type"));
            assertEquals("unchanged-hash", row.getString("password"));
            assertEquals("existing@example.com", row.getString("email"));
            assertEquals("20200000.00", row.getBigDecimal("total_paid").toPlainString());
            assertEquals(505, row.getInt("total_point"));
            assertEquals("GOLD", row.getString("rank_level"));
            assertTrue(row.getBoolean("is_deleted"));
            assertFalse(row.next()); // Migration did not create a shared login account.
        }
        execute("INSERT INTO users (name, auth_provider) VALUES ('New registered', 'EMAIL')");
        assertEquals("REGISTERED", scalar("SELECT account_type FROM users WHERE name = 'New registered'"));
    }

    @Test
    void databaseEnforcesTypeAndImmutableSingletonSystemIdentity() throws Exception {
        legacySchema();
        migrate();
        assertEquals("23514", assertThrows(SQLException.class,
                () -> execute("INSERT INTO users (name, auth_provider, account_type) VALUES ('Invalid', 'EMAIL', 'OTHER')"))
                .getSQLState());
        execute("INSERT INTO users (name, auth_provider, account_type) VALUES ('Walk in', 'EMAIL', 'WALK_IN')");
        assertEquals("23505", assertThrows(SQLException.class,
                () -> execute("INSERT INTO users (name, auth_provider, account_type) VALUES ('Duplicate', 'EMAIL', 'WALK_IN')"))
                .getSQLState());
        assertEquals("23514", assertThrows(SQLException.class,
                () -> execute("UPDATE users SET account_type = 'REGISTERED' WHERE account_type = 'WALK_IN'"))
                .getSQLState());
        execute("UPDATE users SET name = 'Renamed', account_type = account_type WHERE account_type = 'WALK_IN'");
        assertEquals("WALK_IN", scalar("SELECT account_type FROM users WHERE name = 'Renamed'"));
    }

    @Test
    void freshReferenceSchemaHasSameAccountTypeRules() throws Exception {
        execute(Files.readString(Path.of("cah_cinema.sql")));
        execute("INSERT INTO users (name, auth_provider) VALUES ('Registered', 'EMAIL')");
        assertEquals("REGISTERED", scalar("SELECT account_type FROM users"));
        assertEquals("23514", assertThrows(SQLException.class,
                () -> execute("UPDATE users SET account_type = 'WALK_IN'" )).getSQLState());
    }

    private void legacySchema() throws Exception {
        try (var input = getClass().getResourceAsStream("/migrations/legacy-users.sql")) {
            assertNotNull(input);
            execute(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private void migrate() throws Exception {
        execute(Files.readString(Path.of("migrations/20261017_01_account_type.sql")));
    }

    private void execute(String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String scalar(String sql) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            assertTrue(rows.next());
            return rows.getString(1);
        }
    }
}
