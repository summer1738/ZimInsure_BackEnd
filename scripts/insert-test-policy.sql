-- Insert a test policy for Policy CRUD testing (run once; requires existing client_id and car_id).
-- Usage: mysql -u root -p ziminsure < scripts/insert-test-policy.sql
-- Get valid client_id and car_id from: SELECT id FROM users WHERE role='CLIENT' LIMIT 1; SELECT id FROM car LIMIT 1;

SET @client_id = (SELECT id FROM users WHERE role = 'CLIENT' LIMIT 1);
SET @car_id = (SELECT id FROM car WHERE client_id = @client_id LIMIT 1);

INSERT INTO policy (policy_number, type, status, start_date, end_date, premium, client_id, car_id)
SELECT 
  CONCAT('POL-TEST-', UNIX_TIMESTAMP()),
  'COMPREHENSIVE',
  'Active',
  CURDATE(),
  DATE_ADD(CURDATE(), INTERVAL 1 YEAR),
  250.00,
  @client_id,
  @car_id
WHERE @client_id IS NOT NULL AND @car_id IS NOT NULL;

SELECT 'Inserted test policy (if client and car exist):' AS '';
SELECT id, policy_number, type, status, client_id, car_id FROM policy ORDER BY id DESC LIMIT 1;
