-- 1. Attendances cədvəlinə kontragent və səfər sütunlarının əlavə edilməsi
ALTER TABLE attendances
    ADD COLUMN IF NOT EXISTS is_field_visit BOOLEAN DEFAULT FALSE;
ALTER TABLE attendances
    ADD COLUMN IF NOT EXISTS counterparty_name VARCHAR(150);
ALTER TABLE attendances
    ADD COLUMN IF NOT EXISTS approval_status VARCHAR(20) DEFAULT 'NOT_REQUIRED';

-- 2. İşçi tapşırıqları üçün employee_tasks cədvəlinin yaradılması
CREATE TABLE IF NOT EXISTS employee_tasks
(
    id             BIGSERIAL PRIMARY KEY,
    external_1c_id VARCHAR(100),
    user_id        BIGINT       NOT NULL,
    title          VARCHAR(300) NOT NULL,
    description    VARCHAR(1000),
    deadline       TIMESTAMP    NOT NULL,
    status         VARCHAR(25)  NOT NULL DEFAULT 'PENDING',
    started_at     TIMESTAMP,
    completed_at   TIMESTAMP,
    spent_minutes  INTEGER               DEFAULT 0,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_employee_tasks_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);