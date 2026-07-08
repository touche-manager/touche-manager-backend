-- ============================================================
-- SEED: Happy path manual — Torneo Juvenil Florete Masculino
-- ------------------------------------------------------------
-- Reconstruido con inscripciones incluidas.
-- ============================================================

BEGIN;

-- 1. ROLES
INSERT INTO roles (name) VALUES ('ATHLETE')   ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('REFEREE')   ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ORGANIZER') ON CONFLICT (name) DO NOTHING;

-- 2. USERS (password = "password123")
INSERT INTO users (id, email, password, active, created_at) VALUES
  (1000, 'organizador.juvenil@touche.com', '$2a$10$D/jfkyiLy3zvhG6tYWRAlerCA1he4/2sMvj00p7NjvjyXy.xKmGU2', true, NOW()),
  (1001, 'arbitro1@touche.com',            '$2a$10$D/jfkyiLy3zvhG6tYWRAlerCA1he4/2sMvj00p7NjvjyXy.xKmGU2', true, NOW()),
  (1002, 'arbitro2@touche.com',            '$2a$10$D/jfkyiLy3zvhG6tYWRAlerCA1he4/2sMvj00p7NjvjyXy.xKmGU2', true, NOW()),
  (1003, 'juan.perez@touche.com',          '$2a$10$D/jfkyiLy3zvhG6tYWRAlerCA1he4/2sMvj00p7NjvjyXy.xKmGU2', true, NOW()),
  (1004, 'mateo.gomez@touche.com',         '$2a$10$D/jfkyiLy3zvhG6tYWRAlerCA1he4/2sMvj00p7NjvjyXy.xKmGU2', true, NOW()),
  (1005, 'thiago.rodriguez@touche.com',    '$2a$10$D/jfkyiLy3zvhG6tYWRAlerCA1he4/2sMvj00p7NjvjyXy.xKmGU2', true, NOW()),
  (1006, 'benjamin.lopez@touche.com',      '$2a$10$D/jfkyiLy3zvhG6tYWRAlerCA1he4/2sMvj00p7NjvjyXy.xKmGU2', true, NOW()),
  (1007, 'lucas.martinez@touche.com',      '$2a$10$D/jfkyiLy3zvhG6tYWRAlerCA1he4/2sMvj00p7NjvjyXy.xKmGU2', true, NOW()),
  (1008, 'santino.garcia@touche.com',      '$2a$10$D/jfkyiLy3zvhG6tYWRAlerCA1he4/2sMvj00p7NjvjyXy.xKmGU2', true, NOW()),
  (1009, 'joaquin.sanchez@touche.com',     '$2a$10$D/jfkyiLy3zvhG6tYWRAlerCA1he4/2sMvj00p7NjvjyXy.xKmGU2', true, NOW()),
  (1010, 'bautista.fernandez@touche.com',  '$2a$10$D/jfkyiLy3zvhG6tYWRAlerCA1he4/2sMvj00p7NjvjyXy.xKmGU2', true, NOW()),
  (1011, 'valentino.diaz@touche.com',      '$2a$10$D/jfkyiLy3zvhG6tYWRAlerCA1he4/2sMvj00p7NjvjyXy.xKmGU2', true, NOW()),
  (1012, 'felipe.romero@touche.com',       '$2a$10$D/jfkyiLy3zvhG6tYWRAlerCA1he4/2sMvj00p7NjvjyXy.xKmGU2', true, NOW()),
  (1013, 'tomas.alvarez@touche.com',       '$2a$10$D/jfkyiLy3zvhG6tYWRAlerCA1he4/2sMvj00p7NjvjyXy.xKmGU2', true, NOW()),
  (1014, 'ramiro.torres@touche.com',       '$2a$10$D/jfkyiLy3zvhG6tYWRAlerCA1he4/2sMvj00p7NjvjyXy.xKmGU2', true, NOW())
ON CONFLICT (id) DO NOTHING;

-- Roles asignación
INSERT INTO user_roles (user_id, role_id) SELECT 1000, id FROM roles WHERE name = 'ORGANIZER' ON CONFLICT DO NOTHING;
INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.id IN (1001, 1002) AND r.name = 'REFEREE' ON CONFLICT DO NOTHING;
INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.id IN (1003,1004,1005,1006,1007,1008,1009,1010,1011,1012,1013,1014) AND r.name = 'ATHLETE' ON CONFLICT DO NOTHING;

-- 3. ATLETAS
INSERT INTO athletes (id, user_id, first_name, last_name, dni, birth_date, gender, dominant_hand, club, province) VALUES
  (1100, 1003, 'Juan',      'Pérez',      '48111001', '2009-01-12', 'MALE', 'RIGHT', 'Club Esgrima Buenos Aires', 'Buenos Aires'),
  (1101, 1004, 'Mateo',     'Gómez',      '48222002', '2009-02-23', 'MALE', 'LEFT',  'Club Esgrima Córdoba',      'Córdoba'),
  (1102, 1005, 'Thiago',    'Rodríguez',  '48333003', '2009-03-04', 'MALE', 'RIGHT', 'Club Esgrima Rosario',      'Santa Fe'),
  (1103, 1006, 'Benjamín',  'López',      '48444004', '2009-04-15', 'MALE', 'RIGHT', 'Club Esgrima Mendoza',      'Mendoza'),
  (1104, 1007, 'Lucas',     'Martínez',   '48555005', '2009-05-26', 'MALE', 'LEFT',  'Club Esgrima La Plata',     'Buenos Aires'),
  (1105, 1008, 'Santino',   'García',     '48666006', '2009-06-07', 'MALE', 'RIGHT', 'Club Esgrima Tucumán',      'Tucumán'),
  (1106, 1009, 'Joaquín',   'Sánchez',    '48777007', '2009-07-18', 'MALE', 'RIGHT', 'Club Esgrima Buenos Aires', 'Buenos Aires'),
  (1107, 1010, 'Bautista',  'Fernández',  '48888008', '2009-08-29', 'MALE', 'LEFT',  'Club Esgrima Córdoba',      'Córdoba'),
  (1108, 1011, 'Valentino', 'Díaz',       '48999009', '2009-09-10', 'MALE', 'RIGHT', 'Club Esgrima Rosario',      'Santa Fe'),
  (1109, 1012, 'Felipe',    'Romero',     '49000010', '2009-10-21', 'MALE', 'RIGHT', 'Club Esgrima Mendoza',      'Mendoza'),
  (1110, 1013, 'Tomás',     'Álvarez',    '49111011', '2009-11-02', 'MALE', 'LEFT',  'Club Esgrima La Plata',     'Buenos Aires'),
  (1111, 1014, 'Ramiro',    'Torres',     '49222012', '2009-12-13', 'MALE', 'RIGHT', 'Club Esgrima Tucumán',      'Tucumán')
ON CONFLICT (id) DO NOTHING;

-- 4. TORNEO
INSERT INTO tournaments (id, name, weapon, category, gender, location, date, base_price, created_by_user_id, phase, advancement_rate, is_national) VALUES
  (1200, 'Torneo Juvenil de Florete Masculino 2026', 'FOIL', 'JUNIOR', 'MALE', 'Club Universitario - Buenos Aires', '2026-08-15', 4000.00, 1000, 'ENROLLMENT', 1.00, false)
ON CONFLICT (id) DO NOTHING;

-- 5. INSCRIPCIONES (NUEVO)
INSERT INTO enrollments (athlete_id, tournament_id, enrollment_date, amount, status) VALUES
  (1100, 1200, NOW(), 4000.00, 'PAID'),
  (1101, 1200, NOW(), 4000.00, 'PAID'),
  (1102, 1200, NOW(), 4000.00, 'PAID'),
  (1103, 1200, NOW(), 4000.00, 'PAID'),
  (1104, 1200, NOW(), 4000.00, 'PAID'),
  (1105, 1200, NOW(), 4000.00, 'PAID'),
  (1106, 1200, NOW(), 4000.00, 'PAID'),
  (1107, 1200, NOW(), 4000.00, 'PAID'),
  (1108, 1200, NOW(), 4000.00, 'PAID'),
  (1109, 1200, NOW(), 4000.00, 'PAID'),
  (1110, 1200, NOW(), 4000.00, 'PAID'),
  (1111, 1200, NOW(), 4000.00, 'PAID')
ON CONFLICT (athlete_id, tournament_id) DO NOTHING;

-- 6. DOCUMENTOS (NUEVO)
INSERT INTO athlete_documents (athlete_id, file_key, content_type, document_type, description, upload_date, validation_status) VALUES
  (1100, 'seed/med.pdf', 'application/pdf', 'MEDICAL_CLEARANCE', 'Apto', NOW(), 'PENDING'),
  (1101, 'seed/med.pdf', 'application/pdf', 'MEDICAL_CLEARANCE', 'Apto', NOW(), 'PENDING'),
  (1102, 'seed/med.pdf', 'application/pdf', 'MEDICAL_CLEARANCE', 'Apto', NOW(), 'PENDING'),
  (1103, 'seed/med.pdf', 'application/pdf', 'MEDICAL_CLEARANCE', 'Apto', NOW(), 'PENDING'),
  (1104, 'seed/med.pdf', 'application/pdf', 'MEDICAL_CLEARANCE', 'Apto', NOW(), 'PENDING'),
  (1105, 'seed/med.pdf', 'application/pdf', 'MEDICAL_CLEARANCE', 'Apto', NOW(), 'PENDING'),
  (1106, 'seed/med.pdf', 'application/pdf', 'MEDICAL_CLEARANCE', 'Apto', NOW(), 'PENDING'),
  (1107, 'seed/med.pdf', 'application/pdf', 'MEDICAL_CLEARANCE', 'Apto', NOW(), 'PENDING'),
  (1108, 'seed/med.pdf', 'application/pdf', 'MEDICAL_CLEARANCE', 'Apto', NOW(), 'PENDING'),
  (1109, 'seed/med.pdf', 'application/pdf', 'MEDICAL_CLEARANCE', 'Apto', NOW(), 'PENDING'),
  (1110, 'seed/med.pdf', 'application/pdf', 'MEDICAL_CLEARANCE', 'Apto', NOW(), 'PENDING'),
  (1111, 'seed/med.pdf', 'application/pdf', 'MEDICAL_CLEARANCE', 'Apto', NOW(), 'PENDING')
ON CONFLICT DO NOTHING;

-- 7. SOLICITUDES DE ÁRBITROS
INSERT INTO referee_applications (tournament_id, referee_id, status, applied_at) VALUES
  (1200, 1001, 'ACCEPTED', NOW()),
  (1200, 1002, 'ACCEPTED', NOW())
ON CONFLICT DO NOTHING;

COMMIT;
