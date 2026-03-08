-- Disable FK checks so we can safely drop and recreate the appointment table
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS appointment;

CREATE TABLE appointment (
    appointment_id   BIGINT         NOT NULL AUTO_INCREMENT,
    owner_id         BIGINT         NOT NULL,
    pet_id           BIGINT         NOT NULL,
    staff_id         BIGINT                  DEFAULT NULL,
    appointment_date DATE           NOT NULL,
    appointment_time TIME           NOT NULL,
    appointment_type VARCHAR(100)   NOT NULL,
    status           VARCHAR(30)    NOT NULL DEFAULT 'PENDING',
    notes            TEXT                    DEFAULT NULL,
    created_at       DATETIME       NOT NULL,
    updated_at       DATETIME       NOT NULL,

    PRIMARY KEY (appointment_id),
    CONSTRAINT fk_appt_owner FOREIGN KEY (owner_id)  REFERENCES pet_owner (owner_id) ON DELETE CASCADE,
    CONSTRAINT fk_appt_pet   FOREIGN KEY (pet_id)    REFERENCES pet       (pet_id)   ON DELETE CASCADE,
    CONSTRAINT fk_appt_staff FOREIGN KEY (staff_id)  REFERENCES staff     (staff_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Re-enable FK checks
SET FOREIGN_KEY_CHECKS = 1;


