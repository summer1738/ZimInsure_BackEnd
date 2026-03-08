# ZimInsure: Permissions (SUPER_ADMIN, AGENT, CLIENT)

## Roles

| Role         | Description |
|-------------|-------------|
| **SUPER_ADMIN** | Full access: all data, create agents, manage any client/car/policy/quotation/insurance term. |
| **AGENT**       | Manages **only** clients they created (`createdBy = agentId`). Can create clients and their cars, policies, quotations, insurance terms for those clients. |
| **CLIENT**      | Sees and manages **only** their own profile, cars, and related policies/quotations/insurance. Cannot create other clients or access agent-only areas. |

---

## Backend (API) – Who can do what

### Authentication
- **Register** (public): creates a **CLIENT** user.
- **Login**: any role; JWT includes `role`.
- **Create agent**: `POST /api/agents` — **SUPER_ADMIN** only.

### Agents (`/api/agents`)
- List / create / update / delete agents: **SUPER_ADMIN** only.

### Clients (`/api/clients`)
- List clients: **AGENT** (only their clients), **SUPER_ADMIN** (all clients).
- Get one client (with cars): **AGENT** (if they created that client), **SUPER_ADMIN** (any).
- Create client: **AGENT**, **SUPER_ADMIN** (client’s `createdBy` set to current user for AGENT).
- Update / delete client: **AGENT** (only clients they created), **SUPER_ADMIN** (any).

### Cars (`/api/cars`)
- **List**: **CLIENT** (own cars), **AGENT** (cars of a client they created, `clientId` required), **SUPER_ADMIN** (all cars, no filter).
- **Create**: **CLIENT** (car assigned to self), **AGENT** (car for a client they created), **SUPER_ADMIN** (any client). **Client is required for every car** (no orphan cars).
- **Update / Delete**: **CLIENT** (own cars only), **AGENT** (cars of their clients), **SUPER_ADMIN** (any car). Cars with no client (legacy/orphan) can only be updated/deleted by SUPER_ADMIN.

### Policies (`/api/policies`)
- **List**: **CLIENT** (own), **AGENT** (with `clientId` — only that client’s policies if they created the client), **SUPER_ADMIN** (all, or filter by `clientId`/`carId`).
- **Create / update / delete**: **AGENT**, **SUPER_ADMIN** only. **CLIENT** cannot create/update/delete policies via these endpoints.

### Quotations (`/api/quotations`)
- **List**: **CLIENT** (own), **AGENT** (their clients), **SUPER_ADMIN** (all).
- **Create / update / delete**: **AGENT**, **SUPER_ADMIN** only. **CLIENT** cannot create/update/delete quotations.

### Insurance terms (`/api/insurance-terms`)
- **List / get current / is-insured**: **CLIENT** (own cars), **AGENT** (clients’ cars), **SUPER_ADMIN** (any).
- **Add / update / delete terms**: **AGENT**, **SUPER_ADMIN** only.

### Notifications
- **Get by role**: **SUPER_ADMIN**, **AGENT**, **CLIENT** (each sees their own `forRole` / scoped data as implemented).
- **Mark read**: per-role logic as in `NotificationController`.

### Profile (`/api/clients/profile`, etc.)
- **Get / update own profile**: **CLIENT**, **AGENT**, **SUPER_ADMIN** (each can update their own profile as allowed by controller).

---

## Frontend (routes & UI)

- **dashboard/super-admin**: **SUPER_ADMIN** only.
- **dashboard/agent**: **AGENT** only.
- **dashboard/client**: **CLIENT** only.
- **profile**: **CLIENT**, **AGENT**, **SUPER_ADMIN**.
- **agents**: **SUPER_ADMIN** only.
- **clients**: **SUPER_ADMIN**, **AGENT**.
- **cars**: **SUPER_ADMIN**, **AGENT** (CLIENT uses profile/dashboard for their cars).
- **policies**: **SUPER_ADMIN**, **AGENT**.
- **quotations**: **SUPER_ADMIN**, **AGENT**.
- **notifications**: **SUPER_ADMIN**, **AGENT**, **CLIENT**.

Buttons (e.g. “Insure”, “Edit”, “Delete”) are shown based on role (e.g. `userRole === 'AGENT' || userRole === 'SUPER_ADMIN'`). The backend still enforces permissions; the UI only hides or shows actions.

---

## Summary table

| Resource     | List / read                    | Create              | Update / delete        |
|-------------|---------------------------------|---------------------|------------------------|
| Agents      | SUPER_ADMIN                     | SUPER_ADMIN         | SUPER_ADMIN            |
| Clients     | AGENT (theirs), SUPER_ADMIN (all) | AGENT, SUPER_ADMIN  | AGENT (theirs), SUPER_ADMIN |
| Cars        | CLIENT (own), AGENT (by clientId), SUPER_ADMIN (all) | CLIENT/AGENT/SUPER_ADMIN | CLIENT (own), AGENT (theirs), SUPER_ADMIN |
| Policies    | CLIENT (own), AGENT (filtered), SUPER_ADMIN | AGENT, SUPER_ADMIN  | AGENT, SUPER_ADMIN     |
| Quotations  | CLIENT (own), AGENT (theirs), SUPER_ADMIN | AGENT, SUPER_ADMIN  | AGENT, SUPER_ADMIN     |
| Insurance terms | CLIENT/AGENT/SUPER_ADMIN (scoped) | AGENT, SUPER_ADMIN  | AGENT, SUPER_ADMIN     |
