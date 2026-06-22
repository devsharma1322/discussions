-- V1__init.sql
-- Schema for AnonCircles discussions-service
-- Tables: users, auth_tokens, circles, memberships, threads, messages

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email               TEXT NOT NULL,
    password_hash       TEXT NOT NULL,           -- bcrypt cost 12
    email_verified_at   TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT users_email_unique UNIQUE (email)
);
CREATE INDEX idx_users_email ON users (lower(email));

CREATE TABLE auth_tokens (
    token        TEXT PRIMARY KEY,                -- opaque random string
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    purpose      TEXT NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL,
    used_at      TIMESTAMPTZ,                     -- single-use
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT auth_tokens_purpose_check CHECK (purpose IN ('verify_email', 'reset_password'))
);
CREATE INDEX idx_auth_tokens_user ON auth_tokens (user_id, purpose);

CREATE TABLE circles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic           TEXT NOT NULL,                 -- immutable after creation
    description     TEXT NOT NULL,
    admin_user_id   UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    member_count    INTEGER NOT NULL DEFAULT 1,    -- maintained by trigger (V2)
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_circles_member_count ON circles (member_count DESC, created_at DESC);
CREATE INDEX idx_circles_topic_trgm ON circles USING gin (topic gin_trgm_ops);

CREATE TABLE memberships (
    user_id     UUID NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
    circle_id   UUID NOT NULL REFERENCES circles(id) ON DELETE CASCADE,
    handle      TEXT NOT NULL,                       -- per-circle pseudonym
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, circle_id)
);
CREATE INDEX idx_memberships_circle ON memberships (circle_id);

CREATE TABLE threads (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    circle_id    UUID NOT NULL REFERENCES circles(id) ON DELETE CASCADE,
    title        TEXT NOT NULL,
    created_by   TEXT NOT NULL,                      -- handle snapshot
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_threads_circle ON threads (circle_id, created_at DESC);

CREATE TABLE messages (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    thread_id   UUID NOT NULL REFERENCES threads(id) ON DELETE CASCADE,
    body        TEXT NOT NULL,
    author      TEXT NOT NULL,                       -- handle snapshot
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_messages_thread ON messages (thread_id, created_at ASC);
