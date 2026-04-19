-- Compatibility patch for legacy schemas where bill_item.description is NOT NULL.
-- POS inserts omit description, so strict mode fails unless column is nullable/defaulted.

DROP PROCEDURE IF EXISTS sp_patch_bill_item_description;

DELIMITER $$
CREATE PROCEDURE sp_patch_bill_item_description()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'bill_item'
          AND column_name = 'description'
    ) THEN
        -- Backfill blank description rows before relaxing constraints.
        UPDATE bill_item
        SET description = COALESCE(NULLIF(TRIM(medicine_name), ''), 'Item')
        WHERE description IS NULL OR TRIM(description) = '';

        ALTER TABLE bill_item
            MODIFY COLUMN description VARCHAR(255) NULL;
    END IF;
END$$
DELIMITER ;

CALL sp_patch_bill_item_description();
DROP PROCEDURE IF EXISTS sp_patch_bill_item_description;

