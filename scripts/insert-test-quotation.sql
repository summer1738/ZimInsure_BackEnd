-- Insert a test quotation for Quotation CRUD testing (run once; requires existing client_id and car).
-- Usage: mysql -u root -p ziminsure < scripts/insert-test-quotation.sql
-- Get valid client_id and car_id from: SELECT id FROM users WHERE role='CLIENT' LIMIT 1; SELECT id FROM car WHERE client_id = <client_id> LIMIT 1;

SET @client_id = (SELECT id FROM users WHERE role = 'CLIENT' LIMIT 1);
SET @car_id = (SELECT id FROM car WHERE client_id = @client_id LIMIT 1);

INSERT INTO quotation (quotation_number, policy_type, status, amount, created_date, client_id, car_id)
SELECT
  CONCAT('QUOT-TEST-', UNIX_TIMESTAMP()),
  'COMPREHENSIVE',
  'Draft',
  350.00,
  CURDATE(),
  @client_id,
  @car_id
WHERE @client_id IS NOT NULL AND @car_id IS NOT NULL;

SELECT 'Inserted test quotation (if client and car exist):' AS '';
SELECT id, quotation_number, policy_type, status, client_id, car_id, amount FROM quotation ORDER BY id DESC LIMIT 1;
