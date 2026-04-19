-- Ensure target tables exist for fresh databases.
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
    INDEX idx_prescription_item_prescription (prescription_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Patch existing schemas without relying on unsupported ADD COLUMN IF NOT EXISTS syntax.
DROP PROCEDURE IF EXISTS sp_patch_medical_schema;

DELIMITER $$
CREATE PROCEDURE sp_patch_medical_schema()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'vaccination' AND column_name = 'given_by_staff_id'
    ) THEN
        ALTER TABLE vaccination ADD COLUMN given_by_staff_id BIGINT NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'vaccination' AND column_name = 'vaccine_name'
    ) THEN
        ALTER TABLE vaccination ADD COLUMN vaccine_name VARCHAR(120) NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'vaccination' AND column_name = 'given_at'
    ) THEN
        ALTER TABLE vaccination ADD COLUMN given_at DATETIME NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'vaccination' AND column_name = 'valid_until'
    ) THEN
        ALTER TABLE vaccination ADD COLUMN valid_until DATE NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'vaccination' AND column_name = 'notes'
    ) THEN
        ALTER TABLE vaccination ADD COLUMN notes TEXT NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'vaccination' AND column_name = 'created_at'
    ) THEN
        ALTER TABLE vaccination ADD COLUMN created_at DATETIME NULL;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'vaccination' AND column_name = 'given_by_doctor_id'
    ) THEN
        UPDATE vaccination SET given_by_staff_id = given_by_doctor_id WHERE given_by_staff_id IS NULL;
    END IF;

    UPDATE vaccination SET given_at = COALESCE(given_at, created_at, NOW()) WHERE given_at IS NULL;
    UPDATE vaccination SET created_at = COALESCE(created_at, given_at, NOW()) WHERE created_at IS NULL;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'prescription' AND column_name = 'pet_id'
    ) THEN
        ALTER TABLE prescription ADD COLUMN pet_id BIGINT NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'prescription' AND column_name = 'prescribed_by_staff_id'
    ) THEN
        ALTER TABLE prescription ADD COLUMN prescribed_by_staff_id BIGINT NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'prescription' AND column_name = 'prescribed_at'
    ) THEN
        ALTER TABLE prescription ADD COLUMN prescribed_at DATETIME NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'prescription' AND column_name = 'diagnosis'
    ) THEN
        ALTER TABLE prescription ADD COLUMN diagnosis TEXT NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'prescription' AND column_name = 'notes'
    ) THEN
        ALTER TABLE prescription ADD COLUMN notes TEXT NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'prescription' AND column_name = 'created_at'
    ) THEN
        ALTER TABLE prescription ADD COLUMN created_at DATETIME NULL;
    END IF;

    UPDATE prescription SET prescribed_at = COALESCE(prescribed_at, created_at, NOW()) WHERE prescribed_at IS NULL;
    UPDATE prescription SET created_at = COALESCE(created_at, prescribed_at, NOW()) WHERE created_at IS NULL;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'prescription_item' AND column_name = 'prescription_id'
    ) THEN
        ALTER TABLE prescription_item ADD COLUMN prescription_id BIGINT NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'prescription_item' AND column_name = 'medicine_name'
    ) THEN
        ALTER TABLE prescription_item ADD COLUMN medicine_name VARCHAR(150) NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'prescription_item' AND column_name = 'dosage'
    ) THEN
        ALTER TABLE prescription_item ADD COLUMN dosage VARCHAR(80) NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'prescription_item' AND column_name = 'frequency'
    ) THEN
        ALTER TABLE prescription_item ADD COLUMN frequency VARCHAR(80) NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'prescription_item' AND column_name = 'duration_days'
    ) THEN
        ALTER TABLE prescription_item ADD COLUMN duration_days INT NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'prescription_item' AND column_name = 'quantity'
    ) THEN
        ALTER TABLE prescription_item ADD COLUMN quantity INT NULL DEFAULT 1;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'prescription_item' AND column_name = 'instructions'
    ) THEN
        ALTER TABLE prescription_item ADD COLUMN instructions VARCHAR(255) NULL;
    END IF;

    UPDATE prescription_item SET quantity = 1 WHERE quantity IS NULL;
END$$
DELIMITER ;

CALL sp_patch_medical_schema();
DROP PROCEDURE IF EXISTS sp_patch_medical_schema;

