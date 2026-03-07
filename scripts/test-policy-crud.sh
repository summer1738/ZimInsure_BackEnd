#!/usr/bin/env bash
# Test Policy CRUD as SUPER_ADMIN and verify in DB.
# Prereqs: Backend running on port 8080, MySQL ziminsure DB, super admin exists,
#          at least one CLIENT and one CAR in the DB (e.g. create via UI or test-client-creation.sh).
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
echo "=== 2. Get a client and car (for policy create) ==="
CLIENTS_RESP=$(curl -s -X GET "$BASE_URL/api/clients" -H "Authorization: Bearer $TOKEN")
CARS_RESP=$(curl -s -X GET "$BASE_URL/api/cars" -H "Authorization: Bearer $TOKEN")

if command -v jq >/dev/null 2>&1; then
  CLIENT_ID=$(echo "$CLIENTS_RESP" | jq -r 'if type == "array" then (.[0].id // empty) else empty end')
  CAR_ID=$(echo "$CARS_RESP" | jq -r 'if type == "array" then (.[0].id // empty) else empty end')
else
  CLIENT_ID=$(echo "$CLIENTS_RESP" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
  CAR_ID=$(echo "$CARS_RESP" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
fi

if [ -z "$CLIENT_ID" ] || [ "$CLIENT_ID" = "null" ]; then
  echo "No client found. Create a client with at least one car first (e.g. via UI or scripts/test-client-creation.sh)."
  exit 1
fi
if [ -z "$CAR_ID" ] || [ "$CAR_ID" = "null" ]; then
  echo "No car found. Create a client with at least one car first."
  exit 1
fi
echo "Using client_id=$CLIENT_ID, car_id=$CAR_ID"

# If POLICY_ID is set, skip create and use it for update/delete (e.g. after inserting via scripts/insert-test-policy.sql)
if [ -n "$POLICY_ID" ] && [ "$POLICY_ID" != "null" ]; then
  echo "Using existing POLICY_ID=$POLICY_ID (skip create)."
  SKIP_CREATE=1
fi

POLICY_NUM="POL-$(date +%s)"
START_DATE=$(date +%Y-%m-%d)
END_DATE=$(date -d "+1 year" +%Y-%m-%d 2>/dev/null || date -v+1y +%Y-%m-%d 2>/dev/null)

echo ""
echo "=== 3. CREATE policy (POST /api/policies) ==="
if [ "$SKIP_CREATE" = "1" ]; then
  echo "Skipped (POLICY_ID provided)."
else
CREATE_RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/policies" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"policyNumber\": \"$POLICY_NUM\",
    \"type\": \"COMPREHENSIVE\",
    \"status\": \"Active\",
    \"startDate\": \"$START_DATE\",
    \"endDate\": \"$END_DATE\",
    \"premium\": 250.00,
    \"carId\": $CAR_ID,
    \"clientId\": $CLIENT_ID
  }")

HTTP_BODY=$(echo "$CREATE_RESP" | head -n -1)
HTTP_CODE=$(echo "$CREATE_RESP" | tail -n 1)

if [ "$HTTP_CODE" = "200" ]; then
  echo "Create policy OK (200)."
  if command -v jq >/dev/null 2>&1; then
    POLICY_ID=$(echo "$HTTP_BODY" | jq -r '.id')
  else
    POLICY_ID=$(echo "$HTTP_BODY" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
  fi
  echo "Created policy id: $POLICY_ID, number: $POLICY_NUM"
else
  echo "Create policy failed. HTTP $HTTP_CODE"
  echo "$HTTP_BODY"
  if [ -z "$POLICY_ID" ]; then
    echo "To test List/Update/Delete only: insert a policy via scripts/insert-test-policy.sql then run with POLICY_ID=<id>"
    exit 1
  fi
fi
fi

echo ""
echo "=== 4. READ list (GET /api/policies) ==="
LIST_RESP=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/policies" \
  -H "Authorization: Bearer $TOKEN")
LIST_BODY=$(echo "$LIST_RESP" | head -n -1)
LIST_CODE=$(echo "$LIST_RESP" | tail -n 1)
if [ "$LIST_CODE" = "200" ]; then
  echo "List policies OK (200)."
  if echo "$LIST_BODY" | grep -q "$POLICY_NUM"; then
    echo "  New policy appears in list (policyNumber: $POLICY_NUM)"
  fi
else
  echo "List policies failed. HTTP $LIST_CODE"
  echo "$LIST_BODY"
fi

echo ""
echo "=== 5. UPDATE policy (PUT /api/policies/$POLICY_ID) ==="
UPDATE_RESP=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/api/policies/$POLICY_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"policyNumber\": \"$POLICY_NUM\",
    \"type\": \"THIRD_PARTY\",
    \"status\": \"Suspended\",
    \"startDate\": \"$START_DATE\",
    \"endDate\": \"$END_DATE\",
    \"premium\": 180.50
  }")
UPDATE_BODY=$(echo "$UPDATE_RESP" | head -n -1)
UPDATE_CODE=$(echo "$UPDATE_RESP" | tail -n 1)
if [ "$UPDATE_CODE" = "200" ]; then
  echo "Update policy OK (200)."
  if echo "$UPDATE_BODY" | grep -q "THIRD_PARTY"; then
    echo "  Type updated to THIRD_PARTY"
  fi
  if echo "$UPDATE_BODY" | grep -q "Suspended"; then
    echo "  Status updated to Suspended"
  fi
else
  echo "Update policy failed. HTTP $UPDATE_CODE"
  echo "$UPDATE_BODY"
fi

echo ""
echo "=== 6. DELETE policy (DELETE /api/policies/$POLICY_ID) ==="
DELETE_RESP=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL/api/policies/$POLICY_ID" \
  -H "Authorization: Bearer $TOKEN")
DELETE_BODY=$(echo "$DELETE_RESP" | head -n -1)
DELETE_CODE=$(echo "$DELETE_RESP" | tail -n 1)
if [ "$DELETE_CODE" = "200" ]; then
  echo "Delete policy OK (200)."
else
  echo "Delete policy failed. HTTP $DELETE_CODE"
  echo "$DELETE_BODY"
fi

echo ""
echo "=== 7. Verify in database (optional) ==="
echo "Before delete you could run:"
echo "  mysql -u root -p ziminsure -e \"SELECT id, policy_number, type, status, client_id, car_id, premium FROM policy ORDER BY id DESC LIMIT 5;\""
echo "See scripts/verify-policies.sql for full verification."
echo ""
echo "Done. Policy CRUD test completed (Create, Read, Update, Delete) as SUPER_ADMIN."
