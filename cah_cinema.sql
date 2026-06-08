-- ==========================================
-- 1. NHÓM BẢNG DANH MỤC (KHÔNG KHÓA NGOẠI)
-- ==========================================

CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    provider_id VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    password VARCHAR(255),
    phone VARCHAR(20),
    avatar_url TEXT,
    auth_provider VARCHAR(20) NOT NULL CHECK (auth_provider IN ('EMAIL', 'GOOGLE')),
    total_paid  DECIMAL(18,2) NOT NULL DEFAULT 0 CHECK (total_paid  >= 0),
    total_point INT           NOT NULL DEFAULT 0 CHECK (total_point >= 0),
    rank_level  VARCHAR(20)   NOT NULL DEFAULT 'SILVER' CHECK (rank_level IN ('SILVER', 'GOLD', 'DIAMOND')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE movies (
    movie_id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    duration INT NOT NULL CHECK (duration >= 15),
    release_date DATE,
    director_name VARCHAR(50),
    actor_list TEXT	,
    age_rating VARCHAR(10) NOT NULL CHECK (age_rating IN ('P', 'K', 'T13', 'T16', 'T18')),
    poster_url TEXT,
    trailer_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE genres (
    genre_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE cinemas (
    cinema_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500) NOT NULL,
    image_url TEXT,
    hotline VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE seat_types (
    seat_type_id SERIAL PRIMARY KEY,
    type_name VARCHAR(50) NOT NULL CHECK(type_name IN ('NORMAL', 'VIP', 'COUPLE', 'AISLE')),
    price_multiplier DECIMAL(4,2) NOT NULL DEFAULT 1.0 CHECK (price_multiplier >= 0),
    CONSTRAINT CHK_aisle_price CHECK (type_name != 'AISLE' OR price_multiplier = 0)
);

CREATE TABLE foods (
    food_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(18,2) NOT NULL CHECK (price >= 0),
    category VARCHAR(20) NOT NULL CHECK (category IN ('Combo', 'Drink', 'Food')),
    image_url TEXT,
    is_available BOOLEAN DEFAULT TRUE,
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE price_configs (
    config_id SERIAL PRIMARY KEY,
    day_type VARCHAR(20) CHECK (day_type IN ('WEEKDAY', 'WEEKEND', 'HOLIDAY')),
    time_slot VARCHAR(20) CHECK (time_slot IN ('MORNING', 'AFTERNOON', 'EVENING')),
    movie_format VARCHAR(20) CHECK (movie_format IN ('2D', '3D', 'IMAX')),
    multiplier DECIMAL(4, 2) NOT NULL DEFAULT 1.0 CHECK (multiplier >= 0)
);

CREATE TABLE holidays (
    holiday_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    holiday_date DATE NOT NULL,
    is_recurring BOOLEAN DEFAULT FALSE
);

CREATE TABLE roles (
    role_id SERIAL PRIMARY KEY,
    role_name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE vouchers (
    voucher_id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('FIXED_AMOUNT', 'PERCENT')),
    value DECIMAL(18,2) NOT NULL CHECK (value >= 0),
    max_discount DECIMAL(18,2) CHECK (max_discount >= 0),
    min_order_value DECIMAL(18,2) DEFAULT 0 CHECK (min_order_value >= 0),
    quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    used_count INT NOT NULL DEFAULT 0 CHECK (used_count >= 0),
    start_at TIMESTAMP NOT NULL,
    expired_at TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT CHK_voucher_duration CHECK (start_at < expired_at),
    CONSTRAINT CHK_voucher_logic CHECK (
        (type = 'PERCENT' AND max_discount IS NOT NULL) OR 
        (type = 'FIXED_AMOUNT' AND (max_discount IS NULL OR max_discount = 0))
    )
);

CREATE TABLE promotion_articles (
    promotion_id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    short_description TEXT NOT NULL,
    start_date DATE,
    end_date DATE,
    conditions TEXT,
    image_url TEXT,
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    is_deleted BOOLEAN DEFAULT FALSE
);

-- ==========================================
-- 2. NHÓM BẢNG CÓ KHÓA NGOẠI CẤP 1
-- ==========================================

CREATE TABLE rooms (
    room_id SERIAL PRIMARY KEY,
    cinema_id INT NOT NULL REFERENCES cinemas(cinema_id),
    room_name VARCHAR(100) NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE
);


CREATE TABLE movie_genres (
    movie_id INT REFERENCES movies(movie_id),
    genre_id INT REFERENCES genres(genre_id),
    PRIMARY KEY (movie_id, genre_id)
);

CREATE TABLE user_roles (
    user_id INT REFERENCES users(user_id),
    role_id INT REFERENCES roles(role_id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_tokens (
    id SERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    user_id INT NOT NULL,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ==========================================
-- 3. NHÓM BẢNG CÓ KHÓA NGOẠI CẤP 2 (GHẾ, SUẤT CHIẾU)
-- ==========================================

CREATE TABLE seats (
    seat_id SERIAL PRIMARY KEY,
    room_id INT NOT NULL REFERENCES rooms(room_id),
    seat_row NUMERIC(5,1) NOT NULL,
    seat_col NUMERIC(5,1) NOT NULL,
    seat_type_id INT NOT NULL REFERENCES seat_types(seat_type_id),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PREPARE')),
    is_deleted BOOLEAN DEFAULT false,
    CONSTRAINT chk_row_col_min_val CHECK (seat_row >= 1 AND seat_col >= 1)
);

CREATE TABLE showtimes (
    showtime_id SERIAL PRIMARY KEY,
    room_id INT NOT NULL REFERENCES rooms(room_id),
    movie_id INT NOT NULL REFERENCES movies(movie_id),
    format VARCHAR(20) NOT NULL CHECK (format IN ('2D', '3D', 'IMAX')),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    base_price DECIMAL(18,2) NOT NULL CHECK (base_price >= 0),
    is_deleted BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'SOLD_OUT', 'HIDDEN', 'CANCELLED')),
    CONSTRAINT CHK_showtime_duration CHECK (start_time < end_time)
);

-- Tạo bảng pending_room_cleanups (dùng cho Room Cloning + Cleanup Scheduler)
CREATE TABLE IF NOT EXISTS pending_room_cleanups (
    cleanup_id   SERIAL PRIMARY KEY,
    old_room_id  INT       NOT NULL REFERENCES rooms(room_id),
    new_room_id  INT       NOT NULL REFERENCES rooms(room_id),
    replaced_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cleaned_up   BOOLEAN   NOT NULL DEFAULT FALSE,
    cleaned_up_at TIMESTAMP
);

-- Index để scheduler query nhanh
CREATE INDEX IF NOT EXISTS idx_pending_room_cleanups_not_done
    ON pending_room_cleanups(cleaned_up)
    WHERE cleaned_up = FALSE;

-- ==========================================
-- 4. NHÓM BẢNG ĐẶT VÉ VÀ THANH TOÁN
-- ==========================================

CREATE TABLE bookings (
    booking_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(user_id),
    showtime_id INT NOT NULL REFERENCES showtimes(showtime_id),
    voucher_id INT REFERENCES vouchers(voucher_id),
    payment_method VARCHAR(20) NOT NULL CHECK (payment_method IN ('CASH', 'VNPAY', 'MOMO')),
    discount_amount DECIMAL(18,2) DEFAULT 0 CHECK (discount_amount >= 0),
    total_price DECIMAL(18,2) NOT NULL DEFAULT 0 CHECK (total_price >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PAID', 'CANCELLED', 'EXPIRED', 'REFUNDED', 'CHECKED_IN')),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_bookings_status_expires_at
ON bookings (status, expires_at);

CREATE TABLE pending_ticket_items (
    pending_ticket_item_id SERIAL PRIMARY KEY,
    booking_id INT NOT NULL REFERENCES bookings(booking_id),
    seat_id INT NOT NULL REFERENCES seats(seat_id),
    unit_price DECIMAL(18,2) NOT NULL CHECK (unit_price >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT uq_pending_ticket_booking_seat UNIQUE (booking_id, seat_id)
);

CREATE TABLE booking_food_draft_items (
    booking_food_draft_item_id SERIAL PRIMARY KEY,
    booking_id INT NOT NULL REFERENCES bookings(booking_id),
    food_id INT NOT NULL REFERENCES foods(food_id),
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(18,2) NOT NULL CHECK (unit_price >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT uq_booking_food_draft UNIQUE (booking_id, food_id)
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
    total_price DECIMAL(18,2) NOT NULL DEFAULT 0 CHECK (total_price >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_food_orders_booking UNIQUE (booking_id)
);

CREATE TABLE payment_confirmations (
    payment_confirmation_id SERIAL PRIMARY KEY,
    payment_ref VARCHAR(100) NOT NULL,
    booking_id INT NOT NULL REFERENCES bookings(booking_id),
    paid_at TIMESTAMP,
    status VARCHAR(30) NOT NULL,
    gateway VARCHAR(30) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_payment_confirmations_payment_ref UNIQUE (payment_ref)
);

CREATE UNIQUE INDEX uq_payment_confirmations_success_booking
ON payment_confirmations (booking_id)
WHERE status = 'SUCCESS';

CREATE TABLE food_order_items (
    food_order_id INT REFERENCES food_orders(food_order_id),
    food_id INT REFERENCES foods(food_id),	
    quantity INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    price DECIMAL(18,2) NOT NULL CHECK (price >= 0),
    PRIMARY KEY (food_order_id, food_id)
);

CREATE TABLE invoices (
    invoice_id SERIAL PRIMARY KEY,
    booking_id INT NOT NULL REFERENCES bookings(booking_id),
    payment_method VARCHAR(20) NOT NULL CHECK (payment_method IN ('CASH', 'VNPAY', 'MOMO')),
    amount_paid DECIMAL(18,2) NOT NULL CHECK (amount_paid >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE outbox_events (
    outbox_event_id SERIAL PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_outbox_event_type_aggregate UNIQUE (event_type, aggregate_id)
);

CREATE INDEX idx_outbox_events_status_next_retry_at
ON outbox_events (status, next_retry_at);

CREATE TABLE movie_comments (
    comment_id SERIAL PRIMARY KEY,
    movie_id INT NOT NULL REFERENCES movies(movie_id),
    user_id INT NOT NULL REFERENCES users(user_id),
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_movie_comments_movie_id ON movie_comments(movie_id);

-- Bảng chung lưu thông tin yêu cầu thanh toán qua các cổng (MoMo, VNPay, ZaloPay, ...)
-- Được tạo khi gọi API tạo đơn thanh toán tới gateway
-- Dùng để:
--   1. Tránh gọi gateway trùng lặp (trả lại payUrl cũ)
--   2. Lưu trạng thái giao dịch xuyên suốt luồng
--   3. Traceability: mapping booking ↔ gateway transaction
CREATE TABLE payment_requests (
    payment_request_id SERIAL PRIMARY KEY,

    -- Liên kết với booking (1 booking chỉ có tối đa 1 payment request active)
    booking_id INT NOT NULL REFERENCES bookings(booking_id),

    -- Cổng thanh toán: MOMO, VNPAY, ZALOPAY, ...
    gateway VARCHAR(30) NOT NULL,

    -- Thông tin gửi lên gateway
    request_id VARCHAR(100) NOT NULL,         -- Idempotency key do client tạo (lưu để trace)
    gateway_request_id VARCHAR(100),          -- UUID do server sinh khi gọi MoMo API (idempotency server→MoMo)
    order_id VARCHAR(200) NOT NULL,           -- Mã đơn hàng gửi lên gateway
    amount BIGINT NOT NULL,                   -- Số tiền (VND)
    order_info VARCHAR(255),                  -- Mô tả đơn hàng

    -- Thông tin gateway trả về khi tạo đơn
    pay_url TEXT,                             -- URL redirect sang trang thanh toán
    deeplink TEXT,                            -- URL mở app thanh toán trực tiếp
    qr_code_url TEXT,                         -- Dữ liệu để tạo mã QR
    result_code INTEGER,                      -- resultCode từ gateway khi tạo đơn
    response_message TEXT,                    -- message từ gateway

    -- Thông tin sau khi user thanh toán (cập nhật từ IPN/callback)
    gateway_trans_id VARCHAR(100),            -- Mã giao dịch phía gateway
    pay_type VARCHAR(30),                     -- Hình thức TT: qr, webApp, app, ...

    -- Trạng thái nội bộ
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED'
        CHECK (status IN ('CREATED', 'PAID', 'FAILED')),
        -- CREATED: đã tạo đơn thành công, chờ user thanh toán
        -- PAID: callback xác nhận thanh toán thành công
        -- FAILED: callback xác nhận thất bại

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index để query nhanh theo bookingId (không unique — 1 booking có nhiều payment requests khi retry)
CREATE INDEX idx_payment_requests_booking_id
    ON payment_requests (booking_id);

-- Index tìm theo bookingId + requestId (dùng cho idempotency check client→server)
CREATE INDEX idx_payment_requests_booking_request
    ON payment_requests (booking_id, request_id);

-- Tra cứu theo gateway + order_id (dùng khi nhận IPN)
CREATE INDEX idx_payment_requests_gateway_order
    ON payment_requests (gateway, order_id);
