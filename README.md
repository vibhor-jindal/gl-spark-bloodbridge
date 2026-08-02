# BloodBridge

Emergency Blood Donation & Donor Matching Platform — GlobalLogic capstone.

## 1. Overview

BloodBridge digitizes emergency blood donation: a hospital, patient's relative, or NGO
raises a request, the platform matches and notifies eligible nearby donors, tracks
responses in real time, and — beyond matching — gives the platform visibility through
role-based auth, multi-channel notifications, blood-bank inventory, admin analytics, and
a donor rewards program.



## 2. Architecture

| Component | Responsibility | Datastore |
|---|---|---|
| Auth / Identity Service | Registration, login, JWT issuance, role management | PostgreSQL |
| Donor Service | Donor profile, blood group, availability & cooldown | PostgreSQL |
| Request Service | Emergency request lifecycle & status tracking | PostgreSQL |
| Matching Service | Blood-group + proximity matching, ranking | Redis |
| Notification Service | SMS / Email / Push dispatch & delivery tracking | PostgreSQL |
| Inventory Service | Blood bank stock levels, unit reservation | PostgreSQL |
| Analytics Service | Reporting, dashboards, trend metrics for admins | PostgreSQL |
| Rewards Service | Donor points, badges & leaderboard | PostgreSQL |
| API Gateway | Single entry point, routing, JWT validation | — |
| Eureka | Service registry & discovery | — |
| Kafka | Async event bus (Matching → Notification/Inventory/Analytics/Rewards) | — |
| Frontend | React + TypeScript SPA | — |

## 3. Commit Convention (Conventional Commits)

`feat(US-00X): ...`, `fix(...): ...`, `test(...): ...`, `docs: ...`, `chore: ...`,
`refactor: ...` — scoped to the user story ID where applicable.

## 4. Repository Structure (grows one module per commit)

```
bloodbridge/
├── pom.xml                 (parent — module list grows each commit)
├── eureka-server/
├── api-gateway/
├── auth-service/           (added commit 2)
├── donor-service/          (added commit 3)
├── request-service/        (added commit 4)
├── matching-service/       (added commit 5)
├── notification-service/   (added commit 6)
├── inventory-service/      (added commit 7)
├── analytics-service/      (added commit 8)
├── rewards-service/        (added commit 9)
└── frontend/                (added commit 10)
```

## 5. User Stories Implemented (tracked as they land)

| ID | Story | Landed In |
|---|---|---|
| US-006 | Secure Login & Role-Based Access | Commit 2 |
| US-001 | Donor Registration | Commit 3 |
| US-002 | Create Emergency Blood Request | Commit 4 |
| US-005 | Track Emergency Request Status | Commit 4 |
| US-003 | Donor Matching by Blood Group & Location | Commit 5 |
| US-004 | Donor Accepts / Declines Match Request | Commit 6 |
| US-007 | Multi-Channel Donor Notification | Commit 6 |
| US-008 | Blood Bank Inventory Management | Commit 7 |
| US-009 | Admin Analytics & Reporting Dashboard | Commit 8 |
| US-010 | Donor Rewards & Recognition | Commit 9 |
