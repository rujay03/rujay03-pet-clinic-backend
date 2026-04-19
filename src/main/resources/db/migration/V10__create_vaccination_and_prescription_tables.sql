CREATE TABLE IF NOT EXISTS vaccination (
    vaccination_id BIGINT NOT NULL AUTO_INCREMENT,
    pet_id BIGINT NOT NULL,
    given_by_staff_id BIGINT NOT NULL,
    vaccine_name VARCHAR(120) NOT NULL,
    given_at DATETIME NOT NULL,
    valid_until DATE DEFAULT NULL,
    notes TEXT DEFAULT NULL,
    created_at DATETIME NOT NULL,

    PRIMARY KEY (vaccination_id),
    CONSTRAINT fk_vaccination_pet FOREIGN KEY (pet_id)
        REFERENCES pet (pet_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_vaccination_staff FOREIGN KEY (given_by_staff_id)
        REFERENCES staff (staff_id)
        ON DELETE RESTRICT,
    INDEX idx_vaccination_pet (pet_id),
    INDEX idx_vaccination_given_at (given_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS prescription (
    prescription_id BIGINT NOT NULL AUTO_INCREMENT,
    pet_id BIGINT NOT NULL,
    prescribed_by_staff_id BIGINT NOT NULL,
    prescribed_at DATETIME NOT NULL,
    diagnosis TEXT DEFAULT NULL,
    notes TEXT DEFAULT NULL,
    created_at DATETIME NOT NULL,

    PRIMARY KEY (prescription_id),
    CONSTRAINT fk_prescription_pet FOREIGN KEY (pet_id)
        REFERENCES pet (pet_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_prescription_staff FOREIGN KEY (prescribed_by_staff_id)
        REFERENCES staff (staff_id)
        ON DELETE RESTRICT,
    INDEX idx_prescription_pet (pet_id),
    INDEX idx_prescription_prescribed_at (prescribed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS prescription_item (
    prescription_item_id BIGINT NOT NULL AUTO_INCREMENT,
    prescription_id BIGINT NOT NULL,
    medicine_name VARCHAR(150) NOT NULL,
    dosage VARCHAR(80) NOT NULL,
    frequency VARCHAR(80) NOT NULL,
    duration_days INT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    instructions VARCHAR(255) DEFAULT NULL,

    PRIMARY KEY (prescription_item_id),
    CONSTRAINT fk_prescription_item_prescription FOREIGN KEY (prescription_id)
        REFERENCES prescription (prescription_id)
        ON DELETE CASCADE,
    INDEX idx_prescription_item_prescription (prescription_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

