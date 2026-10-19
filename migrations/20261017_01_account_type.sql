-- Apply once to the existing schema, before deploying code that maps account_type.
-- Preflight: backup users/user_roles/refresh_tokens; verify this column/index/function
-- do not already exist. Stop on schema drift; do not rerun blindly.
-- Existing rows stay REGISTERED. Never infer WALK_IN from email/name/numeric ID.
-- This migration does NOT seed WALK_IN or enable counter booking.
-- Rollback: the additive schema may stay in place, but do NOT roll back to code
-- without the system-account guards after any WALK_IN record has been created.
BEGIN;
SET LOCAL lock_timeout = '5s';

ALTER TABLE users ADD COLUMN account_type VARCHAR(20) NOT NULL DEFAULT 'REGISTERED'
    CONSTRAINT chk_users_account_type CHECK (account_type IN ('REGISTERED', 'WALK_IN'));
CREATE UNIQUE INDEX uq_users_walk_in ON users (account_type) WHERE account_type = 'WALK_IN';

CREATE FUNCTION prevent_user_account_type_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.account_type IS DISTINCT FROM OLD.account_type THEN
        RAISE EXCEPTION 'Account type is immutable' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_users_account_type_immutable
    BEFORE UPDATE OF account_type ON users
    FOR EACH ROW EXECUTE FUNCTION prevent_user_account_type_change();

COMMIT;
