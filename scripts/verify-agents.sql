-- Verify agents in DB (run after test-agent-crud.sh or after using Agent Management in UI as SUPER_ADMIN)
-- Usage: mysql -u root -p ziminsure < scripts/verify-agents.sql
-- Or:    mysql -u root -p ziminsure -e "SOURCE scripts/verify-agents.sql"

SELECT '=== AGENTS (users with role AGENT) ===' AS '';
SELECT id, email, full_name, id_number, address, phone, status, role
FROM users
WHERE role = 'AGENT'
ORDER BY id DESC
LIMIT 10;

SELECT '=== Super admin (for reference) ===' AS '';
SELECT id, email, full_name, role FROM users WHERE role = 'SUPER_ADMIN';
