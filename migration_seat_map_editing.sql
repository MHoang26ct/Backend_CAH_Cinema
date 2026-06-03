-- ============================================================
-- Migration: Seat Map Editing Feature
-- Thực hiện sau khi deploy code mới
-- ============================================================

-- 1. Thêm CANCELLED vào constraint status của bảng showtimes
ALTER TABLE showtimes
    DROP CONSTRAINT IF EXISTS showtimes_status_check;

ALTER TABLE showtimes
    ADD CONSTRAINT showtimes_status_check
        CHECK (status IN ('AVAILABLE', 'SOLD_OUT', 'HIDDEN', 'CANCELLED'));

-- 2. Tạo bảng pending_room_cleanups (dùng cho Room Cloning + Cleanup Scheduler)
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
