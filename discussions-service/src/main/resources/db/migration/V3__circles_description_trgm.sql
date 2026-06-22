-- V3__circles_description_trgm.sql
-- GIN trigram index on circles.description so the `search` query parameter
-- (which matches topic OR description) stays index-backed at scale.

CREATE INDEX idx_circles_description_trgm
    ON circles USING gin (description gin_trgm_ops);
