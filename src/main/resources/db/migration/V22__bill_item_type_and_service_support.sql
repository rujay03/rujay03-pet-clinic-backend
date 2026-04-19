-- Support mixed POS line items (PRODUCT/SERVICE).

DROP PROCEDURE IF EXISTS sp_bill_item_type_and_service_support;

DELIMITER $$
CREATE PROCEDURE sp_bill_item_type_and_service_support()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'bill_item' AND column_name = 'item_type'
    ) THEN
        ALTER TABLE bill_item
            ADD COLUMN item_type VARCHAR(20) NOT NULL DEFAULT 'PRODUCT' AFTER bill_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'bill_item' AND column_name = 'description'
    ) THEN
        ALTER TABLE bill_item
            ADD COLUMN description VARCHAR(255) NULL AFTER medicine_name;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'bill_item' AND column_name = 'medicine_id'
    ) THEN
        ALTER TABLE bill_item
            MODIFY COLUMN medicine_id BIGINT NULL;
    END IF;

    UPDATE bill_item
    SET item_type = 'PRODUCT'
    WHERE item_type IS NULL OR TRIM(item_type) = '';
END$$
DELIMITER ;

CALL sp_bill_item_type_and_service_support();
DROP PROCEDURE IF EXISTS sp_bill_item_type_and_service_support;

