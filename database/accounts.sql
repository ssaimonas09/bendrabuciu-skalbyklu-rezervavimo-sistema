-- Veikia ir su jau egzistuojanciu postgres_data tomu.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE users ADD COLUMN IF NOT EXISTS login VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'RESIDENT';

UPDATE users
SET login = 'student_' || dormitory_id || '_' || id
WHERE login IS NULL AND role = 'RESIDENT';

UPDATE users
SET password_hash = crypt('student_demo', gen_salt('bf'))
WHERE password_hash IS NULL AND role = 'RESIDENT';

INSERT INTO users (dormitory_id, id, name, login, password_hash, role)
SELECT id, -1, 'Administratorius ' || name, 'admin_' || id,
       crypt('admin_demo', gen_salt('bf')), 'ADMIN'
FROM dormitories
ON CONFLICT (dormitory_id, id) DO NOTHING;

CREATE UNIQUE INDEX IF NOT EXISTS users_login_unique ON users(login);

-- Seka saugiai paskiria nauja ID ir jau naudotoje bazeje.
CREATE SEQUENCE IF NOT EXISTS user_id_seq START WITH 102;
SELECT setval('user_id_seq', GREATEST((SELECT COALESCE(MAX(id), 101) FROM users), 101));

ALTER TABLE users ALTER COLUMN login SET NOT NULL;
ALTER TABLE users ALTER COLUMN password_hash SET NOT NULL;
