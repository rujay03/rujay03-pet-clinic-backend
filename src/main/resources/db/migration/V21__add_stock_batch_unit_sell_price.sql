-- Add per-batch unit sell price for POS pricing.

DROP PROCEDURE IF EXISTS sp_add_stock_batch_unit_sell_price;

DELIMITER $$
CREATE PROCEDURE sp_add_stock_batch_unit_sell_price()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'stock_batch'
          AND column_name = 'unit_sell_price'
    ) THEN
        ALTER TABLE stock_batch
            ADD COLUMN unit_sell_price DECIMAL(10,2) NULL AFTER purchase_price;
    END IF;

    UPDATE stock_batch
    SET unit_sell_price = purchase_price
    WHERE unit_sell_price IS NULL
      AND purchase_price IS NOT NULL;
END$$
DELIMITER ;

CALL sp_add_stock_batch_unit_sell_price();
DROP PROCEDURE IF EXISTS sp_add_stock_batch_unit_sell_price;
