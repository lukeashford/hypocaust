-- Add unique name column to project table
ALTER TABLE project
    ADD COLUMN name varchar(100);

-- Backfill existing rows with their id as name
UPDATE project SET name = id::text WHERE name IS NULL;

-- Make column NOT NULL after backfill
ALTER TABLE project
    ALTER COLUMN name SET NOT NULL;

-- Add unique constraint
ALTER TABLE project
    ADD CONSTRAINT uq_project_name UNIQUE (name);
