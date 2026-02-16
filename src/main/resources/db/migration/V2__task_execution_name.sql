-- Add readable name to task executions for LLM-addressable version lookbacks
ALTER TABLE task_execution ADD COLUMN name VARCHAR(50);
CREATE UNIQUE INDEX idx_task_execution_project_name ON task_execution (project_id, name);
