-- ============================================================
-- SEED: Happy path manual — Torneo Juvenil Florete Masculino
-- ------------------------------------------------------------
-- Deja la BD lista para recorrer el flujo completo desde cero:
--   Organizador: validar docs -> generar poules -> aceptar
--   arbitros -> asignarlos -> generar bracket -> cerrar.
--   Arbitro: cargar resultados bout a bout.
--
-- Torneo 1200 en fase ENROLLMENT, 12 inscriptos PAID,
-- 2 arbitros con solicitud PENDING, documentos PENDING.
--
-- Todas las contrasenias son: password123
-- Rangos de ID 1000+ para no colisionar con seed_rankings.sql
-- ============================================================

BEGIN;

-- ─────────────────────────────────────────
-- 1. ROLES (si no existen)
-- ─────────────────────────────────────────
INSERT INTO roles (name) VALUES ('ATHLETE')   ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('REFEREE')   ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ORGANIZER') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ADMIN')     ON CONFLICT (name) DO NOTHING;

-- ─────────────────────────────────────────
-- 2. USERS (organizador + 2 arbitros + 12 atletas)
-- password = bcrypt("password123")
-- ─────────────────────────────────────────
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

-- ── Roles de cada usuario ──
INSERT INTO user_roles (user_id, role_id)
SELECT 1000, id FROM roles WHERE name = 'ORGANIZER' ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.id IN (1001, 1002) AND r.name = 'REFEREE'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.id IN (1003,1004,1005,1006,1007,1008,1009,1010,1011,1012,1013,1014) AND r.name = 'ATHLETE'
ON CONFLICT DO NOTHING;

-- ─────────────────────────────────────────
-- 3. ATLETAS (12, todos MALE, JUNIOR — nacidos 2009 => 16/17 al 2026)
-- ─────────────────────────────────────────
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

-- ─────────────────────────────────────────
-- 4. TORNEO (Juvenil / Florete / Masculino, NO nacional, ENROLLMENT)
-- ─────────────────────────────────────────
INSERT INTO tournaments (id, name, weapon, category, gender, location, date, base_price, created_by_user_id, phase, advancement_rate, is_national) VALUES
  (1200, 'Torneo Juvenil de Florete Masculino 2026', 'FOIL', 'JUNIOR', 'MALE',
   'Club Universitario - Buenos Aires', '2026-08-15', 4000.00, 1000, 'ENROLLMENT', 1.00, false)
ON CONFLICT (id) DO NOTHING;

-- ─────────────────────────────────────────
-- 5. DOCUMENTOS DE ATLETAS (2 por atleta, todos PENDING)
--    Apto médico + comprobante de pago.
--    NOTA: la previsualización fallará (no hay archivos en MinIO);
--    aprobar/rechazar sí funciona (solo cambia el estado en BD).
-- ─────────────────────────────────────────
INSERT INTO athlete_documents (id, athlete_id, file_key, content_type, document_type, description, upload_date, validation_status, review_notes) VALUES
  (1500, 1100, 'seed/medical-1100.pdf', 'application/pdf', 'MEDICAL_CLEARANCE', 'Apto médico 2026',        NOW(), 'PENDING', NULL),
  (1501, 1100, 'seed/payment-1100.pdf', 'application/pdf', 'PAYMENT_RECEIPT',   'Comprobante de pago',      NOW(), 'PENDING', NULL),
  (1502, 1101, 'seed/medical-1101.pdf', 'application/pdf', 'MEDICAL_CLEARANCE', 'Apto médico 2026',        NOW(), 'PENDING', NULL),
  (1503, 1101, 'seed/payment-1101.pdf', 'application/pdf', 'PAYMENT_RECEIPT',   'Comprobante de pago',      NOW(), 'PENDING', NULL),
  (1504, 1102, 'seed/medical-1102.pdf', 'application/pdf', 'MEDICAL_CLEARANCE', 'Apto médico 2026',        NOW(), 'PENDING', NULL),
  (1505, 1102, 'seed/payment-1102.pdf', 'application/pdf', 'PAYMENT_RECEIPT',   'Comprobante de pago',      NOW(), 'PENDING', NULL),
  (1506, 1103, 'seed/medical-1103.pdf', 'application/pdf', 'MEDICAL_CLEARANCE', 'Apto médico 2026',        NOW(), 'PENDING', NULL),
  (1507, 1103, 'seed/payment-1103.pdf', 'application/pdf', 'PAYMENT_RECEIPT',   'Comprobante de pago',      NOW(), 'PENDING', NULL),
  (1508, 1104, 'seed/medical-1104.pdf', 'application/pdf', 'MEDICAL_CLEARANCE', 'Apto médico 2026',        NOW(), 'PENDING', NULL),
  (1509, 1104, 'seed/payment-1104.pdf', 'application/pdf', 'PAYMENT_RECEIPT',   'Comprobante de pago',      NOW(), 'PENDING', NULL),
  (1510, 1105, 'seed/medical-1105.pdf', 'application/pdf', 'MEDICAL_CLEARANCE', 'Apto médico 2026',        NOW(), 'PENDING', NULL),
  (1511, 1105, 'seed/payment-1105.pdf', 'application/pdf', 'PAYMENT_RECEIPT',   'Comprobante de pago',      NOW(), 'PENDING', NULL),
  (1512, 1106, 'seed/medical-1106.pdf', 'application/pdf', 'MEDICAL_CLEARANCE', 'Apto médico 2026',        NOW(), 'PENDING', NULL),
  (1513, 1106, 'seed/payment-1106.pdf', 'application/pdf', 'PAYMENT_RECEIPT',   'Comprobante de pago',      NOW(), 'PENDING', NULL),
  (1514, 1107, 'seed/medical-1107.pdf', 'application/pdf', 'MEDICAL_CLEARANCE', 'Apto médico 2026',        NOW(), 'PENDING', NULL),
  (1515, 1107, 'seed/payment-1107.pdf', 'application/pdf', 'PAYMENT_RECEIPT',   'Comprobante de pago',      NOW(), 'PENDING', NULL),
  (1516, 1108, 'seed/medical-1108.pdf', 'application/pdf', 'MEDICAL_CLEARANCE', 'Apto médico 2026',        NOW(), 'PENDING', NULL),
  (1517, 1108, 'seed/payment-1108.pdf', 'application/pdf', 'PAYMENT_RECEIPT',   'Comprobante de pago',      NOW(), 'PENDING', NULL),
  (1518, 1109, 'seed/medical-1109.pdf', 'application/pdf', 'MEDICAL_CLEARANCE', 'Apto médico 2026',        NOW(), 'PENDING', NULL),
  (1519, 1109, 'seed/payment-1109.pdf', 'application/pdf', 'PAYMENT_RECEIPT',   'Comprobante de pago',      NOW(), 'PENDING', NULL),
  (1520, 1110, 'seed/medical-1110.pdf', 'application/pdf', 'MEDICAL_CLEARANCE', 'Apto médico 2026',        NOW(), 'PENDING', NULL),
  (1521, 1110, 'seed/payment-1110.pdf', 'application/pdf', 'PAYMENT_RECEIPT',   'Comprobante de pago',      NOW(), 'PENDING', NULL),
  (1522, 1111, 'seed/medical-1111.pdf', 'application/pdf', 'MEDICAL_CLEARANCE', 'Apto médico 2026',        NOW(), 'PENDING', NULL),
  (1523, 1111, 'seed/payment-1111.pdf', 'application/pdf', 'PAYMENT_RECEIPT',   'Comprobante de pago',      NOW(), 'PENDING', NULL)
ON CONFLICT (id) DO NOTHING;

-- ─────────────────────────────────────────
-- 6. INSCRIPCIONES (12, todas PAID — cuentan para generar poules)
-- ─────────────────────────────────────────
INSERT INTO enrollments (id, athlete_id, tournament_id, enrollment_date, amount, status, payment_id) VALUES
  (1300, 1100, 1200, '2026-07-20 10:00:00', 4000.00, 'PAID', 'PAY-1300'),
  (1301, 1101, 1200, '2026-07-20 10:15:00', 4000.00, 'PAID', 'PAY-1301'),
  (1302, 1102, 1200, '2026-07-20 10:30:00', 4000.00, 'PAID', 'PAY-1302'),
  (1303, 1103, 1200, '2026-07-20 10:45:00', 4000.00, 'PAID', 'PAY-1303'),
  (1304, 1104, 1200, '2026-07-21 09:00:00', 4000.00, 'PAID', 'PAY-1304'),
  (1305, 1105, 1200, '2026-07-21 09:15:00', 4000.00, 'PAID', 'PAY-1305'),
  (1306, 1106, 1200, '2026-07-21 09:30:00', 4000.00, 'PAID', 'PAY-1306'),
  (1307, 1107, 1200, '2026-07-21 09:45:00', 4000.00, 'PAID', 'PAY-1307'),
  (1308, 1108, 1200, '2026-07-22 11:00:00', 4000.00, 'PAID', 'PAY-1308'),
  (1309, 1109, 1200, '2026-07-22 11:15:00', 4000.00, 'PAID', 'PAY-1309'),
  (1310, 1110, 1200, '2026-07-22 11:30:00', 4000.00, 'PAID', 'PAY-1310'),
  (1311, 1111, 1200, '2026-07-22 11:45:00', 4000.00, 'PAID', 'PAY-1311')
ON CONFLICT (id) DO NOTHING;

-- ─────────────────────────────────────────
-- 7. SOLICITUDES DE ARBITRO (2, PENDING — el organizador las acepta)
-- ─────────────────────────────────────────
INSERT INTO referee_applications (id, tournament_id, referee_id, status, applied_at, reviewed_at) VALUES
  (1400, 1200, 1001, 'PENDING', NOW(), NULL),
  (1401, 1200, 1002, 'PENDING', NOW(), NULL)
ON CONFLICT (id) DO NOTHING;

-- ─────────────────────────────────────────
-- 8. Actualizar secuencias (usa MAX(id) por si tambien esta seed_rankings.sql)
-- ─────────────────────────────────────────
SELECT setval(pg_get_serial_sequence('users',                'id'), GREATEST(1014, (SELECT MAX(id) FROM users)));
SELECT setval(pg_get_serial_sequence('athletes',             'id'), GREATEST(1111, (SELECT MAX(id) FROM athletes)));
SELECT setval(pg_get_serial_sequence('tournaments',          'id'), GREATEST(1200, (SELECT MAX(id) FROM tournaments)));
SELECT setval(pg_get_serial_sequence('enrollments',          'id'), GREATEST(1311, (SELECT MAX(id) FROM enrollments)));
SELECT setval(pg_get_serial_sequence('athlete_documents',    'id'), GREATEST(1523, (SELECT MAX(id) FROM athlete_documents)));
SELECT setval(pg_get_serial_sequence('referee_applications', 'id'), GREATEST(1401, (SELECT MAX(id) FROM referee_applications)));

COMMIT;

-- ============================================================
-- CREDENCIALES (todas password123)
--   Organizador : organizador.juvenil@touche.com
--   Árbitro 1   : arbitro1@touche.com
--   Árbitro 2   : arbitro2@touche.com
--   Atleta      : juan.perez@touche.com
-- ============================================================
