-- Compatibility patch: normalize stock_batch.purchase_price to nullable.

DROP PROCEDURE IF EXISTS sp_patch_stock_batch_purchase_price;

DELIMITER $$
CREATE PROCEDURE sp_patch_stock_batch_purchase_price()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'stock_batch'
          AND column_name = 'purchase_price'
    ) THEN
        ALTER TABLE stock_batch
            MODIFY COLUMN purchase_price DECIMAL(10,2) NULL;
    ELSE
        ALTER TABLE stock_batch
            ADD COLUMN purchase_price DECIMAL(10,2) NULL AFTER expiry_date;
    END IF;
END$$
DELIMITER ;

CALL sp_patch_stock_batch_purchase_price();
DROP PROCEDURE IF EXISTS sp_patch_stock_batch_purchase_price;

