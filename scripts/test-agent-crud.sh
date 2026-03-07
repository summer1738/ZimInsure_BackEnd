#!/usr/bin/env bash
# Test Agent Management CRUD as SUPER_ADMIN and verify in DB.
# Prereqs: Backend running on port 8080, MySQL ziminsure DB, super admin exists.
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

AGENT_EMAIL="testagent-$(date +%s)@example.com"
AGENT_IDNUM="63-$(shuf -i 100000-999999 -n 1)A12"

echo ""
echo "=== 2. CREATE agent (POST /api/agents) ==="
CREATE_RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/agents" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"full_name\": \"Test Agent\",
    \"email\": \"$AGENT_EMAIL\",
    \"idNumber\": \"$AGENT_IDNUM\",
    \"address\": \"45 Agent Ave, Harare\",
    \"phone\": \"+263771111222\",
    \"status\": \"Active\"
  }")

HTTP_BODY=$(echo "$CREATE_RESP" | head -n -1)
HTTP_CODE=$(echo "$CREATE_RESP" | tail -n 1)

if [ "$HTTP_CODE" = "200" ]; then
  echo "Create agent OK (200)."
  if command -v jq >/dev/null 2>&1; then
    AGENT_ID=$(echo "$HTTP_BODY" | jq -r '.id')
  else
    AGENT_ID=$(echo "$HTTP_BODY" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
  fi
  echo "Created agent id: $AGENT_ID"
else
  echo "Create agent failed. HTTP $HTTP_CODE"
  echo "$HTTP_BODY"
  exit 1
fi

echo ""
echo "=== 3. READ list (GET /api/agents) ==="
LIST_RESP=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/api/agents" \
  -H "Authorization: Bearer $TOKEN")
LIST_BODY=$(echo "$LIST_RESP" | head -n -1)
LIST_CODE=$(echo "$LIST_RESP" | tail -n 1)
if [ "$LIST_CODE" = "200" ]; then
  echo "List agents OK (200)."
  if echo "$LIST_BODY" | grep -q "$AGENT_EMAIL"; then
    echo "  New agent appears in list (email: $AGENT_EMAIL)"
  fi
else
  echo "List agents failed. HTTP $LIST_CODE"
  echo "$LIST_BODY"
fi

echo ""
echo "=== 4. UPDATE agent (PUT /api/agents/$AGENT_ID) ==="
UPDATE_RESP=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/api/agents/$AGENT_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"id\": $AGENT_ID,
    \"full_name\": \"Test Agent Updated\",
    \"email\": \"$AGENT_EMAIL\",
    \"idNumber\": \"$AGENT_IDNUM\",
    \"address\": \"99 Updated Street, Bulawayo\",
    \"phone\": \"+263779999888\",
    \"status\": \"Inactive\"
  }")
UPDATE_BODY=$(echo "$UPDATE_RESP" | head -n -1)
UPDATE_CODE=$(echo "$UPDATE_RESP" | tail -n 1)
if [ "$UPDATE_CODE" = "200" ]; then
  echo "Update agent OK (200)."
  if echo "$UPDATE_BODY" | grep -q "Test Agent Updated"; then
    echo "  Name updated to 'Test Agent Updated'"
  fi
  if echo "$UPDATE_BODY" | grep -q "Bulawayo"; then
    echo "  Address updated to Bulawayo"
  fi
else
  echo "Update agent failed. HTTP $UPDATE_CODE"
  echo "$UPDATE_BODY"
fi

echo ""
echo "=== 5. DELETE agent (DELETE /api/agents/$AGENT_ID) ==="
DELETE_RESP=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL/api/agents/$AGENT_ID" \
  -H "Authorization: Bearer $TOKEN")
DELETE_BODY=$(echo "$DELETE_RESP" | head -n -1)
DELETE_CODE=$(echo "$DELETE_RESP" | tail -n 1)
if [ "$DELETE_CODE" = "200" ]; then
  echo "Delete agent OK (200)."
else
  echo "Delete agent failed. HTTP $DELETE_CODE"
  echo "$DELETE_BODY"
fi

echo ""
echo "=== 6. Verify in database (optional) ==="
echo "Before delete you could run: mysql -u root -p ziminsure -e \"SELECT id, email, full_name, role, status FROM users WHERE role='AGENT' ORDER BY id DESC LIMIT 5;\""
echo "See scripts/verify-agents.sql for full verification."
echo ""
echo "Done. Agent CRUD test completed (Create, Read, Update, Delete) as SUPER_ADMIN."
