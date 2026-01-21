-- Add subtitle and alt columns to artifact table for richer display metadata
ALTER TABLE artifact ADD COLUMN subtitle text;
ALTER TABLE artifact ADD COLUMN alt text;
