# Quotation testing

## Prerequisites

- Backend running on port **8080** (`mvn spring-boot:run` from `insurance-api`)
- MySQL database **ziminsure** with at least one **CLIENT** user and one **car** belonging to that client (create via UI or `scripts/test-client-creation.sh`)
- SUPER_ADMIN user (default: **ziminsure@gmail.com** / **virus1738**)

---

## 1. Automated CRUD test (recommended)

From repo root:

```bash
cd ZimInsure_BackEnd
chmod +x scripts/test-quotation-crud.sh   # if needed
./scripts/test-quotation-crud.sh
```

**What it does:** Logs in as SUPER_ADMIN → gets first client and one of their cars → **Create** quotation → **List** quotations → **Update** quotation → **Delete** quotation.

**Env overrides (optional):**

| Variable         | Default                 | Description           |
|------------------|-------------------------|-----------------------|
| `BASE_URL`       | http://localhost:8080   | API base URL          |
| `SUPER_EMAIL`    | ziminsure@gmail.com      | Super admin email     |
| `SUPER_PASSWORD` | virus1738               | Super admin password  |
| `QUOTATION_ID`   | (none)                   | If set, **skip Create** and use this id for Update/Delete |

**Example – test only List / Update / Delete** (after inserting a quotation via SQL):

```bash
mysql -u root -p ziminsure < scripts/insert-test-quotation.sql
QUOTATION_ID=1 ./scripts/test-quotation-crud.sh
```

---

## 2. API contract (QuotationRequestDTO / QuotationResponse)

**Base URL:** `http://localhost:8080` (or your `BASE_URL`)

**Auth:** `Authorization: Bearer <token>` (get token from `POST /auth/login`).

- **Create/Update request:** flat DTO with `clientId`, `carId` (no nested `client`/`car` objects), same pattern as Policy.
- **List/Response:** `QuotationResponse` with `id`, `quotationNumber`, `policyType`, `status`, `amount`, `createdDate`, `clientId`, `clientName`, `carId`, `carRegNumber`, `agentId`.

### Create quotation — `POST /api/quotations`

**Body (JSON):**

```json
{
  "quotationNumber": "QUOT-001",
  "policyType": "COMPREHENSIVE",
  "status": "Draft",
  "amount": 350.00,
  "createdDate": "2025-03-05",
  "clientId": 3,
  "carId": 1
}
```

- **Required:** `quotationNumber`, `policyType`, `status`, `amount`, `createdDate`, `clientId`, `carId` (flat DTO, same style as Policy).
- **Roles:** CLIENT (client set to self), AGENT (client required, must be agent’s client), SUPER_ADMIN (client and car required)

### List quotations — `GET /api/quotations`

**Query (optional):** `?clientId=3` or `?agentId=2` or `?carId=1`  
**Roles:** CLIENT (own only), AGENT (own or filter by clientId), SUPER_ADMIN (all or filtered)

### Update quotation — `PUT /api/quotations/{id}`

**Body (JSON):** `policyType`, `status`, `amount` (and optionally `quotationNumber`, `createdDate`). Client and car are not changed. Request body uses **QuotationRequestDTO** (flat fields only).

**Roles:** AGENT (own quotations only), SUPER_ADMIN

### Delete quotation — `DELETE /api/quotations/{id}`

**Roles:** AGENT (own only), SUPER_ADMIN

---

## 3. Manual curl examples

Get token:

```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"ziminsure@gmail.com","password":"virus1738"}' | jq -r '.token')
```

Create (use real client id and a car that belongs to that client):

```bash
curl -s -X POST "http://localhost:8080/api/quotations" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"quotationNumber":"QUOT-MANUAL","policyType":"COMPREHENSIVE","status":"Draft","amount":350,"createdDate":"2025-03-05","clientId":3,"carId":1}'
```

List:

```bash
curl -s -X GET "http://localhost:8080/api/quotations" -H "Authorization: Bearer $TOKEN"
```

Update (use real id from create/list):

```bash
curl -s -X PUT "http://localhost:8080/api/quotations/1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"quotationNumber":"QUOT-MANUAL","policyType":"THIRD_PARTY","status":"Accepted","amount":280.50,"createdDate":"2025-03-05"}'
```

Delete:

```bash
curl -s -X DELETE "http://localhost:8080/api/quotations/1" -H "Authorization: Bearer $TOKEN"
```

---

## 4. Database verification

**Insert one test quotation** (needs existing client + car for that client):

```bash
mysql -u root -p ziminsure < scripts/insert-test-quotation.sql
```

**Inspect quotations and clients with cars:**

```bash
mysql -u root -p ziminsure < scripts/verify-quotations.sql
```

**Quick check:**

```bash
mysql -u root -p ziminsure -e "SELECT id, quotation_number, policy_type, status, client_id, car_id, amount FROM quotation ORDER BY id DESC LIMIT 10;"
```

---

## 5. Frontend (quotation form)

- **Quotation Management** (SUPER_ADMIN / AGENT): `/quotations`
- Form follows same logic as **Policy**: **Client** dropdown (all clients), then **Car** dropdown (cars for selected client only). Select client first, then pick a car from that client’s cars.
- Create requires both client and car; backend validates that car belongs to client when creating via API.

---

## 6. Sample form data for UI testing

| Field              | Example value   | Notes |
|--------------------|-----------------|--------|
| **Quotation Number** | `QUOT-UI-001` | Any unique string. |
| **Policy Type**    | `COMPREHENSIVE` or `THIRD_PARTY` | |
| **Amount**         | `350` or `280.50` | Number. |
| **Status**         | `Draft` or `Accepted` or `Rejected` | |
| **Date**           | `2025-03-05`    | Use date picker; format YYYY-MM-DD. |
| **Client**         | Select from dropdown | Then **Car** list loads for that client. |
| **Car**            | Select from dropdown | Only cars for the selected client. |

**Quick copy:** Quotation Number `QUOT-UI-001`, Policy Type `COMPREHENSIVE`, Amount `350`, Status `Draft`, Date today. Then select a client and a car from the dropdowns.

---

## 7. Scripts summary

| Script                         | Purpose |
|--------------------------------|--------|
| `scripts/test-quotation-crud.sh`   | Full CRUD test as SUPER_ADMIN |
| `scripts/insert-test-quotation.sql` | Insert one quotation via SQL (for List/Update/Delete-only test) |
| `scripts/verify-quotations.sql`     | List quotations, counts, clients with cars |
