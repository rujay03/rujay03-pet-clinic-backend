-- Medicine table should store medicine details only.
-- Stock units and reorder management are handled in stock_batch.

DROP PROCEDURE IF EXISTS sp_align_medicine_columns_with_stock_batch;

DELIMITER $$
CREATE PROCEDURE sp_align_medicine_columns_with_stock_batch()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'medicine' AND column_name = 'generic_name'
    ) THEN
        ALTER TABLE medicine ADD COLUMN generic_name VARCHAR(150) NULL AFTER name;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'medicine' AND column_name = 'form'
    ) THEN
        ALTER TABLE medicine ADD COLUMN form VARCHAR(60) NULL AFTER generic_name;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'medicine' AND column_name = 'strength'
    ) THEN
        ALTER TABLE medicine ADD COLUMN strength VARCHAR(60) NULL AFTER form;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'medicine' AND column_name = 'unit'
    ) THEN
        ALTER TABLE medicine DROP COLUMN unit;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'medicine' AND column_name = 'reorder_level'
    ) THEN
        ALTER TABLE medicine DROP COLUMN reorder_level;
    END IF;
END$$
DELIMITER ;

CALL sp_align_medicine_columns_with_stock_batch();
DROP PROCEDURE IF EXISTS sp_align_medicine_columns_with_stock_batch;

