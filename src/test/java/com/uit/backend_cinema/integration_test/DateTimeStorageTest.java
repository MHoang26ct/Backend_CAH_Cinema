package com.uit.backend_cinema.integration_test;

import java.nio.file.*;
import java.sql.*;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import static org.junit.jupiter.api.Assertions.*;
import com.uit.backend_cinema.modules.seat.infrastructure.persistence.RedisSeatLockRepositoryImpl;
import com.uit.backend_cinema.modules.booking.infrastructure.repository.JpaBookingRepository;
import org.springframework.data.jpa.repository.Query;

@Testcontainers(disabledWithoutDocker = true)
class DateTimeStorageTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @Container static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Test void migrationPreservesSchedulesAndExpiryIncludesBoundary() throws Exception {
        try (Connection c = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement st = c.createStatement()) {
            st.execute("CREATE SCHEMA legacy; SET search_path TO legacy");
            st.execute("CREATE TABLE showtimes (start_time timestamp, end_time timestamp)");
            st.execute("INSERT INTO showtimes VALUES ('2026-09-21 23:00', '2026-09-22 01:00')");
            st.execute("""
                    CREATE TABLE bookings (booking_id bigint, showtime_id bigint, status varchar(20),
                    expires_at timestamp, updated_at timestamp, version bigint default 0, is_deleted boolean default false)
                    """);
            st.execute("INSERT INTO bookings (booking_id, showtime_id, status, expires_at) VALUES (1, 3, 'PENDING', '2026-09-21 10:00')");
            st.execute(Files.readString(Path.of("migrations/20260921_datetime_rules.sql")));
            try (ResultSet rs = st.executeQuery("SELECT original_duration_micros, end_time FROM showtimes")) {
                assertTrue(rs.next()); assertEquals(7_200_000_000L, rs.getLong(1));
                assertEquals("2026-09-22 01:00:00.0", rs.getTimestamp(2).toString());
            }
            String blocking = JpaBookingRepository.class.getMethod("hasScheduleBlockingBookings", Long.class, java.time.LocalDateTime.class)
                    .getAnnotation(Query.class).value().replace(":showtimeId", "3").replace(":now", "TIMESTAMP '2026-09-21 10:00'");
            try (ResultSet rs = st.executeQuery(blocking)) { assertTrue(rs.next()); assertFalse(rs.getBoolean(1)); }
            st.execute("UPDATE bookings SET status = 'PAID'");
            try (ResultSet rs = st.executeQuery(blocking)) { assertTrue(rs.next()); assertTrue(rs.getBoolean(1)); }
            st.execute("UPDATE bookings SET status = 'PENDING'");
            String expiry = JpaBookingRepository.class.getMethod("markExpiredIfPendingAndExpired", Long.class, java.time.LocalDateTime.class)
                    .getAnnotation(Query.class).value().replace(":bookingId", "1").replace(":now", "TIMESTAMP '2026-09-21 10:00'");
            assertEquals(1, st.executeUpdate(expiry));
            assertEquals(0, st.executeUpdate(expiry));
            try (ResultSet rs = st.executeQuery("SELECT late_discount_amount FROM bookings")) {
                assertTrue(rs.next()); assertEquals(0, rs.getBigDecimal(1).signum());
            }
        }
    }

    @Test void freshSchemaAndSeedRemainCompatible() throws Exception {
        try (Connection c = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement st = c.createStatement()) {
            st.execute("CREATE SCHEMA fresh; SET search_path TO fresh");
            st.execute(Files.readString(Path.of("cah_cinema.sql")));
            st.execute(Files.readString(Path.of("seed_background_data.sql")));
            try (ResultSet rs = st.executeQuery("SELECT count(*) FROM showtimes WHERE original_duration_micros <= 0")) {
                assertTrue(rs.next()); assertEquals(0, rs.getLong(1));
            }
        }
    }

    @Test void bookingCommitBlocksConcurrentRescheduling() throws Exception {
        String url = POSTGRES.getJdbcUrl();
        try (Connection c = DriverManager.getConnection(url, POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement st = c.createStatement()) {
            st.execute("CREATE SCHEMA concurrent_schedule; SET search_path TO concurrent_schedule");
            st.execute(Files.readString(Path.of("cah_cinema.sql")));
            st.execute(Files.readString(Path.of("seed_background_data.sql")));
            st.execute("INSERT INTO users (user_id, name, auth_provider) VALUES (1, 'Test', 'EMAIL')");
        }
        var dataSource = new org.springframework.jdbc.datasource.DriverManagerDataSource(
                url + (url.contains("?") ? "&" : "?") + "currentSchema=concurrent_schedule",
                POSTGRES.getUsername(), POSTGRES.getPassword());
        var factory = new org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.uit.backend_cinema.modules");
        factory.setJpaVendorAdapter(new org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter());
        factory.afterPropertiesSet();
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var emf = factory.getObject();
            var em = org.springframework.orm.jpa.SharedEntityManagerCreator.createSharedEntityManager(emf);
            var repositories = new org.springframework.data.jpa.repository.support.JpaRepositoryFactory(em);
            var showJpa = repositories.getRepository(com.uit.backend_cinema.modules.showtime.infrastructure.repository.JpaShowtimeRepository.class);
            var bookingJpa = repositories.getRepository(JpaBookingRepository.class);
            var showMapper = org.mapstruct.factory.Mappers.getMapper(com.uit.backend_cinema.modules.showtime.infrastructure.mapper.ShowtimeInfraMapper.class);
            var bookingMapper = org.mapstruct.factory.Mappers.getMapper(com.uit.backend_cinema.modules.booking.infrastructure.mapper.BookingInfraMapper.class);
            var showRepo = new com.uit.backend_cinema.modules.showtime.infrastructure.persistence.ShowtimeRepositoryImpl(showJpa, null, showMapper);
            var bookingRepo = new com.uit.backend_cinema.modules.booking.infrastructure.persistence.BookingRepositoryImpl(bookingJpa, bookingMapper);
            var clock = java.time.Clock.fixed(java.time.Instant.parse("2026-05-08T00:00:00Z"), java.time.ZoneOffset.UTC);
            var service = new com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService(showRepo, null, bookingRepo, clock);
            var tx = new org.springframework.transaction.support.TransactionTemplate(new org.springframework.orm.jpa.JpaTransactionManager(emf));
            var attempt = new java.util.concurrent.CountDownLatch(1);
            var pending = new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.Future<com.uit.backend_cinema.common.exception.ErrorCode>>();
            tx.executeWithoutResult(status -> {
                var show = showRepo.findByIdForUpdate(1L).orElseThrow();
                show.setStartTime(show.getStartTime().plusHours(1));
                pending.set(executor.submit(() -> {
                    attempt.countDown();
                    try {
                        tx.executeWithoutResult(other -> service.updateShowtime(show));
                        return null;
                    } catch (com.uit.backend_cinema.common.exception.BusinessException e) {
                        return e.getCode();
                    }
                }));
                try {
                    assertTrue(attempt.await(5, java.util.concurrent.TimeUnit.SECONDS));
                    assertThrows(java.util.concurrent.TimeoutException.class,
                            () -> pending.get().get(200, java.util.concurrent.TimeUnit.MILLISECONDS));
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
                em.createNativeQuery("""
                        INSERT INTO bookings (user_id, showtime_id, payment_method, expires_at)
                        VALUES (1, 1, 'CASH', TIMESTAMP '2026-05-08 00:15:00')
                        """).executeUpdate();
            });
            assertEquals(com.uit.backend_cinema.common.exception.ErrorCode.SHOWTIME_SCHEDULE_LOCKED,
                    pending.get().get(10, java.util.concurrent.TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
            factory.destroy();
        }
    }

    @Test void redisPromotionPreservesOwnerAndCheckoutTtl() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        factory.afterPropertiesSet();
        try {
            StringRedisTemplate template = new StringRedisTemplate(factory);
            RedisSeatLockRepositoryImpl repo = new RedisSeatLockRepositoryImpl(template);
            assertTrue(repo.lockSeat(3L, 5L, 7L, 300));
            assertFalse(repo.promoteLockIfOwner(3L, 5L, 8L, 900));
            assertTrue(repo.promoteLockIfOwner(3L, 5L, 7L, 900));
            Long ttl = template.getExpire("lock:showtime:3:seat:5");
            assertNotNull(ttl); assertTrue(ttl > 890 && ttl <= 900);
            repo.unlock(3L, 5L);
            assertFalse(repo.isLocked(3L, 5L));
        } finally { factory.destroy(); }
    }
}
