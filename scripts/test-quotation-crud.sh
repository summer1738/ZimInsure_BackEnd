#!/usr/bin/env bash
# Test Quotation CRUD as SUPER_ADMIN and verify in DB.
# Prereqs: Backend running on port 8080, MySQL ziminsure DB, super admin exists,
#          at least one CLIENT and one CAR (car must belong to that client).
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
echo "=== 2. Get a client and one of their cars ==="
CLIENTS_RESP=$(curl -s -X GET "$BASE_URL/api/clients" -H "Authorization: Bearer $TOKEN")
if command -v jq >/dev/null 2>&1; then
  CLIENT_ID=$(echo "$CLIENTS_RESP" | jq -r 'if type == "array" then (.[0].id // empty) else empty end')
else
  CLIENT_ID=$(echo "$CLIENTS_RESP" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
fi
if [ -z "$CLIENT_ID" ] || [ "$CLIENT_ID" = "null" ]; then
  echo "No client found. Create a client with at least one car first (e.g. via UI or scripts/test-client-creation.sh)."
  exit 1
fi
# Get cars for this client only
CARS_RESP=$(curl -s -X GET "$BASE_URL/api/cars?clientId=$CLIENT_ID" -H "Authorization: Bearer $TOKEN")
if command -v jq >/dev/null 2>&1; then
  CAR_ID=$(echo "$CARS_RESP" | jq -r 'if type == "array" then (.[0].id // empty) else empty end')
else
  CAR_ID=$(echo "$CARS_RESP" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
fi
if [ -z "$CAR_ID" ] || [ "$CAR_ID" = "null" ]; then
  echo "No car found for client $CLIENT_ID. Create a car for this client first."
  exit 1
fi
echo "Using client_id=$CLIENT_ID, car_id=$CAR_ID"

# If QUOTATION_ID is set, skip create and use it for update/delete
if [ -n "$QUOTATION_ID" ] && [ "$QUOTATION_ID" != "null" ]; then
  echo "Using existing QUOTATION_ID=$QUOTATION_ID (skip create)."
  SKIP_CREATE=1
fi

QUOT_NUM="QUOT-$(date +%s)"
CREATED_DATE=$(date +%Y-%m-%d)

echo ""
echo "=== 3. CREATE quotation (POST /api/quotations) ==="
if [ "$SKIP_CREATE" = "1" ]; then
  echo "Skipped (QUOTATION_ID provided)."
else
CREATE_RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/quotations" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"quotationNumber\": \"$QUOT_NUM\",
    \"policyType\": \"COMPREHENSIVE\",
    \"status\": \"Draft\",
    \"amount\": 350.00,
    \"createdDate\": \"$CREATED_DATE\",
    \"clientId\": $CLIENT_ID,
    \"carId\": $CAR_ID
  }")

HTTP_BODY=$(echo "$CREATE_RESP" | head -n -1)
HTTP_CODE=$(echo "$CREATE_RESP" | tail -n 1)

if [ "$HTTP_CODE" = "200" ]; then
  echo "Create quotation OK (200)."
  if command -v jq >/dev/null 2>&1; then
    QUOTATION_ID=$(echo "$HTTP_BODY" | jq -r '.id')
  else
    QUOTATION_ID=$(echo "$HTTP_BODY" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
  fi
  echo "Created quotation id: $QUOTATION_ID, number: $QUOT_NUM"
else
  echo "Create quotation failed. HTTP $HTTP_CODE"
  echo "$HTTP_BODY"
  if [ -z "$QUOTATION_ID" ]; then
    echo "To test List/Update/Delete only: insert a quotation via scripts/insert-test-quotation.sql then run with QUOTATION_ID=<id>"
    exit 1
  fi
fi
fi

echo ""
echo "=== 4. READ list (GET /api/quotations) ==="
LIST_RESP=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/quotations" \
  -H "Authorization: Bearer $TOKEN")
LIST_BODY=$(echo "$LIST_RESP" | head -n -1)
LIST_CODE=$(echo "$LIST_RESP" | tail -n 1)
if [ "$LIST_CODE" = "200" ]; then
  echo "List quotations OK (200)."
  if [ -n "$QUOT_NUM" ] && echo "$LIST_BODY" | grep -q "$QUOT_NUM"; then
    echo "  New quotation appears in list (quotationNumber: $QUOT_NUM)"
  fi
else
  echo "List quotations failed. HTTP $LIST_CODE"
  echo "$LIST_BODY"
fi

echo ""
echo "=== 5. UPDATE quotation (PUT /api/quotations/$QUOTATION_ID) ==="
UPDATE_RESP=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/api/quotations/$QUOTATION_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"quotationNumber\": \"$QUOT_NUM\",
    \"policyType\": \"THIRD_PARTY\",
    \"status\": \"Accepted\",
    \"amount\": 280.50,
    \"createdDate\": \"$CREATED_DATE\"
  }")
UPDATE_BODY=$(echo "$UPDATE_RESP" | head -n -1)
UPDATE_CODE=$(echo "$UPDATE_RESP" | tail -n 1)
if [ "$UPDATE_CODE" = "200" ]; then
  echo "Update quotation OK (200)."
  if echo "$UPDATE_BODY" | grep -q "THIRD_PARTY"; then
    echo "  Policy type updated to THIRD_PARTY"
  fi
  if echo "$UPDATE_BODY" | grep -q "Accepted"; then
    echo "  Status updated to Accepted"
  fi
else
  echo "Update quotation failed. HTTP $UPDATE_CODE"
  echo "$UPDATE_BODY"
fi

echo ""
echo "=== 6. DELETE quotation (DELETE /api/quotations/$QUOTATION_ID) ==="
DELETE_RESP=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL/api/quotations/$QUOTATION_ID" \
  -H "Authorization: Bearer $TOKEN")
DELETE_BODY=$(echo "$DELETE_RESP" | head -n -1)
DELETE_CODE=$(echo "$DELETE_RESP" | tail -n 1)
if [ "$DELETE_CODE" = "200" ]; then
  echo "Delete quotation OK (200)."
else
  echo "Delete quotation failed. HTTP $DELETE_CODE"
  echo "$DELETE_BODY"
fi

echo ""
echo "=== 7. Verify in database (optional) ==="
echo "Before delete you could run:"
echo "  mysql -u root -p ziminsure -e \"SELECT id, quotation_number, policy_type, status, client_id, car_id, amount FROM quotation ORDER BY id DESC LIMIT 5;\""
echo "See scripts/verify-quotations.sql for full verification."
echo ""
echo "Done. Quotation CRUD test completed (Create, Read, Update, Delete) as SUPER_ADMIN."
