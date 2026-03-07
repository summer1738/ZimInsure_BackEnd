#!/usr/bin/env bash
# Test client creation as SUPER_ADMIN and verify in DB.
# Prereqs: Backend running on port 8080, MySQL ziminsure DB exists, super admin user exists.
# Super admin (default): ziminsure@gmail.com / virus1738

set -e
BASE_URL="${BASE_URL:-http://localhost:8080}"
SUPER_EMAIL="${SUPER_EMAIL:-ziminsure@gmail.com}"
SUPER_PASSWORD="${SUPER_PASSWORD:-virus1738}"

echo "=== 1. Login as SUPER_ADMIN ==="
LOGIN_RESP=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$SUPER_EMAIL\",\"password\":\"$SUPER_PASSWORD\"}")

if echo "$LOGIN_RESP" | grep -q '"token"'; then
  if command -v jq >/dev/null 2>&1; then
    TOKEN=$(echo "$LOGIN_RESP" | jq -r '.token')
  else
    TOKEN=$(echo "$LOGIN_RESP" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p' | tr -d '\n\r')
  fi
  echo "Login OK. Token length: ${#TOKEN}"
else
  echo "Login failed. Response: $LOGIN_RESP"
  exit 1
fi

echo ""
echo "=== 2. Create client with one car (POST /api/clients) ==="
# Use unique email/idNumber/regNumber so you can run multiple times (change or delete existing first)
CLIENT_EMAIL="testclient-$(date +%s)@example.com"
CLIENT_IDNUM="63-$(shuf -i 100000-999999 -n 1)X12"
REG_NUM="TST$(shuf -i 1000-9999 -n 1)"

CREATE_RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/clients" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"client\": {
      \"full_name\": \"Test Client\",
      \"email\": \"$CLIENT_EMAIL\",
      \"idNumber\": \"$CLIENT_IDNUM\",
      \"address\": \"123 Test Street, Harare\",
      \"phone\": \"+263771234567\",
      \"status\": \"Active\"
    },
    \"cars\": [{
      \"regNumber\": \"$REG_NUM\",
      \"make\": \"Toyota\",
      \"model\": \"Corolla\",
      \"year\": 2020,
      \"owner\": \"Test Client\",
      \"status\": \"Active\",
      \"type\": \"PRIVATE\"
    }]
  }")

HTTP_BODY=$(echo "$CREATE_RESP" | head -n -1)
HTTP_CODE=$(echo "$CREATE_RESP" | tail -n 1)

if [ "$HTTP_CODE" = "200" ]; then
  echo "Create client OK (200)."
  echo "Response (client): $HTTP_BODY"
  CLIENT_ID=$(echo "$HTTP_BODY" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
  echo "Created client id: $CLIENT_ID"
else
  echo "Create client failed. HTTP $HTTP_CODE"
  echo "$HTTP_BODY"
  exit 1
fi

echo ""
echo "=== 3. Verify in database (MySQL) ==="
echo "Run these against DB: ziminsure, user: root (see application.properties)"
echo ""
echo "  mysql -u root -p ziminsure -e \"SELECT id, email, full_name, role, created_by FROM users WHERE email='$CLIENT_EMAIL';\""
echo "  mysql -u root -p ziminsure -e \"SELECT id, reg_number, make, model, client_id FROM car WHERE client_id=$CLIENT_ID;\""
echo ""
echo "Or open MySQL and run:"
echo "  SELECT id, email, full_name, role, created_by FROM users WHERE role='CLIENT' ORDER BY id DESC LIMIT 5;"
echo "  SELECT c.id, c.reg_number, c.make, c.client_id, u.full_name FROM car c JOIN users u ON c.client_id = u.id ORDER BY c.id DESC LIMIT 5;"
echo ""
echo "Done. Client email: $CLIENT_EMAIL, reg: $REG_NUM"
