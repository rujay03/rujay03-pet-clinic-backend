-- Seed realistic dummy data for doctor dashboard widgets.
-- This migration is safe to run once under Flyway and avoids duplicate inserts where possible.

-- 1) Ensure required roles exist.
INSERT INTO roles (role_name)
SELECT 'DOCTOR'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'DOCTOR');

INSERT INTO roles (role_name)
SELECT 'PETOWNER'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'PETOWNER');

-- 2) Resolve a doctor user. Reuse an existing doctor if present; otherwise create one.
SET @doctor_role_id := (SELECT role_id FROM roles WHERE role_name = 'DOCTOR' LIMIT 1);
SET @petowner_role_id := (SELECT role_id FROM roles WHERE role_name = 'PETOWNER' LIMIT 1);

SET @doctor_user_id := (
    SELECT ur.user_id
    FROM user_roles ur
    WHERE ur.role_id = @doctor_role_id
    LIMIT 1
);

INSERT INTO users (email, password_hash, status, created_at, updated_at)
SELECT 'doctor.demo@petcore.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVE', NOW(), NOW()
WHERE @doctor_user_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM users WHERE email = 'doctor.demo@petcore.local');

SET @doctor_user_id := COALESCE(
    @doctor_user_id,
    (SELECT user_id FROM users WHERE email = 'doctor.demo@petcore.local' LIMIT 1)
);

INSERT INTO user_roles (user_id, role_id)
SELECT @doctor_user_id, @doctor_role_id
WHERE @doctor_user_id IS NOT NULL
  AND @doctor_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles
      WHERE user_id = @doctor_user_id AND role_id = @doctor_role_id
  );

INSERT INTO staff (user_id, full_name, contact_no, hire_date, active, created_at)
SELECT @doctor_user_id, 'Dr. Amina Perera', '+94771234000', DATE_SUB(CURDATE(), INTERVAL 2 YEAR), 1, NOW()
WHERE @doctor_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM staff WHERE user_id = @doctor_user_id);

SET @doctor_staff_id := (SELECT staff_id FROM staff WHERE user_id = @doctor_user_id LIMIT 1);

-- 3) Create pet-owner users.
INSERT INTO users (email, password_hash, status, created_at, updated_at)
SELECT data.email, '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVE', NOW(), NOW()
FROM (
    SELECT 'nimal.silva.owner@petcore.local' AS email
    UNION ALL SELECT 'devika.fernando.owner@petcore.local'
    UNION ALL SELECT 'charith.jay.owner@petcore.local'
    UNION ALL SELECT 'sachini.peris.owner@petcore.local'
    UNION ALL SELECT 'kasun.gomez.owner@petcore.local'
    UNION ALL SELECT 'ravi.kulathunga.owner@petcore.local'
) data
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.email = data.email);

INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, @petowner_role_id
FROM users u
WHERE u.email IN (
    'nimal.silva.owner@petcore.local',
    'devika.fernando.owner@petcore.local',
    'charith.jay.owner@petcore.local',
    'sachini.peris.owner@petcore.local',
    'kasun.gomez.owner@petcore.local',
    'ravi.kulathunga.owner@petcore.local'
)
AND @petowner_role_id IS NOT NULL
AND NOT EXISTS (
    SELECT 1
    FROM user_roles ur
    WHERE ur.user_id = u.user_id AND ur.role_id = @petowner_role_id
);

INSERT INTO pet_owner (user_id, full_name, contact_no, address, created_at)
SELECT u.user_id, d.full_name, d.contact_no, d.address, NOW()
FROM users u
JOIN (
    SELECT 'nimal.silva.owner@petcore.local' AS email, 'Nimal Silva' AS full_name, '+94771230001' AS contact_no, 'Maharagama' AS address
    UNION ALL SELECT 'devika.fernando.owner@petcore.local', 'Devika Fernando', '+94771230002', 'Kottawa'
    UNION ALL SELECT 'charith.jay.owner@petcore.local', 'Charith Jayasuriya', '+94771230003', 'Nugegoda'
    UNION ALL SELECT 'sachini.peris.owner@petcore.local', 'Sachini Peris', '+94771230004', 'Boralesgamuwa'
    UNION ALL SELECT 'kasun.gomez.owner@petcore.local', 'Kasun Gomez', '+94771230005', 'Pannipitiya'
    UNION ALL SELECT 'ravi.kulathunga.owner@petcore.local', 'Ravi Kulathunga', '+94771230006', 'Hokandara'
) d ON d.email = u.email
WHERE NOT EXISTS (SELECT 1 FROM pet_owner po WHERE po.user_id = u.user_id);

-- 4) Create pets for each owner.
INSERT INTO pet (owner_id, name, species, breed, sex, dob, notes, image_url, created_at)
SELECT po.owner_id, p.name, p.species, p.breed, p.sex, p.dob, p.notes, NULL, NOW()
FROM pet_owner po
JOIN users u ON u.user_id = po.user_id
JOIN (
    SELECT 'nimal.silva.owner@petcore.local' AS owner_email, 'Bruno' AS name, 'Dog' AS species, 'Labrador' AS breed, 'MALE' AS sex, DATE_SUB(CURDATE(), INTERVAL 6 YEAR) AS dob, 'Friendly senior dog' AS notes
    UNION ALL SELECT 'devika.fernando.owner@petcore.local', 'Luna', 'Cat', 'Persian', 'FEMALE', DATE_SUB(CURDATE(), INTERVAL 3 YEAR), 'Mild seasonal allergy'
    UNION ALL SELECT 'charith.jay.owner@petcore.local', 'Rocky', 'Dog', 'German Shepherd', 'MALE', DATE_SUB(CURDATE(), INTERVAL 4 YEAR), 'Active, needs joint monitoring'
    UNION ALL SELECT 'sachini.peris.owner@petcore.local', 'Milo', 'Cat', 'Domestic Shorthair', 'MALE', DATE_SUB(CURDATE(), INTERVAL 2 YEAR), 'Weight management plan'
    UNION ALL SELECT 'kasun.gomez.owner@petcore.local', 'Bella', 'Dog', 'Beagle', 'FEMALE', DATE_SUB(CURDATE(), INTERVAL 5 YEAR), 'History of ear infections'
    UNION ALL SELECT 'ravi.kulathunga.owner@petcore.local', 'Coco', 'Dog', 'Poodle', 'FEMALE', DATE_SUB(CURDATE(), INTERVAL 1 YEAR), 'Puppy booster schedule'
) p ON p.owner_email = u.email
WHERE NOT EXISTS (
    SELECT 1
    FROM pet existing
    WHERE existing.owner_id = po.owner_id AND existing.name = p.name
);

-- 5) Seed appointments across this year, this month, and upcoming days.
INSERT INTO appointment (
    owner_id,
    pet_id,
    staff_id,
    appointment_date,
    appointment_time,
    appointment_type,
    status,
    notes,
    created_at,
    updated_at
)
SELECT
    po.owner_id,
    pet.pet_id,
    @doctor_staff_id,
    data.appointment_date,
    data.appointment_time,
    data.appointment_type,
    data.status,
    data.notes,
    NOW(),
    NOW()
FROM (
    SELECT 'nimal.silva.owner@petcore.local' AS owner_email, 'Bruno' AS pet_name, DATE_SUB(CURDATE(), INTERVAL 70 DAY) AS appointment_date, '09:00:00' AS appointment_time, 'General Checkup' AS appointment_type, 'COMPLETED' AS status, 'Routine quarterly visit' AS notes
    UNION ALL SELECT 'devika.fernando.owner@petcore.local', 'Luna', DATE_SUB(CURDATE(), INTERVAL 55 DAY), '11:00:00', 'Dental Cleaning', 'COMPLETED', 'Plaque removal and gum care'
    UNION ALL SELECT 'charith.jay.owner@petcore.local', 'Rocky', DATE_SUB(CURDATE(), INTERVAL 42 DAY), '10:30:00', 'Skin Allergy Consultation', 'COMPLETED', 'Started antihistamine'
    UNION ALL SELECT 'sachini.peris.owner@petcore.local', 'Milo', DATE_SUB(CURDATE(), INTERVAL 35 DAY), '14:15:00', 'Rabies Vaccination', 'COMPLETED', 'Annual booster complete'
    UNION ALL SELECT 'kasun.gomez.owner@petcore.local', 'Bella', DATE_SUB(CURDATE(), INTERVAL 30 DAY), '09:45:00', 'Distemper Vaccination', 'COMPLETED', 'Core vaccine administered'
    UNION ALL SELECT 'ravi.kulathunga.owner@petcore.local', 'Coco', DATE_SUB(CURDATE(), INTERVAL 28 DAY), '15:00:00', 'Parvo Vaccination', 'COMPLETED', 'Puppy vaccination round 2'
    UNION ALL SELECT 'nimal.silva.owner@petcore.local', 'Bruno', DATE_SUB(CURDATE(), INTERVAL 24 DAY), '08:45:00', 'Ear Infection Treatment', 'COMPLETED', 'Topical treatment prescribed'
    UNION ALL SELECT 'devika.fernando.owner@petcore.local', 'Luna', DATE_SUB(CURDATE(), INTERVAL 20 DAY), '13:00:00', 'General Checkup', 'COMPLETED', 'Vitals normal'
    UNION ALL SELECT 'charith.jay.owner@petcore.local', 'Rocky', DATE_SUB(CURDATE(), INTERVAL 18 DAY), '10:00:00', 'Orthopedic Follow-up', 'IN_CONSULTATION', 'Mobility assessment'
    UNION ALL SELECT 'sachini.peris.owner@petcore.local', 'Milo', DATE_SUB(CURDATE(), INTERVAL 15 DAY), '16:30:00', 'General Checkup', 'PENDING', 'Awaiting owner confirmation'
    UNION ALL SELECT 'kasun.gomez.owner@petcore.local', 'Bella', DATE_SUB(CURDATE(), INTERVAL 12 DAY), '12:15:00', 'Dental Cleaning', 'CANCELLED', 'Owner requested reschedule'
    UNION ALL SELECT 'ravi.kulathunga.owner@petcore.local', 'Coco', DATE_SUB(CURDATE(), INTERVAL 10 DAY), '11:45:00', 'Vaccination Booster', 'COMPLETED', 'Final booster complete'
    UNION ALL SELECT 'nimal.silva.owner@petcore.local', 'Bruno', DATE_SUB(CURDATE(), INTERVAL 8 DAY), '09:30:00', 'Arthritis Consultation', 'COMPLETED', 'Pain management updated'
    UNION ALL SELECT 'devika.fernando.owner@petcore.local', 'Luna', DATE_SUB(CURDATE(), INTERVAL 6 DAY), '14:45:00', 'General Checkup', 'COMPLETED', 'Diet recommendations provided'
    UNION ALL SELECT 'charith.jay.owner@petcore.local', 'Rocky', DATE_SUB(CURDATE(), INTERVAL 4 DAY), '08:30:00', 'Rabies Vaccination', 'CONFIRMED', 'Scheduled vaccination'
    UNION ALL SELECT 'sachini.peris.owner@petcore.local', 'Milo', DATE_SUB(CURDATE(), INTERVAL 3 DAY), '10:15:00', 'Ear Infection Treatment', 'PENDING', 'Follow-up requested'
    UNION ALL SELECT 'kasun.gomez.owner@petcore.local', 'Bella', DATE_SUB(CURDATE(), INTERVAL 2 DAY), '15:45:00', 'General Checkup', 'COMPLETED', 'Routine review'
    UNION ALL SELECT 'ravi.kulathunga.owner@petcore.local', 'Coco', DATE_SUB(CURDATE(), INTERVAL 1 DAY), '17:00:00', 'Distemper Vaccination', 'CONFIRMED', 'Pre-travel vaccination'
    UNION ALL SELECT 'nimal.silva.owner@petcore.local', 'Bruno', CURDATE(), '09:15:00', 'General Checkup', 'CONFIRMED', 'Senior wellness exam'
    UNION ALL SELECT 'devika.fernando.owner@petcore.local', 'Luna', DATE_ADD(CURDATE(), INTERVAL 1 DAY), '11:30:00', 'Dermatitis Follow-up', 'CONFIRMED', 'Check skin recovery'
    UNION ALL SELECT 'charith.jay.owner@petcore.local', 'Rocky', DATE_ADD(CURDATE(), INTERVAL 2 DAY), '13:30:00', 'Parvo Vaccination', 'CONFIRMED', 'Booster due'
    UNION ALL SELECT 'sachini.peris.owner@petcore.local', 'Milo', DATE_ADD(CURDATE(), INTERVAL 3 DAY), '10:45:00', 'Dental Cleaning', 'PENDING', 'Needs fasting instructions'
    UNION ALL SELECT 'kasun.gomez.owner@petcore.local', 'Bella', DATE_ADD(CURDATE(), INTERVAL 5 DAY), '16:00:00', 'General Checkup', 'CONFIRMED', 'Weight and ear check'
    UNION ALL SELECT 'ravi.kulathunga.owner@petcore.local', 'Coco', DATE_ADD(CURDATE(), INTERVAL 7 DAY), '09:50:00', 'Vaccination Booster', 'CONFIRMED', 'Annual reinforcement'
) data
JOIN users owner_user ON owner_user.email = data.owner_email
JOIN pet_owner po ON po.user_id = owner_user.user_id
JOIN pet ON pet.owner_id = po.owner_id AND pet.name = data.pet_name
WHERE @doctor_staff_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM appointment existing
      WHERE existing.staff_id = @doctor_staff_id
        AND existing.pet_id = pet.pet_id
        AND existing.appointment_date = data.appointment_date
        AND existing.appointment_time = data.appointment_time
        AND existing.appointment_type = data.appointment_type
  );

