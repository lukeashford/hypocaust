-- Fix missing created_at defaults to match BaseEntity requirements
-- This migration ensures all tables that extend BaseEntity have created_at with default now()

-- Add missing created_at column
ALTER TABLE run
    ADD COLUMN created_at timestamptz NOT NULL DEFAULT now();

ALTER TABLE event_log
    ADD COLUMN created_at timestamptz NOT NULL DEFAULT now();

-- Add default now()
ALTER TABLE thread
    ALTER COLUMN created_at SET DEFAULT now();

ALTER TABLE message
    ALTER COLUMN created_at SET DEFAULT now();

ALTER TABLE artifact
    ALTER COLUMN created_at SET DEFAULT now();