-- Migration: Add task_execution_id to event table
ALTER TABLE event
    ADD COLUMN task_execution_id UUID REFERENCES task_execution (id);
CREATE INDEX idx_event_task_execution_id ON event (task_execution_id);
