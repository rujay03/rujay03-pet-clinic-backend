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
    CONSTRAINT fk_stock_batch_medicine FOREIGN KEY (medicine_id)
        REFERENCES medicine (medicine_id)
        ON DELETE RESTRICT,
    CONSTRAINT uk_stock_batch_medicine_batch UNIQUE (medicine_id, batch_no),
    INDEX idx_stock_batch_medicine (medicine_id),
    INDEX idx_stock_batch_expiry (expiry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

