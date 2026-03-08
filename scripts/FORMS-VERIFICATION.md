# Frontend forms verification

Summary of form verification and fixes applied so forms stay in sync with backend rules (client required for cars, client/car for policy and quotation, validation).

---

## Policy (policy-management)

- **Client**: Required; dropdown; `onClientChange()` loads cars for selected client.
- **Car**: Required; dropdown lists only cars for selected client; disabled until client selected.
- **Submit**: Disabled when form invalid or (add mode and client/car not selected). Handler shows "Please select a client and a car." if missing.
- **Payload**: Sends `policyNumber`, `type`, `status`, `startDate`, `endDate`, `premium`, `clientId`, `carId` (matches `PolicyRequestDTO`).

---

## Quotation (quotation-management)

- **Client**: Required for AGENT/SUPER_ADMIN; dropdown; CLIENT sees readonly (and cannot add – Add hidden via `canEditDelete`).
- **Car**: Required; dropdown from selected client; `onClientChange()` clears car and loads client cars.
- **Submit**: Disabled when form invalid, loading, or (add mode and client/car not selected). Handler shows "Please select a client and a car." if missing.
- **Payload**: Add sends `quotationNumber`, `policyType`, `status`, `amount`, `createdDate`, `clientId`, `carId`. Edit sends same except client/car (backend update does not change client/car).
- **createEmptyQuotation()**: Explicitly sets `clientId` and `carId` to `undefined`.

---

## Car (car-management)

- **Client**: Required when adding as AGENT or SUPER_ADMIN; dropdown; list from `ClientService.getClients()`.
- **Payload**: Add sends `client: { id: selectedCar.clientId }` for AGENT/SUPER_ADMIN; CLIENT sends `client: null` (backend sets from principal). Handler blocks submit and shows "Please select a client" if role is AGENT/SUPER_ADMIN and no client.
- **Reg number**: Validated 3 letters + 4 digits (e.g. AEE9375); OK disabled until valid.
- **Edit**: No client dropdown (client not changed on update).

---

## Client (client-management)

- **Cars**: At least one car required; Add/Remove car rows; all car fields required.
- **Reg number**: Validated 3 letters + 4 digits; invalid-feedback and submit disabled via `hasInvalidCarReg()`; handler shows message if any car has invalid reg.
- **Submit**: Add disabled when form invalid or any car reg invalid; Edit disabled until `editFormLoaded`.
- **Payload**: Add uses `addClientWithCars({ client, cars })`; edit uses `updateClientWithCars(client, cars)`.

---

## Add client modal (add-client-modal)

- **Agent**: Required when `agentId === null` (SUPER_ADMIN); dropdown. When `agentId` is set (AGENT), field hidden and value set in `ngOnInit`.
- **Cars**: At least one car; validation in `handleSubmit()`; reg number validated with `isValidRegNumber` / `hasInvalidCarReg()`; submit disabled when invalid reg.
- **Submit**: Disabled when form invalid or any car reg invalid.

---

## Agent (agent-management)

- **Fields**: Name, email, phone, ID number, address, status; all required. ID number pattern `\d{8,9}[A-Z]\d{2}`.
- **Submit**: Disabled when `!agentForm.form.valid`.
- **Access**: SUPER_ADMIN only (route guard).

---

## Auth (login, register, change-password)

- **Login / change-password**: FormGroup with validators; submit on valid.
- **Register**: Creates CLIENT; form has required and pattern validators.

---

## Consistency notes

- **Client required for car**: Enforced on backend; frontend Car Management requires client for AGENT/SUPER_ADMIN and sends `client: { id }`.
- **Client + car for policy/quotation**: Enforced in UI (required dropdowns, submit disabled) and in handlers (error message).
- **Car reg number**: Same pattern (`^[A-Z]{3}\d{4}$`) in Car Management and in Client Management / Add client modal.
- **Backend error messages**: Car add uses `err?.error?.message` so backend "Client is required" etc. are shown.
