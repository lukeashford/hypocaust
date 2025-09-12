-- Alter operator_embeddings table to use UUID for id column instead of BIGSERIAL
-- This migration changes the primary key from BIGSERIAL to UUID for consistency with other entities

-- Drop the existing index that depends on the primary key
DROP INDEX IF EXISTS idx_operator_embeddings_vector;

-- Drop the primary key constraint
ALTER TABLE operator_embeddings
    DROP CONSTRAINT IF EXISTS operator_embeddings_pkey;

-- Add a new UUID column
ALTER TABLE operator_embeddings
    ADD COLUMN id_uuid UUID DEFAULT gen_random_uuid();

-- Update existing records to have UUID values (if any exist)
UPDATE operator_embeddings
SET id_uuid = gen_random_uuid()
WHERE id_uuid IS NULL;

-- Make the new UUID column NOT NULL
ALTER TABLE operator_embeddings
    ALTER COLUMN id_uuid SET NOT NULL;

-- Drop the old BIGSERIAL id column
ALTER TABLE operator_embeddings
    DROP COLUMN id;

-- Rename the new UUID column to id
ALTER TABLE operator_embeddings
    RENAME COLUMN id_uuid TO id;

-- Add the primary key constraint back
ALTER TABLE operator_embeddings
    ADD PRIMARY KEY (id);

-- Recreate the vector similarity search index
CREATE INDEX idx_operator_embeddings_vector ON operator_embeddings
    USING ivfflat (embedding vector_cosine_ops);