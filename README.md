# BloodBridge

Emergency Blood Donation & Donor Matching Platform — GlobalLogic capstone.

## 1. Overview

BloodBridge digitizes emergency blood donation: a hospital, patient's relative, or NGO
raises a request, the platform matches and notifies eligible nearby donors, tracks
responses in real time, and — beyond matching — gives the platform visibility through
role-based auth, multi-channel notifications, blood-bank inventory, admin analytics, and
a donor rewards program.

Full requirements live in the PRD; system diagrams live in the Design Slides deck
(both tracked outside this repo / attached separately per team convention).

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

## 3. Delivery Timeline (commit-by-commit)

This repo is built incrementally, one commit per working session, in this order:

| Commit | Adds |
|---|---|
| 1 (this commit) | Project scaffolding: parent POM, `eureka-server`, `api-gateway` skeleton |
| 2 | `auth-service` — JWT issuance & role management (US-006) |
| 3 | `donor-service` + Gateway JWT filter wired in (US-001) |
| 4 | `request-service` — request lifecycle/status (US-002, US-005) |
| 5 | `matching-service` + Redis — blood-group/proximity ranking (US-003) |
| 6 | Kafka event bus + `notification-service` — SMS/Email/Push (US-004, US-007) |
| 7 | `inventory-service` (US-008) |
| 8 | `analytics-service` (US-009) |
| 9 | `rewards-service` (US-010) |
| 10 | `frontend` + final Gateway route wiring + polish |

Each commit's module is dropped into this same root folder alongside the existing
ones — see that commit's own instructions for exactly where each new file goes.

## 4. Running What Exists So Far

```bash
# 1. Eureka (registry) — start first
cd eureka-server && ./mvnw spring-boot:run
# → http://localhost:8761

# 2. API Gateway
cd api-gateway && ./mvnw spring-boot:run
# → http://localhost:8080
```

There are no routed services yet in this commit — Eureka and the Gateway just need to
come up cleanly and Eureka's dashboard should show the Gateway registering itself.

## 5. Branching Strategy

| Branch | Purpose | Who Merges |
|---|---|---|
| `main` | Production-ready, stable code only | Team Lead (PR + review) |
| `develop` | Integration branch for all features | Team Lead |
| `feature/<story-id>` | One branch per user story | Developer (PR → develop) |
| `hotfix/<issue>` | Critical bug fixes only | Team Lead |

**Never commit directly to `main`.**

## 6. Commit Convention (Conventional Commits)

`feat(US-00X): ...`, `fix(...): ...`, `test(...): ...`, `docs: ...`, `chore: ...`,
`refactor: ...` — scoped to the user story ID where applicable.

## 7. Repository Structure (grows one module per commit)

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

## 8. User Stories Implemented (tracked as they land)

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
