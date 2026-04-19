-- Ensure prescription_item supports medicine foreign key for doctor prescriptions.

DROP PROCEDURE IF EXISTS sp_add_prescription_item_medicine_fk;

DELIMITER $$
CREATE PROCEDURE sp_add_prescription_item_medicine_fk()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'prescription_item'
          AND column_name = 'medicine_id'
    ) THEN
        ALTER TABLE prescription_item
            ADD COLUMN medicine_id BIGINT NULL AFTER prescription_id;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'prescription_item'
          AND column_name = 'medicine_name'
    ) THEN
        UPDATE prescription_item pi
        JOIN medicine m ON LOWER(TRIM(pi.medicine_name)) = LOWER(TRIM(m.name))
        SET pi.medicine_id = m.medicine_id
        WHERE pi.medicine_id IS NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'prescription_item'
          AND index_name = 'idx_prescription_item_medicine'
    ) THEN
        CREATE INDEX idx_prescription_item_medicine ON prescription_item(medicine_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = DATABASE()
          AND table_name = 'prescription_item'
          AND constraint_name = 'fk_prescription_item_medicine'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        ALTER TABLE prescription_item
            ADD CONSTRAINT fk_prescription_item_medicine
            FOREIGN KEY (medicine_id)
            REFERENCES medicine(medicine_id)
            ON DELETE RESTRICT;
    END IF;
END$$
DELIMITER ;

CALL sp_add_prescription_item_medicine_fk();
DROP PROCEDURE IF EXISTS sp_add_prescription_item_medicine_fk;

