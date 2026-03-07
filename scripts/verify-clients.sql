-- Verify client creation (run after test-client-creation.sh or after creating a client in the UI)
-- Usage: mysql -u root -p ziminsure < scripts/verify-clients.sql
-- Or:    mysql -u root -p ziminsure -e "SOURCE scripts/verify-clients.sql"

SELECT '=== Recent CLIENTS (users with role CLIENT) ===' AS '';
SELECT id, email, full_name, id_number, address, phone, role, created_by
FROM users
WHERE role = 'CLIENT'
ORDER BY id DESC
LIMIT 10;

SELECT '=== Recent CARS with client name ===' AS '';
SELECT c.id AS car_id, c.reg_number, c.make, c.model, c.year, c.client_id, u.full_name AS client_name
FROM car c
JOIN users u ON c.client_id = u.id
ORDER BY c.id DESC
LIMIT 10;

SELECT '=== Super admin (creator) ===' AS '';
SELECT id, email, full_name, role FROM users WHERE role = 'SUPER_ADMIN';
