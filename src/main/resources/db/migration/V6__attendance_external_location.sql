-- Kənardan / səfərdən giriş üçün GPS və xarici lokasiya bayrağı
ALTER TABLE attendances
    ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS is_external BOOLEAN DEFAULT FALSE;

ALTER TABLE attendances
    ADD COLUMN IF NOT EXISTS location_note VARCHAR(500);

UPDATE attendances
SET is_external = FALSE
WHERE is_external IS NULL;

ALTER TABLE attendances
    ALTER COLUMN is_external SET DEFAULT FALSE;

ALTER TABLE attendances
    ALTER COLUMN is_external SET NOT NULL;
