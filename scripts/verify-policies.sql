-- Verify policies in DB (run after test-policy-crud.sh or after using Policy Management in UI as SUPER_ADMIN)
-- Usage: mysql -u root -p ziminsure < scripts/verify-policies.sql
-- Or:    mysql -u root -p ziminsure -e "SOURCE scripts/verify-policies.sql"

SELECT '=== POLICIES (last 15) ===' AS '';
SELECT id, policy_number, type, status, start_date, end_date, premium, client_id, car_id
FROM policy
ORDER BY id DESC
LIMIT 15;

SELECT '=== Count by status ===' AS '';
SELECT status, COUNT(*) AS cnt FROM policy GROUP BY status;

SELECT '=== Clients with at least one car (for creating policies) ===' AS '';
SELECT u.id AS client_id, u.full_name, u.email, COUNT(c.id) AS car_count
FROM users u
LEFT JOIN car c ON c.client_id = u.id
WHERE u.role = 'CLIENT'
GROUP BY u.id, u.full_name, u.email
ORDER BY u.id DESC
LIMIT 10;
