package com.uit.backend_cinema.integration_test;

import com.uit.backend_cinema.modules.payment.stripe.domain.StripePayment;
import com.uit.backend_cinema.modules.payment.stripe.infrastructure.StripePaymentStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class StripeMigrationTest {
    @Container static final PostgreSQLContainer<?> DB=new PostgreSQLContainer<>("postgres:15-alpine");
    JdbcTemplate jdbc;
    StripePaymentStore store;
    TransactionTemplate tx;
    @BeforeEach void setup() throws Exception {
        var ds=new DriverManagerDataSource(DB.getJdbcUrl(),DB.getUsername(),DB.getPassword());
        var schema="stripe_"+UUID.randomUUID().toString().replace("-","");
        new JdbcTemplate(ds).execute("CREATE SCHEMA "+schema);
        Properties p=new Properties(); p.setProperty("currentSchema",schema); ds.setConnectionProperties(p);
        jdbc=new JdbcTemplate(ds); tx=new TransactionTemplate(new DataSourceTransactionManager(ds)); store=new StripePaymentStore(jdbc);
        jdbc.execute("CREATE TABLE bookings (booking_id INT PRIMARY KEY, payment_method VARCHAR(20) CHECK(payment_method IN ('CASH','VNPAY','MOMO')))");
        jdbc.execute("ALTER TABLE bookings ADD CONSTRAINT chk_bookings_payment_method CHECK(payment_method IN ('CASH','VNPAY','MOMO'))");
        jdbc.execute("CREATE TABLE invoices (payment_method VARCHAR(20) CHECK(payment_method IN ('CASH','VNPAY','MOMO')))");
        jdbc.execute("CREATE TABLE payment_requests (payment_request_id INT PRIMARY KEY)");
        jdbc.execute(Files.readString(Path.of("migrations/20261017_03_stripe_sandbox.sql")));
        jdbc.execute(Files.readString(Path.of("migrations/20261018_04_stripe_legacy_constraints.sql")));
        jdbc.update("INSERT INTO bookings VALUES (1,'STRIPE'),(2,'VNPAY')"); jdbc.update("INSERT INTO payment_requests VALUES (10),(20)");
    }
    @Test void acceptsStripeAndPreservesOldGateways() {
        jdbc.update("INSERT INTO invoices VALUES ('STRIPE'),('CASH'),('VNPAY'),('MOMO')");
        assertThrows(DataIntegrityViolationException.class,()->jdbc.update("INSERT INTO invoices VALUES ('INVALID')"));
    }
    @Test void storePersistsExactMoneyAndProviderIDs() {
        var now=LocalDateTime.now();
        store.insert(new StripePayment(1,10,"API",98000,"vnd","acct_test","idem",null,null,null,"CREATED",now.plusMinutes(15),now,"https://example.com","https://example.com","pm_card_visa"));
        store.bind(1,null,"pi_test",null);
        assertEquals(98000,store.byObject("pi_test").orElseThrow().amount());
        store.state(1,"SUCCEEDED"); store.state(1,"EXPIRED"); assertEquals("SUCCEEDED",store.find(1).orElseThrow().state());
    }
    @Test void failedBusinessTransactionDoesNotConsumeWebhook() {
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->{assertTrue(store.receive("evt_fail","refund.updated"));throw new IllegalStateException("rollback");}));
        assertEquals(Boolean.TRUE, tx.execute(s -> store.receive("evt_fail","refund.updated")));
    }
    @Test void concurrentDeliveryCommitsOnlyOneInboxRecord() throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2);
        CountDownLatch ready=new CountDownLatch(2); CountDownLatch start=new CountDownLatch(1);
        Callable<Boolean> work=()->{ready.countDown();assertTrue(start.await(10,TimeUnit.SECONDS));return tx.execute(s->{boolean first=store.receive("evt_duplicate","payment_intent.succeeded");if(first)store.processed("evt_duplicate");return first;});};
        try {
            Future<Boolean> a=pool.submit(work),b=pool.submit(work); assertTrue(ready.await(10,TimeUnit.SECONDS)); start.countDown();
            assertNotEquals(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM stripe_webhook_inbox WHERE processed_at IS NOT NULL",Integer.class));
        } finally {pool.shutdownNow();}
    }
}
