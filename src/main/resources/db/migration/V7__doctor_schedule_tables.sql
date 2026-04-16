-- Doctor weekly schedule and date overrides
CREATE TABLE IF NOT EXISTS doctor_weekly_schedule (
    schedule_id      BIGINT      NOT NULL AUTO_INCREMENT,
    doctor_id        BIGINT               DEFAULT NULL,
    day_of_week      VARCHAR(10) NOT NULL,
    slot_start       TIME        NOT NULL,
    slot_end         TIME        NOT NULL,
    is_active        BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (schedule_id),
    CONSTRAINT fk_weekly_doctor FOREIGN KEY (doctor_id) REFERENCES staff (staff_id) ON DELETE CASCADE,
    CONSTRAINT uq_weekly UNIQUE (doctor_id, day_of_week, slot_start)
);

CREATE TABLE IF NOT EXISTS doctor_schedule_override (
    override_id      BIGINT      NOT NULL AUTO_INCREMENT,
    doctor_id        BIGINT      NOT NULL,
    override_date    DATE        NOT NULL,
    slot_start       TIME        NOT NULL,
    slot_end         TIME        NOT NULL,
    is_available     BOOLEAN     NOT NULL DEFAULT TRUE,
    reason           VARCHAR(255) DEFAULT NULL,
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (override_id),
    CONSTRAINT fk_override_doctor FOREIGN KEY (doctor_id) REFERENCES staff (staff_id) ON DELETE CASCADE,
    CONSTRAINT uq_override UNIQUE (doctor_id, override_date, slot_start)
);

-- Prevent double-booking per doctor/date/time (allows multiple NULL staff)
ALTER TABLE appointment
    ADD INDEX idx_appointment_staff_date_time (staff_id, appointment_date, appointment_time);

-- Seed default weekly slots for all doctors (doctor_id NULL means global default)
INSERT INTO doctor_weekly_schedule (doctor_id, day_of_week, slot_start, slot_end)
SELECT NULL, d.day_of_week, s.slot_start, s.slot_end
FROM (
    SELECT 'MONDAY' AS day_of_week UNION SELECT 'TUESDAY' UNION SELECT 'WEDNESDAY' UNION SELECT 'THURSDAY' UNION SELECT 'FRIDAY' UNION SELECT 'SATURDAY' UNION SELECT 'SUNDAY'
) d
CROSS JOIN (
    SELECT TIME('08:00:00') AS slot_start, TIME('15:00:00') AS slot_end
    UNION ALL
    SELECT TIME('21:30:00') AS slot_start, TIME('23:00:00') AS slot_end
) s
WHERE NOT EXISTS (
    SELECT 1 FROM doctor_weekly_schedule e
    WHERE e.doctor_id IS NULL AND e.day_of_week = d.day_of_week AND e.slot_start = s.slot_start
);
