CREATE TABLE IF NOT EXISTS appointment_type_catalog (
    type_id BIGINT NOT NULL AUTO_INCREMENT,
    type_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (type_id),
    CONSTRAINT uq_appointment_type_catalog_name UNIQUE (type_name)
);

INSERT INTO appointment_type_catalog (type_name)
SELECT DISTINCT TRIM(a.appointment_type)
FROM appointment a
WHERE a.appointment_type IS NOT NULL
  AND TRIM(a.appointment_type) <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM appointment_type_catalog c
      WHERE LOWER(c.type_name) = LOWER(TRIM(a.appointment_type))
  );

