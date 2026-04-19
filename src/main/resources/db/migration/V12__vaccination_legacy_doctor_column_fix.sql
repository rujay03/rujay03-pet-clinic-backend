-- Fix legacy schemas where vaccination.given_by_doctor_id is NOT NULL.
-- New code writes given_by_staff_id, so legacy column must not block inserts.

DROP PROCEDURE IF EXISTS sp_fix_legacy_vaccination_doctor_column;

DELIMITER $$
CREATE PROCEDURE sp_fix_legacy_vaccination_doctor_column()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'vaccination'
          AND column_name = 'given_by_doctor_id'
    ) THEN
        -- Backfill from staff column when possible before relaxing NOT NULL.
        UPDATE vaccination
        SET given_by_doctor_id = given_by_staff_id
        WHERE given_by_doctor_id IS NULL
          AND given_by_staff_id IS NOT NULL;

        -- Relax the legacy column so inserts using the new model do not fail.
        ALTER TABLE vaccination
            MODIFY COLUMN given_by_doctor_id BIGINT NULL;
    END IF;
END$$
DELIMITER ;

CALL sp_fix_legacy_vaccination_doctor_column();
DROP PROCEDURE IF EXISTS sp_fix_legacy_vaccination_doctor_column;

