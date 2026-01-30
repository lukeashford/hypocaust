-- 1. Update status for artifacts currently marked as deleted
UPDATE artifact
SET status = 'DELETED'
WHERE deleted = TRUE;

-- 2. Remove the deleted column
ALTER TABLE artifact DROP COLUMN deleted;

-- 3. Update the status check constraint to include 'DELETED'
-- Since it's an anonymous constraint, we first drop any existing status check and then add a new one.
-- Note: In PostgreSQL, anonymous constraints often follow a pattern, but it's safer to drop and recreate.
-- However, dropping an anonymous constraint without knowing its name is tricky in plain SQL.
-- Given the 'minimal and purpose-driven' guidelines, we will just allow the new value if possible,
-- but the CHECK constraint will prevent it.

-- A better way to handle this in a migration if the constraint name is unknown:
DO
$$
BEGIN
    IF
EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'artifact_status_check') THEN
ALTER TABLE artifact DROP CONSTRAINT artifact_status_check;
END IF;
END $$;

-- Also try the common default name if created by some tools
ALTER TABLE artifact DROP CONSTRAINT IF EXISTS artifact_status_check;

-- Add the new constraint
ALTER TABLE artifact
    ADD CONSTRAINT artifact_status_check CHECK (status IN ('SCHEDULED', 'CREATED', 'CANCELLED', 'DELETED'));
