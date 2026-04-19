-- Fix legacy prescription schema that still requires treatment_id / medicine_id.
-- New implementation writes pet-based prescriptions and free-text prescription items.

DROP PROCEDURE IF EXISTS sp_fix_legacy_prescription_columns;

DELIMITER $$
CREATE PROCEDURE sp_fix_legacy_prescription_columns()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'prescription'
          AND column_name = 'treatment_id'
    ) THEN
        -- Legacy schema may enforce NOT NULL and block inserts from new model.
        ALTER TABLE prescription
            MODIFY COLUMN treatment_id BIGINT NULL;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'prescription_item'
          AND column_name = 'medicine_id'
    ) THEN
        -- Legacy schema may enforce NOT NULL and block inserts from new model.
        ALTER TABLE prescription_item
            MODIFY COLUMN medicine_id BIGINT NULL;
    END IF;
END$$
DELIMITER ;

CALL sp_fix_legacy_prescription_columns();
DROP PROCEDURE IF EXISTS sp_fix_legacy_prescription_columns;

