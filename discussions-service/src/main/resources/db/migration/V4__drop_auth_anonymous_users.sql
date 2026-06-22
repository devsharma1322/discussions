-- V4__drop_auth_anonymous_users.sql
-- Switches the platform from email/password auth to pure anonymous sessions.
-- Every user is identified by a generated display name and a UUID; the JWT
-- still tracks the visitor across requests.

DROP TABLE IF EXISTS auth_tokens;

-- Drop email + its dependent unique constraint + lower(email) index via CASCADE.
ALTER TABLE users DROP COLUMN IF EXISTS email CASCADE;
ALTER TABLE users DROP COLUMN IF EXISTS password_hash;
ALTER TABLE users DROP COLUMN IF EXISTS email_verified_at;

-- Add display_name. Default only used for the migration step itself; the
-- application always supplies a value, so the default is dropped afterwards.
ALTER TABLE users ADD COLUMN display_name TEXT NOT NULL DEFAULT 'Anonymous';
ALTER TABLE users ALTER COLUMN display_name DROP DEFAULT;
