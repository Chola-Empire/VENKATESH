# Security Specification — Chola Empire Construction Accounts

## 1. Data Invariants
1. **Workspace Isolation**: Every document in `/users/{userId}`, `/users/{userId}/clients/{clientId}`, `/users/{userId}/projects/{projectId}`, and `/users/{userId}/transactions/{transactionId}` belongs strictly to `userId == request.auth.uid` with `request.auth.token.email_verified == true`.
2. **Master Gate Relational Sync**: Writes to `clients`, `projects`, and `transactions` verify parent workspace ownership (`userId == request.auth.uid`) and validate `ownerId == request.auth.uid` on both create and update.
3. **Referential Integrity on Transactions**: Creating a `transaction` verifies via `exists()` that the referenced `projectId` exists under `/users/$(userId)/projects/$(incoming().projectId)`.
4. **Immutable Ownership & Creation Timestamps**: `ownerId`, `id`, and `createdAt` cannot change after creation; `createdAt == request.time` on create and `updatedAt == request.time` on create/update.
5. **Strict Schema & Bounds**: All IDs match `^[a-zA-Z0-9_\-]+$` (`size() <= 128`), all strings enforce bounded `.size()`, numeric `amount > 0` and `contractValue >= 0`, and `keys().hasOnly(...)` / `affectedKeys().hasOnly(...)` block shadow fields.

## 2. The "Dirty Dozen" Attack Payloads (All Must Return PERMISSION_DENIED)
1. **Unauthenticated Read/Write**: `auth = null` attempting `get` or `create` on `/users/u1/projects/p1`.
2. **Unverified Email**: `auth = { uid: "u1", token: { email_verified: false } }` writing to `/users/u1/clients/c1`.
3. **Cross-User Read (PII Leak)**: `u2` attempting `get` or `list` on `/users/u1/clients/c1` (contains client phone/address/GSTIN).
4. **Cross-User Write**: `u2` attempting `create` under `/users/u1/transactions/t1`.
5. **OwnerId Spoofing**: `u1` creating `/users/u1/projects/p1` with `ownerId: "u2"`.
6. **Ghost / Shadow Field Injection**: `u1` creating `/users/u1/projects/p1` with extra key `isAdmin: true`.
7. **ID Poisoning**: `u1` creating `/users/u1/projects/invalid!id$with*spaces` or a 500-char ID.
8. **String Exhaustion**: `u1` creating `/users/u1/clients/c1` with `name` of 5,000 characters.
9. **Orphaned Transaction**: `u1` creating `/users/u1/transactions/t1` with `projectId: "non_existent_project"`.
10. **Client Timestamp Forgery**: `u1` creating `/users/u1/transactions/t1` with backdated `createdAt` instead of `request.time`.
11. **Immutable Field Mutation**: `u1` updating `ownerId` or `createdAt` on an existing project.
12. **Negative Amount / Invalid Enum**: `u1` creating a transaction with `amount: -500` or `status: "HACKED"`.
