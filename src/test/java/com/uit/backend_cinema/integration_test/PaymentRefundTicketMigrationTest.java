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

@Testcontainers
class PaymentRefundTicketMigrationTest {

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
    void testMigrationEnablesSeatResaleAfterCancellationAndEnforcesPartialUniqueIndex() throws Exception {
        legacySchema();
        seedBaseData();

        // 1. Insert an old cancelled booking with a ticket, and an active paid booking with a ticket
        execute("INSERT INTO bookings (booking_id, user_id, showtime_id, payment_method, total_price, status, expires_at) " +
                "VALUES (101, 1, 1, 'VNPAY', 100000, 'CANCELLED', NOW() + INTERVAL '1 hour')");
        execute("INSERT INTO tickets (ticket_id, seat_id, showtime_id, booking_id, price) " +
                "VALUES (1, 1, 1, 101, 100000)");

        execute("INSERT INTO bookings (booking_id, user_id, showtime_id, payment_method, total_price, status, expires_at) " +
                "VALUES (102, 1, 1, 'VNPAY', 100000, 'PAID', NOW() + INTERVAL '1 hour')");
        execute("INSERT INTO tickets (ticket_id, seat_id, showtime_id, booking_id, price) " +
                "VALUES (2, 2, 1, 102, 100000)");

        // Run migration
        migrate();

        // Verify ticket 1 was backfilled to CANCELLED, ticket 2 stayed VALID
        assertEquals("CANCELLED", scalar("SELECT status FROM tickets WHERE ticket_id = 1"));
        assertEquals("VALID", scalar("SELECT status FROM tickets WHERE ticket_id = 2"));

        // Seat 1 is now available for resale! Insert a new booking and VALID ticket on seat 1
        execute("INSERT INTO bookings (booking_id, user_id, showtime_id, payment_method, total_price, status, expires_at) " +
                "VALUES (103, 1, 1, 'VNPAY', 100000, 'PAID', NOW() + INTERVAL '1 hour')");
        execute("INSERT INTO tickets (ticket_id, seat_id, showtime_id, booking_id, price, status) " +
                "VALUES (3, 1, 1, 103, 100000, 'VALID')");
        assertEquals("VALID", scalar("SELECT status FROM tickets WHERE ticket_id = 3"));

        // But inserting another VALID ticket for seat 1 fails with unique violation (23505)
        execute("INSERT INTO bookings (booking_id, user_id, showtime_id, payment_method, total_price, status, expires_at) " +
                "VALUES (104, 1, 1, 'VNPAY', 100000, 'PAID', NOW() + INTERVAL '1 hour')");
        assertEquals("23505", assertThrows(SQLException.class, () ->
                execute("INSERT INTO tickets (ticket_id, seat_id, showtime_id, booking_id, price, status) " +
                        "VALUES (4, 1, 1, 104, 100000, 'VALID')")
        ).getSQLState());

        // While inserting a CANCELLED ticket for seat 1 is permitted (historical audit trail)
        assertDoesNotThrow(() ->
                execute("INSERT INTO tickets (ticket_id, seat_id, showtime_id, booking_id, price, status) " +
                        "VALUES (5, 1, 1, 104, 100000, 'CANCELLED')")
        );
    }

    @Test
    void testPaymentReceiptsAndRefundsConstraints() throws Exception {
        legacySchema();
        seedBaseData();
        execute("INSERT INTO bookings (booking_id, user_id, showtime_id, payment_method, total_price, status, expires_at) " +
                "VALUES (201, 1, 1, 'VNPAY', 150000, 'PAID', NOW() + INTERVAL '1 hour')");
        migrate();

        // Insert payment receipt
        execute("INSERT INTO payment_receipts (booking_id, gateway, transaction_reference, amount) " +
                "VALUES (201, 'VNPAY', 'VNP_TRANS_999', 150000)");

        // Duplicate transaction_reference for same gateway fails (23505)
        assertEquals("23505", assertThrows(SQLException.class, () ->
                execute("INSERT INTO payment_receipts (booking_id, gateway, transaction_reference, amount) " +
                        "VALUES (201, 'VNPAY', 'VNP_TRANS_999', 150000)")
        ).getSQLState());

        // Same reference on different gateway is allowed
        assertDoesNotThrow(() ->
                execute("INSERT INTO payment_receipts (booking_id, gateway, transaction_reference, amount) " +
                        "VALUES (201, 'CASH', 'VNP_TRANS_999', 150000)")
        );

        // Insert refund obligation
        execute("INSERT INTO refunds (payment_receipt_id, booking_id, cause, amount, request_reference) " +
                "VALUES (1, 201, 'CUSTOMER_CANCELLED', 120000, 'REFUND_REQ_001')");

        // Duplicate cause for same payment receipt fails (23505)
        assertEquals("23505", assertThrows(SQLException.class, () ->
                execute("INSERT INTO refunds (payment_receipt_id, booking_id, cause, amount, request_reference) " +
                        "VALUES (1, 201, 'CUSTOMER_CANCELLED', 120000, 'REFUND_REQ_002')")
        ).getSQLState());

        // Duplicate request_reference fails (23505)
        assertEquals("23505", assertThrows(SQLException.class, () ->
                execute("INSERT INTO refunds (payment_receipt_id, booking_id, cause, amount, request_reference) " +
                        "VALUES (1, 201, 'SHOWTIME_CANCELLED', 120000, 'REFUND_REQ_001')")
        ).getSQLState());
    }

    @Test
    void testFullReferenceSchemaLoadsAndValidatesCleanly() throws Exception {
        execute(Files.readString(Path.of("cah_cinema.sql")));
        execute("INSERT INTO users (name, auth_provider) VALUES ('Test User', 'EMAIL')");
        execute("INSERT INTO movies (title, duration, age_rating) VALUES ('Movie Test', 120, 'P')");
        execute("INSERT INTO cinemas (name, address) VALUES ('Cinema 1', '123 Street')");
        execute("INSERT INTO rooms (cinema_id, room_name) VALUES (1, 'Room 1')");
        execute("INSERT INTO seat_types (type_name, price_multiplier) VALUES ('NORMAL', 1.0)");
        execute("INSERT INTO seats (room_id, seat_type_id, seat_row, seat_col) VALUES (1, 1, 1, 1)");
        execute("INSERT INTO showtimes (movie_id, room_id, format, start_time, end_time, original_duration_micros, base_price) " +
                "VALUES (1, 1, '2D', NOW() + INTERVAL '1 hour', NOW() + INTERVAL '3 hours', 7200000000, 90000)");

        // Create booking with new defaults
        execute("INSERT INTO bookings (user_id, showtime_id, payment_method, expires_at) " +
                "VALUES (1, 1, 'VNPAY', NOW() + INTERVAL '15 minutes')");
        assertEquals("ONLINE", scalar("SELECT booking_source FROM bookings WHERE booking_id = 1"));
        assertEquals("NOT_APPLICABLE", scalar("SELECT food_fulfillment_status FROM bookings WHERE booking_id = 1"));
        assertEquals("0.00", scalar("SELECT seat_subtotal_gross FROM bookings WHERE booking_id = 1"));

        // Insert ticket
        execute("INSERT INTO tickets (seat_id, showtime_id, booking_id, price) VALUES (1, 1, 1, 90000)");
        assertEquals("VALID", scalar("SELECT status FROM tickets WHERE ticket_id = 1"));
    }

    private void seedBaseData() throws SQLException {
        execute("INSERT INTO users (user_id, name, auth_provider) VALUES (1, 'User 1', 'EMAIL')");
        execute("INSERT INTO cinemas (cinema_id, name) VALUES (1, 'Cinema 1')");
        execute("INSERT INTO rooms (room_id, cinema_id, name) VALUES (1, 1, 'Room 1')");
        execute("INSERT INTO seat_types (seat_type_id, type_name) VALUES (1, 'STANDARD')");
        execute("INSERT INTO seats (seat_id, room_id, seat_type_id, seat_number) VALUES (1, 1, 1, 'A1'), (2, 1, 1, 'A2')");
        execute("INSERT INTO movies (movie_id, title, duration) VALUES (1, 'Movie 1', 120)");
        execute("INSERT INTO showtimes (showtime_id, movie_id, room_id, start_time, end_time, base_price) " +
                "VALUES (1, 1, 1, NOW() + INTERVAL '1 hour', NOW() + INTERVAL '3 hours', 100000)");
    }

    private void legacySchema() throws Exception {
        try (var input = getClass().getResourceAsStream("/migrations/legacy-booking-tickets.sql")) {
            assertNotNull(input);
            execute(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private void migrate() throws Exception {
        execute(Files.readString(Path.of("migrations/20261017_02_payment_refund_tickets.sql")));
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
