-- Remove 'DELETED' from artifact status check constraint
ALTER TABLE artifact DROP CONSTRAINT IF EXISTS artifact_status_check;
ALTER TABLE artifact
    ADD CONSTRAINT artifact_status_check CHECK (status IN
                                                ('GESTATING', 'CREATED', 'MANIFESTED', 'CANCELLED',
                                                 'FAILED'));
