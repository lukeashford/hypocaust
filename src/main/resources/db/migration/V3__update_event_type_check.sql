-- Update event_type_check to include operator events
ALTER TABLE event DROP CONSTRAINT IF EXISTS event_type_check;

ALTER TABLE event
    ADD CONSTRAINT event_type_check CHECK (type IN (
                                                    'artifact.scheduled', 'artifact.created',
                                                    'artifact.cancelled',
                                                    'run.scheduled', 'run.started', 'run.completed',
                                                    'tool.calling',
                                                    'operator.started', 'operator.finished',
                                                    'operator.failed',
                                                    'error'
        ));
