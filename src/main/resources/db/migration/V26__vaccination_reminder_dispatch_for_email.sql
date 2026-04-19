ALTER TABLE vaccination_reminder_dispatch
    MODIFY appointment_id BIGINT NULL;

ALTER TABLE vaccination_reminder_dispatch
    ADD COLUMN vaccination_id BIGINT NULL AFTER appointment_id;

ALTER TABLE vaccination_reminder_dispatch
    ADD COLUMN reminder_type VARCHAR(20) NOT NULL DEFAULT 'WEEK_BEFORE' AFTER reminder_date;

ALTER TABLE vaccination_reminder_dispatch
    ADD INDEX idx_vrd_vaccination_id (vaccination_id);

ALTER TABLE vaccination_reminder_dispatch
    ADD CONSTRAINT fk_vrd_vaccination FOREIGN KEY (vaccination_id)
        REFERENCES vaccination (vaccination_id)
        ON DELETE CASCADE;

ALTER TABLE vaccination_reminder_dispatch
    ADD UNIQUE KEY uk_vrd_vaccination_type (vaccination_id, reminder_type);

