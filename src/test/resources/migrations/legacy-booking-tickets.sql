-- Minimal legacy schema before 20261017_02_payment_refund_tickets.sql
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    auth_provider VARCHAR(20) NOT NULL CHECK (auth_provider IN ('EMAIL', 'GOOGLE')),
    account_type VARCHAR(20) NOT NULL DEFAULT 'REGISTERED',
    total_paid DECIMAL(18,2) NOT NULL DEFAULT 0,
    total_point INT NOT NULL DEFAULT 0,
    rank_level VARCHAR(20) NOT NULL DEFAULT 'SILVER',
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE cinemas (
    cinema_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE rooms (
    room_id SERIAL PRIMARY KEY,
    cinema_id INT NOT NULL REFERENCES cinemas(cinema_id),
    name VARCHAR(100) NOT NULL
);

CREATE TABLE seat_types (
    seat_type_id SERIAL PRIMARY KEY,
    type_name VARCHAR(50) NOT NULL,
    price_multiplier DECIMAL(4,2) NOT NULL DEFAULT 1.00
);

CREATE TABLE seats (
    seat_id SERIAL PRIMARY KEY,
    room_id INT NOT NULL REFERENCES rooms(room_id),
    seat_type_id INT NOT NULL REFERENCES seat_types(seat_type_id),
    seat_number VARCHAR(10) NOT NULL
);

CREATE TABLE movies (
    movie_id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    duration INT NOT NULL
);

CREATE TABLE showtimes (
    showtime_id SERIAL PRIMARY KEY,
    movie_id INT NOT NULL REFERENCES movies(movie_id),
    room_id INT NOT NULL REFERENCES rooms(room_id),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    base_price DECIMAL(18,2) NOT NULL
);

CREATE TABLE vouchers (
    voucher_id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE bookings (
    booking_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(user_id),
    showtime_id INT NOT NULL REFERENCES showtimes(showtime_id),
    voucher_id INT REFERENCES vouchers(voucher_id),
    payment_method VARCHAR(20) NOT NULL CHECK (payment_method IN ('CASH', 'VNPAY', 'MOMO')),
    discount_amount DECIMAL(18,2) DEFAULT 0 CHECK (discount_amount >= 0),
    late_discount_amount DECIMAL(18,2) NOT NULL DEFAULT 0 CHECK (late_discount_amount >= 0),
    total_price DECIMAL(18,2) NOT NULL DEFAULT 0 CHECK (total_price >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PAID', 'CANCELLED', 'EXPIRED', 'REFUNDED', 'CHECKED_IN')),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE tickets (
    ticket_id SERIAL PRIMARY KEY,
    seat_id INT NOT NULL REFERENCES seats(seat_id),
    showtime_id INT NOT NULL REFERENCES showtimes(showtime_id),
    booking_id INT NOT NULL REFERENCES bookings(booking_id),
    price DECIMAL(18,2) NOT NULL CHECK (price >= 0),
    is_checked_in BOOLEAN DEFAULT FALSE,
    CONSTRAINT uq_tickets_booking_seat UNIQUE (booking_id, seat_id),
    CONSTRAINT uq_tickets_showtime_seat UNIQUE (showtime_id, seat_id)
);

CREATE TABLE food_orders (
    food_order_id SERIAL PRIMARY KEY,
    booking_id INT NOT NULL REFERENCES bookings(booking_id),
    total_price DECIMAL(18,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment_requests (
    payment_request_id SERIAL PRIMARY KEY,
    booking_id INT NOT NULL REFERENCES bookings(booking_id),
    gateway VARCHAR(30) NOT NULL,
    request_id VARCHAR(100) NOT NULL,
    order_id VARCHAR(200) NOT NULL,
    amount BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED'
);
