-- =========================================================
-- Pet Clinic Management System - Normalized Schema (MySQL 8+)
-- =========================================================

SET FOREIGN_KEY_CHECKS = 0;

-- Optional: create database
-- CREATE DATABASE IF NOT EXISTS pet_clinic CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
-- USE pet_clinic;

-- -------------------------
-- 1) Identity & Roles
-- -------------------------
DROP TABLE IF EXISTS user_notification;
DROP TABLE IF EXISTS notification;

DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS bill_item;
DROP TABLE IF EXISTS bill;
DROP TABLE IF EXISTS service;

DROP TABLE IF EXISTS stock_batch;
DROP TABLE IF EXISTS supplier;
DROP TABLE IF EXISTS prescription_item;
DROP TABLE IF EXISTS prescription;
DROP TABLE IF EXISTS treatment_staff;
DROP TABLE IF EXISTS treatment;
DROP TABLE IF EXISTS vaccination;
DROP TABLE IF EXISTS appointment;
DROP TABLE IF EXISTS pet;

DROP TABLE IF EXISTS doctor;
DROP TABLE IF EXISTS staff;
DROP TABLE IF EXISTS pet_owner;

DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
                       user_id           BIGINT PRIMARY KEY AUTO_INCREMENT,
                       email             VARCHAR(255) NOT NULL,
                       password_hash     VARCHAR(255) NOT NULL,
                       status            ENUM('ACTIVE','INACTIVE','SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
                       created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE roles (
                       role_id     INT PRIMARY KEY AUTO_INCREMENT,
                       role_name   VARCHAR(50) NOT NULL,
                       CONSTRAINT uq_roles_name UNIQUE (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed roles (safe if run once on fresh schema)
INSERT INTO roles (role_name) VALUES ('ADMIN'),('DOCTOR'),('PHARMACIST'),('PETOWNER');

CREATE TABLE user_roles (
                            user_id   BIGINT NOT NULL,
                            role_id   INT NOT NULL,
                            PRIMARY KEY (user_id, role_id),
                            CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                            CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------
-- 2) People
-- -------------------------
CREATE TABLE pet_owner (
                           owner_id      BIGINT PRIMARY KEY AUTO_INCREMENT,
                           user_id       BIGINT NOT NULL,
                           full_name     VARCHAR(150) NOT NULL,
                           contact_no    VARCHAR(30) NOT NULL,
                           address       VARCHAR(255),
                           created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           CONSTRAINT uq_pet_owner_user UNIQUE (user_id),
                           CONSTRAINT uq_pet_owner_contact UNIQUE (contact_no),
                           CONSTRAINT fk_pet_owner_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE staff (
                       staff_id      BIGINT PRIMARY KEY AUTO_INCREMENT,
                       user_id       BIGINT NOT NULL,
                       full_name     VARCHAR(150) NOT NULL,
                       contact_no    VARCHAR(30),
                       hire_date     DATE,
                       active        TINYINT(1) NOT NULL DEFAULT 1,
                       created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       CONSTRAINT uq_staff_user UNIQUE (user_id),
                       CONSTRAINT fk_staff_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE doctor (
                        doctor_id        BIGINT PRIMARY KEY AUTO_INCREMENT,
                        staff_id         BIGINT NOT NULL,
                        license_no       VARCHAR(60),
                        specialization   VARCHAR(120),
                        CONSTRAINT uq_doctor_staff UNIQUE (staff_id),
                        CONSTRAINT uq_doctor_license UNIQUE (license_no),
                        CONSTRAINT fk_doctor_staff FOREIGN KEY (staff_id) REFERENCES staff(staff_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------
-- 3) Pets
-- -------------------------
CREATE TABLE pet (
                     pet_id      BIGINT PRIMARY KEY AUTO_INCREMENT,
                     owner_id    BIGINT NOT NULL,
                     name        VARCHAR(100) NOT NULL,
                     species     VARCHAR(60) NOT NULL,
                     breed       VARCHAR(80),
                     sex         ENUM('MALE','FEMALE','UNKNOWN') NOT NULL DEFAULT 'UNKNOWN',
                     dob         DATE,
                     notes       TEXT,
                     created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                     CONSTRAINT fk_pet_owner FOREIGN KEY (owner_id) REFERENCES pet_owner(owner_id) ON DELETE CASCADE,
                     INDEX idx_pet_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------
-- 4) Appointments (visit once confirmed)
-- -------------------------
CREATE TABLE appointment (
                             appointment_id   BIGINT PRIMARY KEY AUTO_INCREMENT,
                             owner_id         BIGINT NOT NULL,
                             pet_id           BIGINT NOT NULL,
                             doctor_id        BIGINT NOT NULL,
                             scheduled_start  DATETIME NOT NULL,
                             duration_min     INT NOT NULL DEFAULT 15,
                             status           ENUM('REQUESTED','CONFIRMED','COMPLETED','CANCELLED','NO_SHOW') NOT NULL DEFAULT 'REQUESTED',
                             notes            TEXT,
                             created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                             CONSTRAINT fk_appt_owner  FOREIGN KEY (owner_id)  REFERENCES pet_owner(owner_id) ON DELETE RESTRICT,
                             CONSTRAINT fk_appt_pet    FOREIGN KEY (pet_id)    REFERENCES pet(pet_id)         ON DELETE RESTRICT,
                             CONSTRAINT fk_appt_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(doctor_id)   ON DELETE RESTRICT,

    -- Basic double-book protection (timeslots)
                             CONSTRAINT uq_doctor_timeslot UNIQUE (doctor_id, scheduled_start),

                             INDEX idx_appt_owner (owner_id),
                             INDEX idx_appt_pet (pet_id),
                             INDEX idx_appt_doctor_time (doctor_id, scheduled_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------
-- 5) Clinical: Treatment + Prescription
-- -------------------------
CREATE TABLE treatment (
                           treatment_id     BIGINT PRIMARY KEY AUTO_INCREMENT,
                           appointment_id   BIGINT NOT NULL,
                           diagnosis        TEXT,
                           notes            TEXT,
                           created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT fk_treatment_appt FOREIGN KEY (appointment_id)
                               REFERENCES appointment(appointment_id) ON DELETE CASCADE,

                           INDEX idx_treatment_appt (appointment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE treatment_staff (
                                 treatment_id       BIGINT NOT NULL,
                                 staff_id           BIGINT NOT NULL,
                                 role_in_treatment  ENUM('DOCTOR','ASSISTANT') NOT NULL DEFAULT 'DOCTOR',
                                 PRIMARY KEY (treatment_id, staff_id),
                                 CONSTRAINT fk_treatment_staff_treatment FOREIGN KEY (treatment_id) REFERENCES treatment(treatment_id) ON DELETE CASCADE,
                                 CONSTRAINT fk_treatment_staff_staff     FOREIGN KEY (staff_id)     REFERENCES staff(staff_id)       ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE medicine (
                          medicine_id     BIGINT PRIMARY KEY AUTO_INCREMENT,
                          name            VARCHAR(150) NOT NULL,
                          generic_name    VARCHAR(150),
                          form            VARCHAR(60),      -- tablet/syrup/etc.
                          strength        VARCHAR(60),      -- e.g., 250mg
                          is_active       TINYINT(1) NOT NULL DEFAULT 1,
                          CONSTRAINT uq_medicine_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE prescription (
                              prescription_id  BIGINT PRIMARY KEY AUTO_INCREMENT,
                              treatment_id     BIGINT NOT NULL,
                              prescribed_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              notes            TEXT,
                              CONSTRAINT fk_prescription_treatment FOREIGN KEY (treatment_id) REFERENCES treatment(treatment_id) ON DELETE CASCADE,
                              INDEX idx_prescription_treatment (treatment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE prescription_item (
                                   prescription_item_id  BIGINT PRIMARY KEY AUTO_INCREMENT,
                                   prescription_id       BIGINT NOT NULL,
                                   medicine_id           BIGINT NOT NULL,
                                   dosage                VARCHAR(80) NOT NULL,   -- e.g., "1 tab"
                                   frequency             VARCHAR(80) NOT NULL,   -- e.g., "2 times a day"
                                   duration_days         INT NOT NULL,
                                   instructions          VARCHAR(255),
                                   qty                   INT NOT NULL DEFAULT 1,

                                   CONSTRAINT fk_pres_item_prescription FOREIGN KEY (prescription_id) REFERENCES prescription(prescription_id) ON DELETE CASCADE,
                                   CONSTRAINT fk_pres_item_medicine     FOREIGN KEY (medicine_id)     REFERENCES medicine(medicine_id)         ON DELETE RESTRICT,

                                   INDEX idx_pres_item_prescription (prescription_id),
                                   INDEX idx_pres_item_medicine (medicine_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------
-- 6) Vaccination
-- -------------------------
CREATE TABLE vaccination (
                             vaccination_id        BIGINT PRIMARY KEY AUTO_INCREMENT,
                             pet_id                BIGINT NOT NULL,
                             vaccine_name          VARCHAR(120) NOT NULL,
                             given_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             valid_until           DATE,
                             given_by_doctor_id    BIGINT NOT NULL,
                             notes                 TEXT,

                             CONSTRAINT fk_vacc_pet    FOREIGN KEY (pet_id)             REFERENCES pet(pet_id)       ON DELETE CASCADE,
                             CONSTRAINT fk_vacc_doctor FOREIGN KEY (given_by_doctor_id) REFERENCES doctor(doctor_id) ON DELETE RESTRICT,

                             INDEX idx_vacc_pet (pet_id),
                             INDEX idx_vacc_valid_until (valid_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------
-- 7) Inventory: Supplier + Stock Batch
-- -------------------------
CREATE TABLE supplier (
                          supplier_id   BIGINT PRIMARY KEY AUTO_INCREMENT,
                          name          VARCHAR(150) NOT NULL,
                          contact_no    VARCHAR(30),
                          email         VARCHAR(120),
                          address       VARCHAR(255),
                          CONSTRAINT uq_supplier_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE stock_batch (
                             batch_id        BIGINT PRIMARY KEY AUTO_INCREMENT,
                             medicine_id     BIGINT NOT NULL,
                             supplier_id     BIGINT,
                             batch_number    VARCHAR(80) NOT NULL,
                             expiry_date     DATE,
                             purchase_price  DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                             qty_on_hand     INT NOT NULL DEFAULT 0,
                             received_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             CONSTRAINT fk_batch_medicine  FOREIGN KEY (medicine_id) REFERENCES medicine(medicine_id) ON DELETE RESTRICT,
                             CONSTRAINT fk_batch_supplier  FOREIGN KEY (supplier_id) REFERENCES supplier(supplier_id) ON DELETE SET NULL,
                             CONSTRAINT uq_medicine_batch  UNIQUE (medicine_id, batch_number),

                             INDEX idx_batch_medicine (medicine_id),
                             INDEX idx_batch_expiry (expiry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------
-- 8) POS / Billing
-- -------------------------
CREATE TABLE service (
                         service_id      BIGINT PRIMARY KEY AUTO_INCREMENT,
                         name            VARCHAR(120) NOT NULL,
                         default_price   DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                         is_active       TINYINT(1) NOT NULL DEFAULT 1,
                         CONSTRAINT uq_service_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE bill (
                      bill_id              BIGINT PRIMARY KEY AUTO_INCREMENT,
                      owner_id             BIGINT NULL,
                      appointment_id       BIGINT NULL,
                      created_by_staff_id  BIGINT NOT NULL,

                      status               ENUM('DRAFT','ISSUED','VOID','REFUNDED') NOT NULL DEFAULT 'ISSUED',

                      subtotal             DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                      discount             DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                      tax                  DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                      total                DECIMAL(10,2) NOT NULL DEFAULT 0.00,

                      created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      updated_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                      CONSTRAINT fk_bill_owner       FOREIGN KEY (owner_id)       REFERENCES pet_owner(owner_id)      ON DELETE SET NULL,
                      CONSTRAINT fk_bill_appointment FOREIGN KEY (appointment_id) REFERENCES appointment(appointment_id) ON DELETE SET NULL,
                      CONSTRAINT fk_bill_staff       FOREIGN KEY (created_by_staff_id) REFERENCES staff(staff_id)    ON DELETE RESTRICT,

                      INDEX idx_bill_owner (owner_id),
                      INDEX idx_bill_appt (appointment_id),
                      INDEX idx_bill_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE bill_item (
                           bill_item_id   BIGINT PRIMARY KEY AUTO_INCREMENT,
                           bill_id        BIGINT NOT NULL,

                           item_type      ENUM('SERVICE','MEDICINE','CUSTOM') NOT NULL,

                           service_id     BIGINT NULL,
                           medicine_id    BIGINT NULL,
                           batch_id       BIGINT NULL,    -- optional if you want to decrement specific batch

                           description    VARCHAR(255) NOT NULL,
                           qty            INT NOT NULL DEFAULT 1,
                           unit_price     DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                           line_total     DECIMAL(10,2) NOT NULL DEFAULT 0.00,

                           CONSTRAINT fk_bill_item_bill    FOREIGN KEY (bill_id)     REFERENCES bill(bill_id)         ON DELETE CASCADE,
                           CONSTRAINT fk_bill_item_service FOREIGN KEY (service_id)  REFERENCES service(service_id)   ON DELETE SET NULL,
                           CONSTRAINT fk_bill_item_medicine FOREIGN KEY (medicine_id) REFERENCES medicine(medicine_id) ON DELETE SET NULL,
                           CONSTRAINT fk_bill_item_batch   FOREIGN KEY (batch_id)    REFERENCES stock_batch(batch_id) ON DELETE SET NULL,

                           INDEX idx_bill_item_bill (bill_id),
                           INDEX idx_bill_item_type (item_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payment (
                         payment_id       BIGINT PRIMARY KEY AUTO_INCREMENT,
                         bill_id          BIGINT NOT NULL,
                         amount           DECIMAL(10,2) NOT NULL,
                         method           ENUM('CASH','CARD','ONLINE') NOT NULL,
                         status           ENUM('PENDING','PAID','FAILED','REFUNDED') NOT NULL DEFAULT 'PAID',
                         transaction_ref  VARCHAR(120),
                         paid_at          DATETIME,
                         created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT fk_payment_bill FOREIGN KEY (bill_id) REFERENCES bill(bill_id) ON DELETE CASCADE,
                         INDEX idx_payment_bill (bill_id),
                         INDEX idx_payment_paid_at (paid_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------
-- 9) Notifications (per-recipient read status)
-- -------------------------
CREATE TABLE notification (
                              notification_id      BIGINT PRIMARY KEY AUTO_INCREMENT,
                              type                 VARCHAR(60) NOT NULL,    -- e.g., VACCINATION_DUE, APPOINTMENT_CONFIRMED
                              title                VARCHAR(150) NOT NULL,
                              body                 TEXT NOT NULL,
                              created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              created_by_user_id   BIGINT NULL,
                              CONSTRAINT fk_notification_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(user_id) ON DELETE SET NULL,
                              INDEX idx_notification_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_notification (
                                   user_id          BIGINT NOT NULL,
                                   notification_id  BIGINT NOT NULL,
                                   is_read          TINYINT(1) NOT NULL DEFAULT 0,
                                   read_at          DATETIME NULL,
                                   delivered_at     DATETIME NULL,
                                   PRIMARY KEY (user_id, notification_id),
                                   CONSTRAINT fk_user_notification_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                                   CONSTRAINT fk_user_notification_notification FOREIGN KEY (notification_id) REFERENCES notification(notification_id) ON DELETE CASCADE,
                                   INDEX idx_user_notification_is_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
