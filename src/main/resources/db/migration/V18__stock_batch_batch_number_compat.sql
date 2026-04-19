-- Fix legacy schemas where both batch_no and batch_number exist,
-- and batch_number is NOT NULL without a default.

DROP PROCEDURE IF EXISTS sp_fix_stock_batch_batch_number_compat;

DELIMITER $$
CREATE PROCEDURE sp_fix_stock_batch_batch_number_compat()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'stock_batch' AND column_name = 'batch_no'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'stock_batch' AND column_name = 'batch_number'
    ) THEN
        -- Keep historical values but ensure inserts that only set batch_no do not fail.
        UPDATE stock_batch
        SET batch_number = batch_no
        WHERE (batch_number IS NULL OR TRIM(batch_number) = '')
          AND batch_no IS NOT NULL
          AND TRIM(batch_no) <> '';

        ALTER TABLE stock_batch
            MODIFY COLUMN batch_number VARCHAR(100) NULL;
    END IF;
END$$
DELIMITER ;

CALL sp_fix_stock_batch_batch_number_compat();
DROP PROCEDURE IF EXISTS sp_fix_stock_batch_batch_number_compat;

