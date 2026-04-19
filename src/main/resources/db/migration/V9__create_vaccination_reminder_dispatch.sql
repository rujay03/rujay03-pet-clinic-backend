CREATE TABLE IF NOT EXISTS vaccination_reminder_dispatch (
    dispatch_id BIGINT NOT NULL AUTO_INCREMENT,
    appointment_id BIGINT NOT NULL,
    reminder_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(500) DEFAULT NULL,
    created_at DATETIME NOT NULL,

    PRIMARY KEY (dispatch_id),
    CONSTRAINT fk_vrd_appointment FOREIGN KEY (appointment_id)
        REFERENCES appointment (appointment_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
