
-- Mövcud task status constraint-ini sil
ALTER TABLE employee_tasks
DROP CONSTRAINT IF EXISTS employee_tasks_status_check;

-- PAUSED statusunu əlavə etməklə constraint-i yenidən yarat
ALTER TABLE employee_tasks
    ADD CONSTRAINT employee_tasks_status_check
        CHECK (
            status IN (
                       'PENDING',
                       'IN_PROGRESS',
                       'COMPLETED',
                       'PAUSED'
                )
            );
