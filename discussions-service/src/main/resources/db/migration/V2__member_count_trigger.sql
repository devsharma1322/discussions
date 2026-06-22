-- V2__member_count_trigger.sql
-- Maintains circles.member_count transactionally on membership INSERT/DELETE.

CREATE OR REPLACE FUNCTION fn_circle_member_count() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE circles
           SET member_count = member_count + 1
         WHERE id = NEW.circle_id;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE circles
           SET member_count = member_count - 1
         WHERE id = OLD.circle_id;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_memberships_member_count
AFTER INSERT OR DELETE ON memberships
FOR EACH ROW EXECUTE FUNCTION fn_circle_member_count();

-- Rollback (manual):
--   DROP TRIGGER IF EXISTS trg_memberships_member_count ON memberships;
--   DROP FUNCTION IF EXISTS fn_circle_member_count();
