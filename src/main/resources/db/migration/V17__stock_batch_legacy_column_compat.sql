-- Normalize legacy stock_batch schemas to match JPA mappings.

CREATE TABLE IF NOT EXISTS stock_batch (
    stock_batch_id BIGINT NOT NULL AUTO_INCREMENT,
    medicine_id BIGINT NOT NULL,
    batch_no VARCHAR(100) NOT NULL,
    expiry_date DATE DEFAULT NULL,
    purchase_price DECIMAL(10,2) DEFAULT NULL,
    quantity_available INT NOT NULL,
    received_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (stock_batch_id),
    INDEX idx_stock_batch_medicine (medicine_id),
    INDEX idx_stock_batch_expiry (expiry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP PROCEDURE IF EXISTS sp_patch_stock_batch_legacy_columns;

DELIMITER $$
CREATE PROCEDURE sp_patch_stock_batch_legacy_columns()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'stock_batch' AND column_name = 'id'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'stock_batch' AND column_name = 'stock_batch_id'
    ) THEN
        ALTER TABLE stock_batch
            CHANGE COLUMN id stock_batch_id BIGINT NOT NULL AUTO_INCREMENT;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'stock_batch' AND column_name = 'batch_id'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'stock_batch' AND column_name = 'stock_batch_id'
    ) THEN
        ALTER TABLE stock_batch
            CHANGE COLUMN batch_id stock_batch_id BIGINT NOT NULL AUTO_INCREMENT;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'stock_batch' AND column_name = 'batch_number'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'stock_batch' AND column_name = 'batch_no'
    ) THEN
        ALTER TABLE stock_batch
            CHANGE COLUMN batch_number batch_no VARCHAR(100) NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'stock_batch' AND column_name = 'qty_on_hand'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'stock_batch' AND column_name = 'quantity_available'
    ) THEN
        ALTER TABLE stock_batch
            CHANGE COLUMN qty_on_hand quantity_available INT NOT NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'stock_batch' AND column_name = 'created_at'
    ) THEN
        ALTER TABLE stock_batch
            ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'stock_batch' AND column_name = 'stock_batch_id'
    ) THEN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.table_constraints
            WHERE table_schema = DATABASE()
              AND table_name = 'stock_batch'
              AND constraint_type = 'PRIMARY KEY'
        ) THEN
            ALTER TABLE stock_batch
                ADD PRIMARY KEY (stock_batch_id);
        END IF;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'stock_batch'
          AND index_name = 'uk_stock_batch_medicine_batch'
    ) THEN
        CREATE UNIQUE INDEX uk_stock_batch_medicine_batch ON stock_batch(medicine_id, batch_no);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'stock_batch'
          AND index_name = 'idx_stock_batch_medicine'
    ) THEN
        CREATE INDEX idx_stock_batch_medicine ON stock_batch(medicine_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'stock_batch'
          AND index_name = 'idx_stock_batch_expiry'
    ) THEN
        CREATE INDEX idx_stock_batch_expiry ON stock_batch(expiry_date);
    END IF;
END$$
DELIMITER ;

CALL sp_patch_stock_batch_legacy_columns();
DROP PROCEDURE IF EXISTS sp_patch_stock_batch_legacy_columns;

