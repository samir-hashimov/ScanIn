-- 1. İşçilər, HR və Admin cədvəli
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       employee_code VARCHAR(20) NOT NULL UNIQUE,
                       username VARCHAR(50) UNIQUE,          -- NOT NULL silindi (İşçi özü yazacaq)
                       password VARCHAR(255),                -- NOT NULL silindi (İşçi özü yazacaq)
                       full_name VARCHAR(100) NOT NULL,
                       position VARCHAR(100) NOT NULL,
                       role VARCHAR(20) NOT NULL,
                       shift_start_time TIME NOT NULL DEFAULT '09:00:00',
                       shift_end_time TIME NOT NULL DEFAULT '18:00:00'
);

-- 2. Gündəlik Giriş-Çıxış (Attendance) cədvəli
CREATE TABLE attendances (
                             id BIGSERIAL PRIMARY KEY,
                             user_id BIGINT NOT NULL,
                             work_date DATE NOT NULL,
                             check_in_time TIMESTAMP NOT NULL,
                             check_out_time TIMESTAMP,
                             arrival_status VARCHAR(30) NOT NULL,
                             departure_status VARCHAR(30) NOT NULL,
                             is_first_to_arrive BOOLEAN NOT NULL DEFAULT FALSE,
                             is_last_to_leave BOOLEAN NOT NULL DEFAULT FALSE,
                             late_minutes INT NOT NULL DEFAULT 0,
                             overtime_minutes INT NOT NULL DEFAULT 0,
                             total_worked_minutes INT NOT NULL DEFAULT 0,
                             late_reason VARCHAR(500),
                             early_leave_reason VARCHAR(500),
                             system_note VARCHAR(255),

    -- Foreign Key əlaqəsi (User silinsə qeydlər qarışmasın)
                             CONSTRAINT fk_attendance_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    -- Eyni gündə eyni işçinin yalnız 1 sətri ola bilər
                             CONSTRAINT uk_user_work_date UNIQUE (user_id, work_date)
);

-- 3. Ofis Dinamik IP Ayarları cədvəli
CREATE TABLE office_config (
                               id BIGINT PRIMARY KEY,
                               current_office_ip VARCHAR(50) NOT NULL,
                               updated_at TIMESTAMP NOT NULL,
                               updated_by VARCHAR(50)
);

-- Başlanğıc olaraq 1-ci sətri (default localhost IP ilə) əlavə edirik ki, cədvəl boş qalmasın:
INSERT INTO office_config (id, current_office_ip, updated_at, updated_by)
VALUES (1, '127.0.0.1', CURRENT_TIMESTAMP, 'SYSTEM_INIT');