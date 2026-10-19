package com.uit.backend_cinema.integration_test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import com.uit.backend_cinema.modules.booking.domain.entity.Booking;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import com.uit.backend_cinema.modules.booking.domain.entity.FoodFulfillmentStatus;
import com.uit.backend_cinema.modules.booking.domain.service.BookingService;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real PostgreSQL migration tests; no application server, gateways or live database. */
@Testcontainers
class LegacyFoodSnapshotsMigrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private Connection connection;

    @BeforeEach
    void setup() throws Exception {
        connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        // Each test owns a schema in the disposable container.
        String schema = "snapshot_" + UUID.randomUUID().toString().replace("-", "");
        execute("CREATE SCHEMA " + schema);
        execute("SET search_path TO " + schema);
        execute(Files.readString(Path.of("cah_cinema.sql")));
        execute("INSERT INTO users (user_id, name, auth_provider) VALUES (1, 'Historical buyer', 'EMAIL')");
        execute("INSERT INTO movies (movie_id, title, duration, age_rating) VALUES (1, 'Movie', 120, 'P')");
        execute("INSERT INTO cinemas (cinema_id, name, address) VALUES (1, 'Cinema', 'Address')");
        execute("INSERT INTO rooms (room_id, cinema_id, room_name) VALUES (1, 1, 'Room')");
        execute("INSERT INTO seat_types (seat_type_id, type_name, price_multiplier) VALUES (1, 'NORMAL', 1)");
        execute("INSERT INTO seats (seat_id, room_id, seat_type_id, seat_row, seat_col) " +
                "SELECT n, 1, 1, 1, n FROM generate_series(1, 50) n");
        execute("INSERT INTO showtimes (showtime_id, movie_id, room_id, format, start_time, end_time, " +
                "original_duration_micros, base_price) VALUES (1, 1, 1, '2D', NOW(), NOW() + INTERVAL '2 hours', 7200000000, 999999)");
        execute("INSERT INTO foods (food_id, name, price, category) VALUES (1, 'Changed catalog food', 999999, 'Food')");
        execute("INSERT INTO vouchers (voucher_id, code, type, value, start_at, expired_at) " +
                "VALUES (1, 'CHANGED', 'FIXED_AMOUNT', 999999, NOW(), NOW() + INTERVAL '1 day')");
    }

    @AfterEach
    void close() throws Exception {
        if (connection != null) connection.close();
    }

    @Test
    void historicalFoodIsFullyRefundedWhileSeatsUse80Or50Percent() throws Exception {
        legacy(1, "100000", "50000", "150000", "0", "0", false);
        // Multiple tickets must not multiply the joined food total.
        execute("UPDATE tickets SET price=60000 WHERE booking_id=1");
        execute("INSERT INTO tickets (booking_id, showtime_id, seat_id, price) VALUES (1, 1, 50, 40000)");
        oldMigration();
        assertMoney("150000", value(1, "net_seat_amount"));
        assertMoney("0", value(1, "net_food_amount"));
        migrate();
        assertRecovered(1, "100000", "50000", "0", "0", "0");
        assertQuote(1, 8, false, "80000", "50000", "130000");
        assertQuote(1, 3, false, "50000", "50000", "100000");
        assertQuote(1, 8, true, "80000", "0", "80000");
        assertQuote(1, 3, true, "50000", "0", "50000");
    }

    @Test
    void reconstructsHistoricalRankAndVoucherWithoutCurrentCatalogOrCurrentRank() throws Exception {
        // Rank 2%, then a 30000 voucher: 147000 - 30000 = 117000.
        legacy(1, "100000", "50000", "117000", "30000", "0", true);
        oldMigration();
        execute("UPDATE users SET rank_level='DIAMOND' WHERE user_id=1");
        migrate();
        assertRecovered(1, "78000", "39000", "3000", "20000", "10000");
        assertQuote(1, 8, false, "62400", "39000", "101400");
        assertQuote(1, 3, false, "39000", "39000", "78000");
    }

    @Test
    void lateDiscountIsSeatOnlyAndCancelledTicketsRemainHistoricalEvidence() throws Exception {
        legacy(1, "100000", "50000", "98000", "49000", "49000", false);
        execute("UPDATE bookings SET status='CANCELLED' WHERE booking_id=1");
        oldMigration();
        assertEquals("CANCELLED", scalar("SELECT status FROM tickets WHERE booking_id=1"));
        migrate();
        assertRecovered(1, "49000", "49000", "3000", "0", "0");
        assertEquals("CANCELLED", value(1, "status"));
    }

    @Test
    void voucherRoundingAndFinalCentRemainderConserveActualPaidTotal() throws Exception {
        legacy(1, "100", "50", "140", "10", "0", true);
        // .97 * 150.01 = 145.5097, then voucher 10 -> stored total 135.51.
        legacy(2, "100.01", "50", "135.51", "10", "0", true);
        oldMigration();
        migrate();
        assertRecovered(1, "93.33", "46.67", "0", "6.67", "3.33");
        assertRecovered(2, "90.34", "45.17", "4.50", "6.67", "3.33");
    }

    @Test
    void missingOrContradictoryEvidenceIsQuarantinedWithoutInventingMoney() throws Exception {
        legacy(1, null, "50", "150", "0", "0", false);
        legacy(2, "100", null, "150", "0", "0", false);
        legacy(3, "100", "50", "130", "0", "0", false); // not a historical rank
        legacy(4, "100", "50", "140", "10", "0", false); // lost voucher evidence
        legacy(5, "100", "50", "150", "0", "0", false);
        execute("UPDATE food_order_items SET price=51 WHERE food_order_id=5");
        execute("INSERT INTO booking_food_draft_items (booking_id, food_id, quantity, unit_price, is_deleted) " +
                "VALUES (2, 1, 1, 50, TRUE)");
        oldMigration();
        String[] before = new String[5];
        for (int id = 1; id <= 5; id++) before[id - 1] = originalRow(id);
        migrate();
        String[] reasons = {"MISSING_TICKET_EVIDENCE", "MISSING_FOOD_EVIDENCE", "UNRECONCILED_TOTAL_OR_DISCOUNT",
                "MISSING_VOUCHER_EVIDENCE", "INCONSISTENT_FOOD_EVIDENCE"};
        for (int id = 1; id <= 5; id++) {
            assertReview(id, reasons[id - 1]);
            assertEquals(before[id - 1], originalRow(id), "Quarantine must not alter original booking values");
        }
    }

    @Test
    void ambiguousRankRoundingAndClampedZeroTotalsRequireManualReview() throws Exception {
        // Both .97 and .95 round gross .17 to paid .16, but seats round to .11 / .10.
        legacy(1, "0.11", "0.06", "0.16", "0", "0", false);
        legacy(2, "100", "50", "0", "160", "0", true);
        legacy(3, "100", "50", "100", "50", "49", true);
        oldMigration();
        migrate();
        assertReview(1, "AMBIGUOUS_ROUNDING");
        assertReview(2, "UNRECONCILED_TOTAL_OR_DISCOUNT");
        assertReview(3, "CONFLICTING_DISCOUNTS");
        assertMoney("0.16", value(1, "net_seat_amount"));
        assertMoney("0", value(1, "net_food_amount"));
    }

    @Test
    void modernSnapshotsIncludingSeatOnlyAndFreeBookingsAreNeverOverwritten() throws Exception {
        oldMigration();
        legacy(1, "100", "50", "120", "0", "0", false);
        execute("UPDATE bookings SET seat_subtotal_gross=100, food_subtotal_gross=50, " +
                "rank_discount_amount=30, net_seat_amount=80, net_food_amount=40 WHERE booking_id=1");
        legacy(2, "100", null, "100", "0", "0", false);
        execute("UPDATE bookings SET seat_subtotal_gross=100, net_seat_amount=100 WHERE booking_id=2");
        legacy(3, "100", "50", "0", "150", "0", true);
        execute("UPDATE bookings SET seat_subtotal_gross=100, food_subtotal_gross=50, " +
                "voucher_discount_seat=100, voucher_discount_food=50 WHERE booking_id=3");
        legacy(4, "0", null, "0", "0", "0", false);
        execute("UPDATE bookings SET booking_source='COUNTER', contact_name='Walk in' WHERE booking_id=4");
        String[] before = new String[4];
        for (int id = 1; id <= 4; id++) before[id - 1] = originalRow(id);
        migrate();
        migrate();
        for (int id = 1; id <= 4; id++) {
            assertEquals(before[id - 1], originalRow(id));
            assertNull(value(id, "legacy_snapshot_status"));
            assertNull(value(id, "legacy_snapshot_evidence"));
        }
    }

    @Test
    void rerunsPreserveRecoveryAndQuarantineEvenIfHistoricalEvidenceChanges() throws Exception {
        legacy(1, "100", "50", "150", "0", "0", false);
        legacy(2, null, "50", "150", "0", "0", false);
        legacy(3, "100", null, "98", "0", "0", false);
        oldMigration();
        migrate();
        assertRecovered(3, "98", "0", "2", "0", "0");
        String recovered = scalar("SELECT to_jsonb(b)::text FROM bookings b WHERE booking_id=1");
        String quarantined = scalar("SELECT to_jsonb(b)::text FROM bookings b WHERE booking_id=2");
        execute("UPDATE tickets SET price=999 WHERE booking_id=1");
        execute("INSERT INTO tickets (booking_id, showtime_id, seat_id, price) VALUES (2, 1, 2, 100)");
        migrate();
        assertEquals(recovered, scalar("SELECT to_jsonb(b)::text FROM bookings b WHERE booking_id=1"));
        assertEquals(quarantined, scalar("SELECT to_jsonb(b)::text FROM bookings b WHERE booking_id=2"));
    }

    private void legacy(int id, String seat, String food, String paid, String discount, String late, boolean voucher)
            throws SQLException {
        execute("INSERT INTO bookings (booking_id, user_id, showtime_id, payment_method, total_price, " +
                "discount_amount, late_discount_amount, voucher_id, status, expires_at) VALUES (" + id +
                ", 1, 1, 'VNPAY', " + paid + ", " + discount + ", " + late + ", " + (voucher ? "1" : "NULL") +
                ", 'PAID', NOW() + INTERVAL '1 hour')");
        if (seat != null) execute("INSERT INTO tickets (booking_id, showtime_id, seat_id, price) VALUES (" +
                id + ", 1, " + id + ", " + seat + ")");
        if (food != null) {
            execute("INSERT INTO food_orders (food_order_id, booking_id, total_price) VALUES (" + id + ", " + id + ", " + food + ")");
            execute("INSERT INTO food_order_items (food_order_id, food_id, quantity, price) VALUES (" + id + ", 1, 1, " + food + ")");
        }
    }

    private void assertRecovered(int id, String seat, String food, String rank, String voucherSeat, String voucherFood)
            throws SQLException {
        assertEquals("RECOVERED", value(id, "legacy_snapshot_status"));
        assertMoney(seat, value(id, "net_seat_amount"));
        assertMoney(food, value(id, "net_food_amount"));
        assertMoney(rank, value(id, "rank_discount_amount"));
        assertMoney(voucherSeat, value(id, "voucher_discount_seat"));
        assertMoney(voucherFood, value(id, "voucher_discount_food"));
        assertMoney(value(id, "total_price"), value(id, "net_seat_amount + net_food_amount"));
        assertNotNull(value(id, "legacy_snapshot_evidence"));
    }

    private void assertReview(int id, String reason) throws SQLException {
        assertEquals("REVIEW_REQUIRED", value(id, "legacy_snapshot_status"));
        assertEquals(reason, value(id, "legacy_snapshot_reason"));
        assertNotNull(value(id, "legacy_snapshot_evidence"));
    }

    private void assertQuote(int id, int hours, boolean fulfilled, String ticketRefund, String foodRefund, String total)
            throws SQLException {
        Booking booking = new Booking();
        booking.setBookingId((long) id);
        booking.setShowtimeId(1L);
        booking.setStatus(BookingStatus.PAID);
        booking.setTotalAmount(new BigDecimal(value(id, "total_price")));
        booking.setNetSeatAmount(new BigDecimal(value(id, "net_seat_amount")));
        booking.setNetFoodAmount(new BigDecimal(value(id, "net_food_amount")));
        booking.setSeatSubtotalGross(new BigDecimal(value(id, "seat_subtotal_gross")));
        booking.setFoodSubtotalGross(new BigDecimal(value(id, "food_subtotal_gross")));
        booking.setFoodFulfillmentStatus(fulfilled ? FoodFulfillmentStatus.FULFILLED : FoodFulfillmentStatus.PENDING);
        LocalDateTime now = LocalDateTime.of(2026, 10, 19, 9, 0);
        Showtime showtime = new Showtime();
        showtime.setStartTime(now.plusHours(hours));
        ShowtimeService showtimes = mock(ShowtimeService.class);
        when(showtimes.getById(1L)).thenReturn(showtime);
        BookingService service = new BookingService(null, null, showtimes, null, mock(TicketService.class),
                null, null, null, null, null, null, Clock.systemUTC());
        var quote = service.calculateCancellationQuote(booking, now);
        assertTrue(quote.isCanCancel());
        assertMoney(ticketRefund, quote.getTicketRefundAmount().toPlainString());
        assertMoney(foodRefund, quote.getFoodRefundAmount().toPlainString());
        assertMoney(total, quote.getTotalRefundAmount().toPlainString());
    }

    private String originalRow(int id) throws SQLException {
        return scalar("SELECT (to_jsonb(b) - ARRAY['legacy_snapshot_status', 'legacy_snapshot_reason', " +
                "'legacy_snapshot_evidence'])::text FROM bookings b WHERE booking_id=" + id);
    }

    private void oldMigration() throws Exception {
        execute(Files.readString(Path.of("migrations/20261017_02_payment_refund_tickets.sql")));
    }

    private void migrate() throws Exception {
        execute(Files.readString(Path.of("migrations/20261019_06_legacy_food_snapshots.sql")));
    }

    private String value(int id, String expression) throws SQLException {
        return scalar("SELECT " + expression + " FROM bookings WHERE booking_id=" + id);
    }

    private static void assertMoney(String expected, String actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(new BigDecimal(actual)), expected + " != " + actual);
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
