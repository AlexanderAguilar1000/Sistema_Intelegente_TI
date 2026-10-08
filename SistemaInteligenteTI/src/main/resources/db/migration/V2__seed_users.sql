-- Usuarios de ejemplo para el selector de usuario activo (RF-1, RF-2).
-- Dos supervisores (sin área) y cuatro técnicos repartidos en áreas distintas.
--
-- ON CONFLICT hace la carga idempotente: si el script se vuelve a ejecutar no duplica
-- usuarios (el nombre de usuario es único).

INSERT INTO users (username, full_name, role, area) VALUES
    ('laura.supervisora', 'Laura Gómez',    'SUPERVISOR', NULL),
    ('carlos.supervisor', 'Carlos Ramírez', 'SUPERVISOR', NULL),
    ('andres.apps',       'Andrés Torres',  'TECHNICIAN', 'APPLICATIONS'),
    ('sofia.db',          'Sofía Herrera',  'TECHNICIAN', 'DATABASE'),
    ('diego.infra',       'Diego Morales',  'TECHNICIAN', 'INFRASTRUCTURE'),
    ('valeria.soporte',   'Valeria Castro', 'TECHNICIAN', 'USER_SUPPORT')
ON CONFLICT (username) DO NOTHING;
