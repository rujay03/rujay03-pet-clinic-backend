CREATE TABLE IF NOT EXISTS bill (
    bill_id BIGINT NOT NULL AUTO_INCREMENT,
    bill_no VARCHAR(50) NOT NULL,
    billed_at DATETIME NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(12,2) NOT NULL,
    payment_method VARCHAR(40) NOT NULL,
    notes VARCHAR(255) DEFAULT NULL,
    created_by_staff_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,

    PRIMARY KEY (bill_id),
    CONSTRAINT uk_bill_bill_no UNIQUE (bill_no),
    CONSTRAINT fk_bill_created_by_staff FOREIGN KEY (created_by_staff_id)
        REFERENCES staff (staff_id)
        ON DELETE RESTRICT,
    INDEX idx_bill_billed_at (billed_at),
    INDEX idx_bill_created_by_staff (created_by_staff_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bill_item (
    bill_item_id BIGINT NOT NULL AUTO_INCREMENT,
    bill_id BIGINT NOT NULL,
    medicine_id BIGINT NOT NULL,
    medicine_name VARCHAR(150) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    line_total DECIMAL(12,2) NOT NULL,

    PRIMARY KEY (bill_item_id),
    CONSTRAINT fk_bill_item_bill FOREIGN KEY (bill_id)
        REFERENCES bill (bill_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_bill_item_medicine FOREIGN KEY (medicine_id)
        REFERENCES medicine (medicine_id)
        ON DELETE RESTRICT,
    INDEX idx_bill_item_bill (bill_id),
    INDEX idx_bill_item_medicine (medicine_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

