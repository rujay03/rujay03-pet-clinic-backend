CREATE TABLE IF NOT EXISTS pet_type_catalog (
    type_id BIGINT NOT NULL AUTO_INCREMENT,
    type_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (type_id),
    CONSTRAINT uq_pet_type_catalog_name UNIQUE (type_name)
);

INSERT INTO pet_type_catalog (type_name)
SELECT DISTINCT TRIM(p.species)
FROM pet p
WHERE p.species IS NOT NULL
  AND TRIM(p.species) <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM pet_type_catalog c
      WHERE LOWER(c.type_name) = LOWER(TRIM(p.species))
  );

