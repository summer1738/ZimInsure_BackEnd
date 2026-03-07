-- Verify quotations in DB (run after test-quotation-crud.sh or after using Quotation Management in UI as SUPER_ADMIN)
-- Usage: mysql -u root -p ziminsure < scripts/verify-quotations.sql
-- Or:    mysql -u root -p ziminsure -e "SOURCE scripts/verify-quotations.sql"

SELECT '=== QUOTATIONS (last 15) ===' AS '';
SELECT id, quotation_number, policy_type, status, amount, created_date, client_id, agent_id, car_id
FROM quotation
ORDER BY id DESC
LIMIT 15;

SELECT '=== Count by status ===' AS '';
SELECT status, COUNT(*) AS cnt FROM quotation GROUP BY status;

SELECT '=== Clients with cars (for creating quotations) ===' AS '';
SELECT u.id AS client_id, u.full_name, u.email, COUNT(c.id) AS car_count
FROM users u
LEFT JOIN car c ON c.client_id = u.id
WHERE u.role = 'CLIENT'
GROUP BY u.id, u.full_name, u.email
ORDER BY u.id DESC
LIMIT 10;
