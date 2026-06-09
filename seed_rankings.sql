-- ============================================================
-- SEED: Torneos terminados para testear sección de Rankings
-- Actualizado: columna is_national + torneos extra para RSP
-- ============================================================

BEGIN;

-- ─────────────────────────────────────────
-- 1. ROLES (si no existen)
-- ─────────────────────────────────────────
INSERT INTO roles (name) VALUES ('ATHLETE') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('REFEREE') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ORGANIZER') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ADMIN') ON CONFLICT (name) DO NOTHING;

-- ─────────────────────────────────────────
-- 2. USERS (organizador + atletas)
-- password = bcrypt("password123")
-- ─────────────────────────────────────────
INSERT INTO users (id, email, password, active, created_at) VALUES
  (100, 'organizador@touche.com', '$2a$10$7QJ8vHzUFfomEbXLOKTyMu8Q4A1bKsJtPROkR5vNmY3P.wFqb1VJy', true, NOW()),
  (101, 'gomez@touche.com',       '$2a$10$7QJ8vHzUFfomEbXLOKTyMu8Q4A1bKsJtPROkR5vNmY3P.wFqb1VJy', true, NOW()),
  (102, 'rodriguez@touche.com',   '$2a$10$7QJ8vHzUFfomEbXLOKTyMu8Q4A1bKsJtPROkR5vNmY3P.wFqb1VJy', true, NOW()),
  (103, 'martinez@touche.com',    '$2a$10$7QJ8vHzUFfomEbXLOKTyMu8Q4A1bKsJtPROkR5vNmY3P.wFqb1VJy', true, NOW()),
  (104, 'lopez@touche.com',       '$2a$10$7QJ8vHzUFfomEbXLOKTyMu8Q4A1bKsJtPROkR5vNmY3P.wFqb1VJy', true, NOW()),
  (105, 'garcia@touche.com',      '$2a$10$7QJ8vHzUFfomEbXLOKTyMu8Q4A1bKsJtPROkR5vNmY3P.wFqb1VJy', true, NOW()),
  (106, 'sanchez@touche.com',     '$2a$10$7QJ8vHzUFfomEbXLOKTyMu8Q4A1bKsJtPROkR5vNmY3P.wFqb1VJy', true, NOW()),
  (107, 'fernandez@touche.com',   '$2a$10$7QJ8vHzUFfomEbXLOKTyMu8Q4A1bKsJtPROkR5vNmY3P.wFqb1VJy', true, NOW()),
  (108, 'perez@touche.com',       '$2a$10$7QJ8vHzUFfomEbXLOKTyMu8Q4A1bKsJtPROkR5vNmY3P.wFqb1VJy', true, NOW()),
  (110, 'alvarez@touche.com',     '$2a$10$7QJ8vHzUFfomEbXLOKTyMu8Q4A1bKsJtPROkR5vNmY3P.wFqb1VJy', true, NOW()),
  (111, 'torres@touche.com',      '$2a$10$7QJ8vHzUFfomEbXLOKTyMu8Q4A1bKsJtPROkR5vNmY3P.wFqb1VJy', true, NOW()),
  (112, 'reyes@touche.com',       '$2a$10$7QJ8vHzUFfomEbXLOKTyMu8Q4A1bKsJtPROkR5vNmY3P.wFqb1VJy', true, NOW()),
  (113, 'flores@touche.com',      '$2a$10$7QJ8vHzUFfomEbXLOKTyMu8Q4A1bKsJtPROkR5vNmY3P.wFqb1VJy', true, NOW())
ON CONFLICT (id) DO NOTHING;

-- Roles
INSERT INTO user_roles (user_id, role_id)
SELECT 100, id FROM roles WHERE name = 'ORGANIZER' ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.id IN (101,102,103,104,105,106,107,108,110,111,112,113) AND r.name = 'ATHLETE'
ON CONFLICT DO NOTHING;

-- ─────────────────────────────────────────
-- 3. ATLETAS
-- ─────────────────────────────────────────
INSERT INTO athletes (id, user_id, first_name, last_name, dni, birth_date, gender, dominant_hand, club, province) VALUES
  (201, 101, 'Carlos',    'Gómez',      '32111001', '1995-03-15', 'MALE', 'RIGHT', 'Club Esgrima Buenos Aires', 'Buenos Aires'),
  (202, 102, 'Matías',    'Rodríguez',  '33222002', '1997-07-22', 'MALE', 'RIGHT', 'Club Esgrima Córdoba',      'Córdoba'),
  (203, 103, 'Santiago',  'Martínez',   '34333003', '1996-11-08', 'MALE', 'LEFT',  'Club Esgrima Rosario',      'Santa Fe'),
  (204, 104, 'Nicolás',   'López',      '35444004', '1994-05-30', 'MALE', 'RIGHT', 'Club Esgrima Mendoza',      'Mendoza'),
  (205, 105, 'Facundo',   'García',     '36555005', '1998-02-14', 'MALE', 'RIGHT', 'Club Esgrima Buenos Aires', 'Buenos Aires'),
  (206, 106, 'Agustín',   'Sánchez',    '37666006', '1999-09-25', 'MALE', 'RIGHT', 'Club Esgrima La Plata',     'Buenos Aires'),
  (207, 107, 'Ignacio',   'Fernández',  '38777007', '1993-12-01', 'MALE', 'LEFT',  'Club Esgrima Tucumán',      'Tucumán'),
  (208, 108, 'Tomás',     'Pérez',      '39888008', '2000-06-18', 'MALE', 'RIGHT', 'Club Esgrima Córdoba',      'Córdoba'),
  (210, 110, 'Valentina', 'Álvarez',    '40111010', '2008-04-12', 'FEMALE', 'RIGHT', 'Club Esgrima Buenos Aires', 'Buenos Aires'),
  (211, 111, 'Lucía',     'Torres',     '40222011', '2007-09-03', 'FEMALE', 'RIGHT', 'Club Esgrima Rosario',    'Santa Fe'),
  (212, 112, 'Sofía',     'Reyes',      '40333012', '2008-01-25', 'FEMALE', 'LEFT',  'Club Esgrima Córdoba',   'Córdoba'),
  (213, 113, 'Emma',      'Flores',     '40444013', '2007-06-07', 'FEMALE', 'RIGHT', 'Club Esgrima Mendoza',   'Mendoza')
ON CONFLICT (id) DO NOTHING;

-- ─────────────────────────────────────────
-- 4. TORNEOS  ← columna is_national agregada
--
-- 301: Campeonato Nacional Florete Senior M  → is_national = true  (coef ×1.2)
-- 302: Torneo Regional Espada Cadet F        → is_national = false
-- 303: Open Florete Senior M - Agosto        → is_national = false
-- 304: Interprovincial Florete Senior M      → is_national = false
--      (4 torneos para la misma disciplina → el RSP descarta el peor)
-- ─────────────────────────────────────────
INSERT INTO tournaments (id, name, weapon, category, gender, location, date, base_price, created_by_user_id, phase, advancement_rate, is_national) VALUES
  (301, 'Campeonato Nacional de Florete Senior Masculino 2025', 'FOIL', 'SENIOR', 'MALE',
   'Estadio Cubierto CENARD - Buenos Aires', '2025-11-15', 5000.00, 100, 'FINISHED', 1.00, true),
  (302, 'Torneo Regional de Espada Cadet Femenino 2025', 'EPEE', 'CADET', 'FEMALE',
   'Polideportivo Municipal Córdoba', '2025-09-20', 3000.00, 100, 'FINISHED', 1.00, false),
  (303, 'Open de Florete Senior Masculino - Buenos Aires 2025', 'FOIL', 'SENIOR', 'MALE',
   'Club Universitario Buenos Aires', '2025-08-10', 3000.00, 100, 'FINISHED', 1.00, false),
  (304, 'Torneo Interprovincial de Florete Senior Masculino 2025', 'FOIL', 'SENIOR', 'MALE',
   'Polideportivo Córdoba', '2025-06-21', 2500.00, 100, 'FINISHED', 1.00, false)
ON CONFLICT (id) DO NOTHING;

-- ─────────────────────────────────────────
-- 5. INSCRIPCIONES (todas PAID)
-- ─────────────────────────────────────────
INSERT INTO enrollments (id, athlete_id, tournament_id, enrollment_date, amount, status, payment_id) VALUES
  -- Torneo 301 (Nacional Florete Senior M)
  (401, 201, 301, '2025-10-01 10:00:00', 5000.00, 'PAID', 'PAY-001'),
  (402, 202, 301, '2025-10-01 10:30:00', 5000.00, 'PAID', 'PAY-002'),
  (403, 203, 301, '2025-10-02 09:00:00', 5000.00, 'PAID', 'PAY-003'),
  (404, 204, 301, '2025-10-02 11:00:00', 5000.00, 'PAID', 'PAY-004'),
  (405, 205, 301, '2025-10-03 14:00:00', 5000.00, 'PAID', 'PAY-005'),
  (406, 206, 301, '2025-10-03 15:00:00', 5000.00, 'PAID', 'PAY-006'),
  (407, 207, 301, '2025-10-04 08:00:00', 5000.00, 'PAID', 'PAY-007'),
  (408, 208, 301, '2025-10-04 09:30:00', 5000.00, 'PAID', 'PAY-008'),
  -- Torneo 302 (Regional Espada Cadet F)
  (410, 210, 302, '2025-08-15 10:00:00', 3000.00, 'PAID', 'PAY-010'),
  (411, 211, 302, '2025-08-15 10:30:00', 3000.00, 'PAID', 'PAY-011'),
  (412, 212, 302, '2025-08-16 09:00:00', 3000.00, 'PAID', 'PAY-012'),
  (413, 213, 302, '2025-08-16 11:00:00', 3000.00, 'PAID', 'PAY-013'),
  -- Torneo 303 (Open Florete Senior M)
  (420, 201, 303, '2025-07-01 10:00:00', 3000.00, 'PAID', 'PAY-020'),
  (421, 202, 303, '2025-07-01 10:30:00', 3000.00, 'PAID', 'PAY-021'),
  (422, 203, 303, '2025-07-02 09:00:00', 3000.00, 'PAID', 'PAY-022'),
  (423, 204, 303, '2025-07-02 11:00:00', 3000.00, 'PAID', 'PAY-023'),
  (424, 205, 303, '2025-07-03 14:00:00', 3000.00, 'PAID', 'PAY-024'),
  (425, 206, 303, '2025-07-03 15:00:00', 3000.00, 'PAID', 'PAY-025'),
  (426, 207, 303, '2025-07-04 08:00:00', 3000.00, 'PAID', 'PAY-026'),
  (427, 208, 303, '2025-07-04 09:30:00', 3000.00, 'PAID', 'PAY-027'),
  -- Torneo 304 (Interprovincial Florete Senior M)
  (430, 201, 304, '2025-05-01 10:00:00', 2500.00, 'PAID', 'PAY-030'),
  (431, 202, 304, '2025-05-01 10:30:00', 2500.00, 'PAID', 'PAY-031'),
  (432, 203, 304, '2025-05-02 09:00:00', 2500.00, 'PAID', 'PAY-032'),
  (433, 204, 304, '2025-05-02 11:00:00', 2500.00, 'PAID', 'PAY-033'),
  (434, 205, 304, '2025-05-03 14:00:00', 2500.00, 'PAID', 'PAY-034'),
  (435, 206, 304, '2025-05-03 15:00:00', 2500.00, 'PAID', 'PAY-035'),
  (436, 207, 304, '2025-05-04 08:00:00', 2500.00, 'PAID', 'PAY-036'),
  (437, 208, 304, '2025-05-04 09:30:00', 2500.00, 'PAID', 'PAY-037')
ON CONFLICT (id) DO NOTHING;

-- ─────────────────────────────────────────
-- 6. POULES
-- ─────────────────────────────────────────
INSERT INTO poules (id, tournament_id, number, status) VALUES
  (501, 301, 1, 'FINISHED'),
  (502, 301, 2, 'FINISHED'),
  (510, 302, 1, 'FINISHED'),
  (520, 303, 1, 'FINISHED'),
  (521, 303, 2, 'FINISHED'),
  (530, 304, 1, 'FINISHED'),
  (531, 304, 2, 'FINISHED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO poule_athletes (poule_id, athlete_id) VALUES
  (501, 201), (501, 202), (501, 203), (501, 204),
  (502, 205), (502, 206), (502, 207), (502, 208),
  (510, 210), (510, 211), (510, 212), (510, 213),
  (520, 201), (520, 202), (520, 203), (520, 204),
  (521, 205), (521, 206), (521, 207), (521, 208),
  (530, 201), (530, 202), (530, 203), (530, 204),
  (531, 205), (531, 206), (531, 207), (531, 208)
ON CONFLICT DO NOTHING;

-- ─────────────────────────────────────────
-- 7. BOUTS DE POULE
-- ─────────────────────────────────────────

-- ── Torneo 301 POULE 1 (201,202,203,204) ──
INSERT INTO bouts (id, tournament_id, poule_id, athlete_left_id, athlete_right_id, format, score_left, score_right, status, current_period, elapsed_seconds, started_at, finished_at, winner_id, bout_order) VALUES
  (601, 301, 501, 201, 202, 'POULE', 5, 3, 'FINISHED', 1, 180, '2025-11-15 09:00:00', '2025-11-15 09:03:00', 201, 1),
  (602, 301, 501, 203, 204, 'POULE', 5, 2, 'FINISHED', 1, 180, '2025-11-15 09:05:00', '2025-11-15 09:08:00', 203, 2),
  (603, 301, 501, 201, 203, 'POULE', 5, 4, 'FINISHED', 1, 180, '2025-11-15 09:10:00', '2025-11-15 09:13:00', 201, 3),
  (604, 301, 501, 202, 204, 'POULE', 5, 1, 'FINISHED', 1, 180, '2025-11-15 09:15:00', '2025-11-15 09:18:00', 202, 4),
  (605, 301, 501, 204, 201, 'POULE', 3, 5, 'FINISHED', 1, 180, '2025-11-15 09:20:00', '2025-11-15 09:23:00', 201, 5),
  (606, 301, 501, 202, 203, 'POULE', 4, 5, 'FINISHED', 1, 180, '2025-11-15 09:25:00', '2025-11-15 09:28:00', 203, 6)
ON CONFLICT (id) DO NOTHING;

-- ── Torneo 301 POULE 2 (205,206,207,208) ──
INSERT INTO bouts (id, tournament_id, poule_id, athlete_left_id, athlete_right_id, format, score_left, score_right, status, current_period, elapsed_seconds, started_at, finished_at, winner_id, bout_order) VALUES
  (611, 301, 502, 205, 206, 'POULE', 5, 2, 'FINISHED', 1, 180, '2025-11-15 09:00:00', '2025-11-15 09:03:00', 205, 1),
  (612, 301, 502, 207, 208, 'POULE', 5, 3, 'FINISHED', 1, 180, '2025-11-15 09:05:00', '2025-11-15 09:08:00', 207, 2),
  (613, 301, 502, 205, 207, 'POULE', 5, 4, 'FINISHED', 1, 180, '2025-11-15 09:10:00', '2025-11-15 09:13:00', 205, 3),
  (614, 301, 502, 206, 208, 'POULE', 3, 5, 'FINISHED', 1, 180, '2025-11-15 09:15:00', '2025-11-15 09:18:00', 208, 4),
  (615, 301, 502, 208, 205, 'POULE', 4, 5, 'FINISHED', 1, 180, '2025-11-15 09:20:00', '2025-11-15 09:23:00', 205, 5),
  (616, 301, 502, 206, 207, 'POULE', 2, 5, 'FINISHED', 1, 180, '2025-11-15 09:25:00', '2025-11-15 09:28:00', 207, 6)
ON CONFLICT (id) DO NOTHING;

-- ── Torneo 302 POULE única (210,211,212,213) ──
INSERT INTO bouts (id, tournament_id, poule_id, athlete_left_id, athlete_right_id, format, score_left, score_right, status, current_period, elapsed_seconds, started_at, finished_at, winner_id, bout_order) VALUES
  (650, 302, 510, 210, 211, 'POULE', 2, 5, 'FINISHED', 1, 180, '2025-09-20 09:00:00', '2025-09-20 09:03:00', 211, 1),
  (651, 302, 510, 212, 213, 'POULE', 5, 3, 'FINISHED', 1, 180, '2025-09-20 09:05:00', '2025-09-20 09:08:00', 212, 2),
  (652, 302, 510, 210, 212, 'POULE', 3, 5, 'FINISHED', 1, 180, '2025-09-20 09:10:00', '2025-09-20 09:13:00', 212, 3),
  (653, 302, 510, 211, 213, 'POULE', 5, 1, 'FINISHED', 1, 180, '2025-09-20 09:15:00', '2025-09-20 09:18:00', 211, 4),
  (654, 302, 510, 213, 210, 'POULE', 5, 4, 'FINISHED', 1, 180, '2025-09-20 09:20:00', '2025-09-20 09:23:00', 213, 5),
  (655, 302, 510, 211, 212, 'POULE', 5, 4, 'FINISHED', 1, 180, '2025-09-20 09:25:00', '2025-09-20 09:28:00', 211, 6)
ON CONFLICT (id) DO NOTHING;

-- ── Torneo 303 POULE 1 (201,202,203,204) ──
INSERT INTO bouts (id, tournament_id, poule_id, athlete_left_id, athlete_right_id, format, score_left, score_right, status, current_period, elapsed_seconds, started_at, finished_at, winner_id, bout_order) VALUES
  (801, 303, 520, 201, 202, 'POULE', 5, 2, 'FINISHED', 1, 180, '2025-08-10 09:00:00', '2025-08-10 09:03:00', 201, 1),
  (802, 303, 520, 203, 204, 'POULE', 4, 5, 'FINISHED', 1, 180, '2025-08-10 09:05:00', '2025-08-10 09:08:00', 204, 2),
  (803, 303, 520, 201, 203, 'POULE', 5, 1, 'FINISHED', 1, 180, '2025-08-10 09:10:00', '2025-08-10 09:13:00', 201, 3),
  (804, 303, 520, 202, 204, 'POULE', 3, 5, 'FINISHED', 1, 180, '2025-08-10 09:15:00', '2025-08-10 09:18:00', 204, 4),
  (805, 303, 520, 204, 201, 'POULE', 2, 5, 'FINISHED', 1, 180, '2025-08-10 09:20:00', '2025-08-10 09:23:00', 201, 5),
  (806, 303, 520, 202, 203, 'POULE', 5, 3, 'FINISHED', 1, 180, '2025-08-10 09:25:00', '2025-08-10 09:28:00', 202, 6)
ON CONFLICT (id) DO NOTHING;

-- ── Torneo 303 POULE 2 (205,206,207,208) ──
INSERT INTO bouts (id, tournament_id, poule_id, athlete_left_id, athlete_right_id, format, score_left, score_right, status, current_period, elapsed_seconds, started_at, finished_at, winner_id, bout_order) VALUES
  (811, 303, 521, 205, 206, 'POULE', 5, 3, 'FINISHED', 1, 180, '2025-08-10 09:00:00', '2025-08-10 09:03:00', 205, 1),
  (812, 303, 521, 207, 208, 'POULE', 4, 5, 'FINISHED', 1, 180, '2025-08-10 09:05:00', '2025-08-10 09:08:00', 208, 2),
  (813, 303, 521, 205, 207, 'POULE', 5, 2, 'FINISHED', 1, 180, '2025-08-10 09:10:00', '2025-08-10 09:13:00', 205, 3),
  (814, 303, 521, 206, 208, 'POULE', 5, 4, 'FINISHED', 1, 180, '2025-08-10 09:15:00', '2025-08-10 09:18:00', 206, 4),
  (815, 303, 521, 208, 205, 'POULE', 3, 5, 'FINISHED', 1, 180, '2025-08-10 09:20:00', '2025-08-10 09:23:00', 205, 5),
  (816, 303, 521, 206, 207, 'POULE', 2, 5, 'FINISHED', 1, 180, '2025-08-10 09:25:00', '2025-08-10 09:28:00', 207, 6)
ON CONFLICT (id) DO NOTHING;

-- ── Torneo 304 POULE 1 (201,202,203,204) ──
INSERT INTO bouts (id, tournament_id, poule_id, athlete_left_id, athlete_right_id, format, score_left, score_right, status, current_period, elapsed_seconds, started_at, finished_at, winner_id, bout_order) VALUES
  (901, 304, 530, 201, 202, 'POULE', 3, 5, 'FINISHED', 1, 180, '2025-06-21 09:00:00', '2025-06-21 09:03:00', 202, 1),
  (902, 304, 530, 203, 204, 'POULE', 5, 4, 'FINISHED', 1, 180, '2025-06-21 09:05:00', '2025-06-21 09:08:00', 203, 2),
  (903, 304, 530, 201, 203, 'POULE', 4, 5, 'FINISHED', 1, 180, '2025-06-21 09:10:00', '2025-06-21 09:13:00', 203, 3),
  (904, 304, 530, 202, 204, 'POULE', 5, 2, 'FINISHED', 1, 180, '2025-06-21 09:15:00', '2025-06-21 09:18:00', 202, 4),
  (905, 304, 530, 204, 201, 'POULE', 5, 3, 'FINISHED', 1, 180, '2025-06-21 09:20:00', '2025-06-21 09:23:00', 204, 5),
  (906, 304, 530, 202, 203, 'POULE', 4, 5, 'FINISHED', 1, 180, '2025-06-21 09:25:00', '2025-06-21 09:28:00', 203, 6)
ON CONFLICT (id) DO NOTHING;

-- ── Torneo 304 POULE 2 (205,206,207,208) ──
INSERT INTO bouts (id, tournament_id, poule_id, athlete_left_id, athlete_right_id, format, score_left, score_right, status, current_period, elapsed_seconds, started_at, finished_at, winner_id, bout_order) VALUES
  (911, 304, 531, 205, 206, 'POULE', 2, 5, 'FINISHED', 1, 180, '2025-06-21 09:00:00', '2025-06-21 09:03:00', 206, 1),
  (912, 304, 531, 207, 208, 'POULE', 5, 2, 'FINISHED', 1, 180, '2025-06-21 09:05:00', '2025-06-21 09:08:00', 207, 2),
  (913, 304, 531, 205, 207, 'POULE', 3, 5, 'FINISHED', 1, 180, '2025-06-21 09:10:00', '2025-06-21 09:13:00', 207, 3),
  (914, 304, 531, 206, 208, 'POULE', 5, 1, 'FINISHED', 1, 180, '2025-06-21 09:15:00', '2025-06-21 09:18:00', 206, 4),
  (915, 304, 531, 208, 205, 'POULE', 3, 5, 'FINISHED', 1, 180, '2025-06-21 09:20:00', '2025-06-21 09:23:00', 205, 5),
  (916, 304, 531, 206, 207, 'POULE', 4, 5, 'FINISHED', 1, 180, '2025-06-21 09:25:00', '2025-06-21 09:28:00', 207, 6)
ON CONFLICT (id) DO NOTHING;

-- ─────────────────────────────────────────
-- 8. BOUTS DE ELIMINACIÓN
-- ─────────────────────────────────────────

-- ── Torneo 301 (Nacional ×1.2) ──
-- SEMIFINAL 1: Gómez(201) vs Fernández(207) → Gómez gana 15-10
INSERT INTO bouts (id, tournament_id, poule_id, athlete_left_id, athlete_right_id, format, score_left, score_right, status, current_period, elapsed_seconds, started_at, finished_at, winner_id, elimination_round, bracket_position)
VALUES (701, 301, NULL, 201, 207, 'ELIMINATION', 15, 10, 'FINISHED', 3, 540, '2025-11-15 11:00:00', '2025-11-15 11:09:00', 201, 'SEMIFINAL', 1)
ON CONFLICT (id) DO NOTHING;

-- SEMIFINAL 2: García(205) vs Martínez(203) → García gana 15-12
INSERT INTO bouts (id, tournament_id, poule_id, athlete_left_id, athlete_right_id, format, score_left, score_right, status, current_period, elapsed_seconds, started_at, finished_at, winner_id, elimination_round, bracket_position)
VALUES (702, 301, NULL, 205, 203, 'ELIMINATION', 15, 12, 'FINISHED', 3, 540, '2025-11-15 11:10:00', '2025-11-15 11:19:00', 205, 'SEMIFINAL', 2)
ON CONFLICT (id) DO NOTHING;

-- FINAL: Gómez(201) vs García(205) → García gana 15-13
-- Resultado: 1°García(32×1.2=38.4), 2°Gómez(26×1.2=31.2), 3°Martínez(22×1.2=26.4), 3°Fernández(22×1.2=26.4)
INSERT INTO bouts (id, tournament_id, poule_id, athlete_left_id, athlete_right_id, format, score_left, score_right, status, current_period, elapsed_seconds, started_at, finished_at, winner_id, elimination_round, bracket_position)
VALUES (703, 301, NULL, 201, 205, 'ELIMINATION', 13, 15, 'FINISHED', 3, 540, '2025-11-15 12:00:00', '2025-11-15 12:09:00', 205, 'FINAL', 1)
ON CONFLICT (id) DO NOTHING;

-- ── Torneo 302 ──
-- FINAL: Torres(211) vs Reyes(212) → Torres gana 15-8
INSERT INTO bouts (id, tournament_id, poule_id, athlete_left_id, athlete_right_id, format, score_left, score_right, status, current_period, elapsed_seconds, started_at, finished_at, winner_id, elimination_round, bracket_position)
VALUES (710, 302, NULL, 211, 212, 'ELIMINATION', 15, 8, 'FINISHED', 3, 480, '2025-09-20 11:00:00', '2025-09-20 11:08:00', 211, 'FINAL', 1)
ON CONFLICT (id) DO NOTHING;

-- ── Torneo 303 (Open Agosto) ──
-- SEMIFINAL 1: Gómez(201) vs Rodríguez(202) → Gómez gana 15-11
INSERT INTO bouts (id, tournament_id, poule_id, athlete_left_id, athlete_right_id, format, score_left, score_right, status, current_period, elapsed_seconds, started_at, finished_at, winner_id, elimination_round, bracket_position)
VALUES (721, 303, NULL, 201, 202, 'ELIMINATION', 15, 11, 'FINISHED', 3, 540, '2025-08-10 11:00:00', '2025-08-10 11:09:00', 201, 'SEMIFINAL', 1)
ON CONFLICT (id) DO NOTHING;

-- SEMIFINAL 2: García(205) vs Fernández(207) → García gana 15-9
INSERT INTO bouts (id, tournament_id, poule_id, athlete_left_id, athlete_right_id, format, score_left, score_right, status, current_period, elapsed_seconds, started_at, finished_at, winner_id, elimination_round, bracket_position)
VALUES (722, 303, NULL, 205, 207, 'ELIMINATION', 15, 9, 'FINISHED', 3, 540, '2025-08-10 11:10:00', '2025-08-10 11:19:00', 205, 'SEMIFINAL', 2)
ON CONFLICT (id) DO NOTHING;

-- FINAL: Gómez(201) vs García(205) → Gómez gana 15-14
-- Resultado: 1°Gómez(32), 2°García(26), 3°Rodríguez(22), 3°Fernández(22)
INSERT INTO bouts (id, tournament_id, poule_id, athlete_left_id, athlete_right_id, format, score_left, score_right, status, current_period, elapsed_seconds, started_at, finished_at, winner_id, elimination_round, bracket_position)
VALUES (723, 303, NULL, 201, 205, 'ELIMINATION', 15, 14, 'FINISHED', 3, 540, '2025-08-10 12:00:00', '2025-08-10 12:09:00', 201, 'FINAL', 1)
ON CONFLICT (id) DO NOTHING;

-- ── Torneo 304 (Interprovincial Junio) ──
-- SEMIFINAL 1: Martínez(203) vs López(204) → Martínez gana 15-8
INSERT INTO bouts (id, tournament_id, poule_id, athlete_left_id, athlete_right_id, format, score_left, score_right, status, current_period, elapsed_seconds, started_at, finished_at, winner_id, elimination_round, bracket_position)
VALUES (731, 304, NULL, 203, 204, 'ELIMINATION', 15, 8, 'FINISHED', 3, 540, '2025-06-21 11:00:00', '2025-06-21 11:09:00', 203, 'SEMIFINAL', 1)
ON CONFLICT (id) DO NOTHING;

-- SEMIFINAL 2: Fernández(207) vs Sánchez(206) → Fernández gana 15-12
INSERT INTO bouts (id, tournament_id, poule_id, athlete_left_id, athlete_right_id, format, score_left, score_right, status, current_period, elapsed_seconds, started_at, finished_at, winner_id, elimination_round, bracket_position)
VALUES (732, 304, NULL, 207, 206, 'ELIMINATION', 15, 12, 'FINISHED', 3, 540, '2025-06-21 11:10:00', '2025-06-21 11:19:00', 207, 'SEMIFINAL', 2)
ON CONFLICT (id) DO NOTHING;

-- FINAL: Martínez(203) vs Fernández(207) → Fernández gana 15-10
-- Resultado: 1°Fernández(32), 2°Martínez(26), 3°López(22), 3°Sánchez(22)
INSERT INTO bouts (id, tournament_id, poule_id, athlete_left_id, athlete_right_id, format, score_left, score_right, status, current_period, elapsed_seconds, started_at, finished_at, winner_id, elimination_round, bracket_position)
VALUES (733, 304, NULL, 203, 207, 'ELIMINATION', 10, 15, 'FINISHED', 3, 540, '2025-06-21 12:00:00', '2025-06-21 12:09:00', 207, 'FINAL', 1)
ON CONFLICT (id) DO NOTHING;

-- ─────────────────────────────────────────
-- 9. Actualizar secuencias
-- ─────────────────────────────────────────
SELECT setval(pg_get_serial_sequence('users',       'id'), GREATEST(200, (SELECT MAX(id) FROM users)));
SELECT setval(pg_get_serial_sequence('athletes',    'id'), GREATEST(300, (SELECT MAX(id) FROM athletes)));
SELECT setval(pg_get_serial_sequence('tournaments', 'id'), GREATEST(400, (SELECT MAX(id) FROM tournaments)));
SELECT setval(pg_get_serial_sequence('enrollments', 'id'), GREATEST(500, (SELECT MAX(id) FROM enrollments)));
SELECT setval(pg_get_serial_sequence('poules',      'id'), GREATEST(700, (SELECT MAX(id) FROM poules)));
SELECT setval(pg_get_serial_sequence('bouts',       'id'), GREATEST(1000, (SELECT MAX(id) FROM bouts)));

COMMIT;
