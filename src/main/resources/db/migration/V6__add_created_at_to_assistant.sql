-- Add missing created_at column to assistant table to match BaseEntity
ALTER TABLE assistant
    ADD COLUMN created_at timestamptz NOT NULL DEFAULT now();
