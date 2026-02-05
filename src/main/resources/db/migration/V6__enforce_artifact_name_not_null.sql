-- Enforce non-null name for artifacts to ensure ironclad versioning
-- First, ensure the column is named 'name' if it was incorrectly named 'file_name'
DO
$$
BEGIN
    IF
EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'artifact' AND column_name = 'file_name') THEN
ALTER TABLE artifact RENAME COLUMN file_name TO name;
END IF;
END $$;

-- Then make it NOT NULL
ALTER TABLE artifact
    ALTER COLUMN name SET NOT NULL;
