# Policy testing

## Prerequisites

- Backend running on port **8080** (`mvn spring-boot:run` from `insurance-api`)
- MySQL database **ziminsure** with at least one **CLIENT** user and one **car** (create via UI or `scripts/test-client-creation.sh`)
- SUPER_ADMIN user (default: **ziminsure@gmail.com** / **virus1738**)

---

## 1. Automated CRUD test (recommended)

From repo root:

```bash
cd ZimInsure_BackEnd
./scripts/test-policy-crud.sh
```

**What it does:** Logs in as SUPER_ADMIN → gets first client & car → **Create** policy → **List** policies → **Update** policy → **Delete** policy.

**Env overrides (optional):**

| Variable       | Default                 | Description        |
|----------------|-------------------------|--------------------|
| `BASE_URL`     | http://localhost:8080   | API base URL       |
| `SUPER_EMAIL`  | ziminsure@gmail.com     | Super admin email  |
| `SUPER_PASSWORD` | virus1738             | Super admin password |
| `POLICY_ID`    | (none)                  | If set, **skip Create** and use this id for Update/Delete |

**Example – test only List / Update / Delete** (after inserting a policy via SQL):

```bash
mysql -u root -p ziminsure < scripts/insert-test-policy.sql
POLICY_ID=1 ./scripts/test-policy-crud.sh
```

---

## 2. API contract (PolicyRequestDTO)

**Base URL:** `http://localhost:8080` (or your `BASE_URL`)

**Auth:** `Authorization: Bearer <token>` (get token from `POST /auth/login`).

### Create policy — `POST /api/policies`

**Body (JSON):**

```json
{
  "policyNumber": "POL-001",
  "type": "COMPREHENSIVE",
  "status": "Active",
  "startDate": "2025-01-01",
  "endDate": "2026-01-01",
  "premium": 250.00,
  "carId": 1,
  "clientId": 3
}
```

- **Required:** `policyNumber`, `type`, `status`, `startDate`, `endDate`, `carId`, `clientId`
- **Optional:** `premium`
- **Roles:** AGENT, SUPER_ADMIN

### List policies — `GET /api/policies`

**Query (optional):** `?clientId=3` or `?carId=1`  
**Roles:** CLIENT (own only), AGENT (with `clientId`), SUPER_ADMIN (all or filtered)

### Update policy — `PUT /api/policies/{id}`

**Body (JSON):** same fields as create; only non-null fields are updated. `carId` / `clientId` are not used on update.

**Roles:** AGENT, SUPER_ADMIN

### Delete policy — `DELETE /api/policies/{id}`

**Roles:** AGENT, SUPER_ADMIN

---

## 3. Manual curl examples

Get token:

```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"ziminsure@gmail.com","password":"virus1738"}' | jq -r '.token')
```

Create:

```bash
curl -s -X POST "http://localhost:8080/api/policies" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"policyNumber":"POL-MANUAL","type":"COMPREHENSIVE","status":"Active","startDate":"2025-01-01","endDate":"2026-01-01","premium":200,"carId":1,"clientId":3}'
```

List:

```bash
curl -s -X GET "http://localhost:8080/api/policies" -H "Authorization: Bearer $TOKEN"
```

Update (use real id from create/list):

```bash
curl -s -X PUT "http://localhost:8080/api/policies/1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"policyNumber":"POL-MANUAL","type":"THIRD_PARTY","status":"Suspended","startDate":"2025-01-01","endDate":"2026-01-01","premium":150}'
```

Delete:

```bash
curl -s -X DELETE "http://localhost:8080/api/policies/1" -H "Authorization: Bearer $TOKEN"
```

---

## 4. Database verification

**Insert one test policy** (needs existing client + car):

```bash
mysql -u root -p ziminsure < scripts/insert-test-policy.sql
```

**Inspect policies and clients with cars:**

```bash
mysql -u root -p ziminsure < scripts/verify-policies.sql
```

**Quick check:**

```bash
mysql -u root -p ziminsure -e "SELECT id, policy_number, type, status, client_id, car_id, premium FROM policy ORDER BY id DESC LIMIT 10;"
```

---

## 5. Frontend

- **Policy Management** (SUPER_ADMIN / AGENT): `/policies`
- Uses same API with `carId` and `clientId`; create/edit in the UI and confirm list/update/delete work as expected.

---

## 6. Sample form data for UI testing

Use these values in the **Add Policy** / **Edit Policy** form when testing in the browser:

| Field           | Example value   | Notes |
|-----------------|-----------------|--------|
| **Policy Number** | `POL-UI-001`  | Any unique string (e.g. POL-UI-002, POL-TEST-001). |
| **Type**        | `COMPREHENSIVE` or `THIRD_PARTY` | Must match backend enum. |
| **Premium**     | `250` or `150.50` | Number (currency amount). |
| **Status**      | `Active` or `Suspended` or `Expired` | Must match backend enum. |
| **Start Date**  | `2025-01-01`   | Use date picker; format YYYY-MM-DD. |
| **End Date**    | `2026-01-01`   | Must be after start date. |
| **Client Name** | (read-only / display) | For **Add**, the backend needs **Client** and **Car** (IDs). If the form has client/car dropdowns, pick an existing client and car; otherwise get valid IDs from DB (see below). |

**Quick copy – one full set:**

- Policy Number: `POL-UI-001`
- Type: `COMPREHENSIVE`
- Premium: `250`
- Status: `Active`
- Start Date: `2025-01-01`
- End Date: `2026-01-01`

**Getting valid Client ID and Car ID for Add Policy**

- From DB:  
  `SELECT id FROM user WHERE role = 'CLIENT' LIMIT 1;`  
  `SELECT id FROM car LIMIT 1;`
- Or from the script: run `./scripts/test-policy-crud.sh` once; it prints the `clientId` and `carId` it used.
- If the UI has client/car dropdowns, select any listed client and car.

**Edit Policy:** Open an existing policy with **Edit**, then change e.g. Type to `THIRD_PARTY`, Status to `Suspended`, Premium to `150`, and save.

---

## 7. Scripts summary

| Script                    | Purpose                                      |
|---------------------------|----------------------------------------------|
| `scripts/test-policy-crud.sh` | Full CRUD test as SUPER_ADMIN           |
| `scripts/insert-test-policy.sql` | Insert one policy via SQL (for List/Update/Delete-only test) |
| `scripts/verify-policies.sql`    | List policies, counts, clients with cars |
