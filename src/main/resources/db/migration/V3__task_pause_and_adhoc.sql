-- Fasilə/davam üçün seqment vaxtı və əlavə fəaliyyətlər (ad-hoc)
ALTER TABLE employee_tasks
    ADD COLUMN IF NOT EXISTS paused_at TIMESTAMP;
ALTER TABLE employee_tasks
    ADD COLUMN IF NOT EXISTS last_resumed_at TIMESTAMP;
ALTER TABLE employee_tasks
    ADD COLUMN IF NOT EXISTS allocated_minutes INTEGER;
ALTER TABLE employee_tasks
    ADD COLUMN IF NOT EXISTS kind VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED';
ALTER TABLE employee_tasks
    ADD COLUMN IF NOT EXISTS paused_from_task_id BIGINT;

ALTER TABLE employee_tasks
    DROP CONSTRAINT IF EXISTS fk_employee_tasks_paused_from;
ALTER TABLE employee_tasks
    ADD CONSTRAINT fk_employee_tasks_paused_from
        FOREIGN KEY (paused_from_task_id) REFERENCES employee_tasks (id) ON DELETE SET NULL;

-- Artıq icrada olan tapşırıqlarda cari seqmentin başlanğıcını bərpa et
UPDATE employee_tasks
SET last_resumed_at = started_at
WHERE status = 'IN_PROGRESS'
  AND last_resumed_at IS NULL
  AND started_at IS NOT NULL;
