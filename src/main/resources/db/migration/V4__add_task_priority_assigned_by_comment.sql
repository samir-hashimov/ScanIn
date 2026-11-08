ALTER TABLE employee_tasks
    ADD COLUMN IF NOT EXISTS assigned_by VARCHAR(255) DEFAULT 'HR / Müdiriyyət';
ALTER TABLE employee_tasks
    ADD COLUMN IF NOT EXISTS priority VARCHAR(50) DEFAULT 'NORMAL';
ALTER TABLE employee_tasks
    ADD COLUMN IF NOT EXISTS completion_comment TEXT;

UPDATE employee_tasks
SET assigned_by = 'HR / Müdiriyyət'
WHERE assigned_by IS NULL
   OR assigned_by = '';

UPDATE employee_tasks
SET priority = 'NORMAL'
WHERE priority IS NULL
   OR priority = '';
