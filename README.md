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
| Auth / Identity Service | Registration, login, JWT issuance, role management | PostgreSQL ✅ |
| Donor Service | Donor profile, blood group, availability & cooldown | PostgreSQL ✅ |
| Request Service | Emergency request lifecycle & status tracking | PostgreSQL ✅ |
| Matching Service | Blood-group + proximity matching, ranking | Caffeine (in-JVM cache) ✅ |
| Notification Service | SMS / Email / Push dispatch & delivery tracking | PostgreSQL ✅ |
| Inventory Service | Blood bank stock levels, unit reservation | PostgreSQL ✅ |
| Analytics Service | Reporting, dashboards, trend metrics for admins | PostgreSQL ✅ |
| Rewards Service | Donor points, badges & leaderboard | PostgreSQL ✅ |
| API Gateway | Single entry point, routing, JWT validation | — |
| Eureka | Service registry & discovery | — |
| Kafka | Async event bus (Matching → Notification/Inventory/Analytics/Rewards) | ✅ (Request/Matching → Notification/Analytics/Rewards wired) |
| Frontend | React + TypeScript SPA | — |

## 3. Delivery Timeline (commit-by-commit)

This repo is built incrementally, one commit per working session, in this order:

| Commit | Adds |
|---|---|
| 1 (this commit) | Project scaffolding: parent POM, `eureka-server`, `api-gateway` skeleton |
| 2 | `auth-service` — JWT issuance & role management (US-006) |
| 3 | `donor-service` + Gateway JWT filter wired in (US-001) |
| 4 | `request-service` — request lifecycle/status (US-002, US-005) |
| 5 | `matching-service` — blood-group/proximity ranking, accept/decline (US-003, US-004) |
| 6 | Kafka event bus + `notification-service` — SMS/Email/Push (US-007) |
| 7 | `inventory-service` (US-008) |
| 8 | `analytics-service` (US-009) |
| 9 | `rewards-service` (US-010) |
| 10 | `frontend` + final Gateway route wiring + polish |

Each commit's module is dropped into this same root folder alongside the existing
ones — see that commit's own instructions for exactly where each new file goes.

## 4. Database Setup (PostgreSQL via pgAdmin)

Every service uses its own PostgreSQL database. Before running a service for the first
time, create its database in pgAdmin (right-click Databases → Create → Database):

| Service | Database name |
|---|---|
| auth-service | `auth_db` |
| donor-service | `donor_db` |
| request-service | `request_db` |
| matching-service | `matching_db` |
| notification-service | `notification_db` |
| inventory-service | `inventory_db` |
| analytics-service | `analytics_db` |
| rewards-service | `rewards_db` |

## 5. Kafka Setup (local, no Docker)

Matching Service publishes events; Notification Service consumes them. Kafka needs to be
running locally before starting either of those two services:

1. Download Apache Kafka (includes Zookeeper) from https://kafka.apache.org/downloads —
   pick the binary download (e.g. `kafka_2.13-3.7.0.tgz`) and extract it.
2. Open two terminals in the extracted folder:

```bash
# Terminal 1 — start Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# Terminal 2 — start the Kafka broker
bin/kafka-server-start.sh config/server.properties
```

(On Windows, use the `.bat` equivalents in `bin\windows\`.)

Kafka now listens on `localhost:9092` — matching Matching/Notification Service's default
`KAFKA_BOOTSTRAP_SERVERS`. Topics (`donor-matched-events`, `request-confirmed-events`) are
created automatically by Matching Service on startup — no manual topic setup needed.

To stop: `Ctrl+C` in both terminals (stop Kafka before Zookeeper).

Matching Service caches donor-search results for 30 seconds using **Caffeine** — a
pure in-JVM cache (no external server, no Docker needed). This cuts down repeated calls
to Donor Service during a burst of matching activity, and needs zero setup.

Tables are created automatically on startup (`spring.jpa.hibernate.ddl-auto: update`).
Connection defaults to `localhost:5432` with user/password `postgres`/`postgres` — override
with `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` environment variables if your
local setup differs.

## 6. Running What Exists So Far

### One-command start (Windows)

From the `bloodbridge` root (after PostgreSQL DBs exist — see Section 4, and Kafka is extracted):

```powershell
# Set once per machine (adjust to your extract path)
$env:KAFKA_HOME = "C:\kafka_2.13-3.7.2"

.\scripts\start-all.ps1
# or double-click / run:  .\scripts\start-all.bat
```

This opens separate windows for Zookeeper, Kafka, Eureka, every microservice, the API Gateway,
and the React frontend (`http://localhost:3000`).

```powershell
.\scripts\stop-all.ps1              # Java + frontend
.\scripts\stop-all.ps1 -IncludeKafka  # also Kafka + Zookeeper
.\scripts\start-all.ps1 -SkipKafka    # if Kafka is already up
```

### Manual start (any OS)

```bash
# 1. Eureka (registry) — start first
cd eureka-server && mvn spring-boot:run
# → http://localhost:8761

# 2. Auth Service
cd auth-service && mvn spring-boot:run
# → http://localhost:8081

# 3. Donor Service
cd donor-service && mvn spring-boot:run
# → http://localhost:8082

# 4. Request Service
cd request-service && mvn spring-boot:run
# → http://localhost:8083

# 5. Matching Service
cd matching-service && mvn spring-boot:run
# → http://localhost:8084

# 6. Notification Service (needs Kafka + Zookeeper running — see Section 5)
cd notification-service && mvn spring-boot:run
# → http://localhost:8085

# 7. Inventory Service
cd inventory-service && mvn spring-boot:run
# → http://localhost:8086

# 8. Analytics Service (needs Kafka + Zookeeper running — see Section 5)
cd analytics-service && mvn spring-boot:run
# → http://localhost:8087

# 9. Rewards Service (needs Kafka + Zookeeper running — see Section 5)
cd rewards-service && mvn spring-boot:run
# → http://localhost:8088

# 10. API Gateway
cd api-gateway && mvn spring-boot:run
# → http://localhost:8080

# 11. Frontend
cd frontend && npm install && cp .env.example .env && npm run dev
# → http://localhost:3000
```

The SPA talks to the Gateway at `http://localhost:8080` (see `frontend/.env`). The Gateway
allows CORS from `http://localhost:3000` and skips JWT checks on CORS `OPTIONS` preflight —
without that, the browser blocks every API call from the frontend.

Notification Service sends real email via Gmail SMTP — set these before starting it:

```bash
export MAIL_USERNAME=your-gmail-address@gmail.com
export MAIL_PASSWORD=your-16-char-app-password
```
(2-Step Verification + App Password: https://myaccount.google.com/apppasswords — SMS and
push are simulated/logged for this sprint, ready to swap in Twilio/FCM credentials later.)

Try it end-to-end through the Gateway:

```bash
# Register + login (Auth Service) — open routes, no token needed yet
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Rahul Sharma","email":"rahul@example.com","password":"SecurePass123","role":"DONOR"}'

curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"rahul@example.com","password":"SecurePass123"}'
# copy the "token" from the response

# Register a donor profile (Donor Service) — now requires the token
curl -X POST http://localhost:8080/api/donors \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"name":"Rahul Sharma","bloodGroup":"O+","phone":"9876543210","email":"rahul@example.com","city":"Delhi","latitude":28.6139,"longitude":77.2090}'

# Create an emergency blood request (Request Service) — also requires a token
curl -X POST http://localhost:8080/api/requests \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"patientName":"Suresh Kumar","bloodGroup":"B+","unitsNeeded":2,"hospitalName":"AIIMS Delhi","city":"Delhi","latitude":28.6129,"longitude":77.2295,"urgency":"CRITICAL"}'

# Track its status
curl http://localhost:8080/api/requests/1 -H "Authorization: Bearer <token>"

# Trigger matching (Matching Service) — ranks eligible donors by real distance
curl -X POST http://localhost:8080/api/matches/requests/1 \
  -H "Authorization: Bearer <token>"

# Donor responds to the match
curl -X POST http://localhost:8080/api/matches/requests/1/responses \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"donorId":1,"accepted":true}'

# View a recipient's notification delivery history (donor id or requester id)
curl http://localhost:8080/api/notifications/1 -H "Authorization: Bearer <token>"

# Blood bank coordinator adds stock
curl -X POST http://localhost:8080/api/inventory \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"bloodBankName":"Red Cross Delhi","city":"Delhi","bloodGroup":"O+","unitsAvailable":10,"collectedDate":"2026-07-20","expiryDate":"2026-08-30"}'

# Reserve units against confirmed stock
curl -X POST http://localhost:8080/api/inventory/reserve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"bloodGroup":"O+","city":"Delhi","unitsNeeded":2}'

# Check low-stock alerts (below the configured safety threshold, default 5)
curl http://localhost:8080/api/inventory/alerts -H "Authorization: Bearer <token>"

# Admin analytics dashboard (requires a token with role=ADMIN — a DONOR/REQUESTER token gets 403)
curl "http://localhost:8080/api/analytics/dashboard?city=Delhi" \
  -H "Authorization: Bearer <admin-token>"

# Export the same filtered data as CSV
curl "http://localhost:8080/api/analytics/export?city=Delhi" \
  -H "Authorization: Bearer <admin-token>" -o bloodbridge-report.csv

# Mark a confirmed request as fulfilled once the donation actually happens —
# this is what credits reward points via Kafka (request-status-changed-events)
curl -X PATCH http://localhost:8080/api/requests/1/fulfill -H "Authorization: Bearer <token>"

# Check a donor's reward profile (points, donations, badges)
curl http://localhost:8080/api/rewards/1 -H "Authorization: Bearer <token>"

# City leaderboard, ranked by points
curl "http://localhost:8080/api/rewards/leaderboard?city=Delhi" -H "Authorization: Bearer <token>"
```

**Reward mechanics**: 100 points per fulfilled donation (`REWARDS_POINTS_PER_DONATION` env var to
change), with automatic one-time badges at 5 donations ("Bronze Lifesaver"), 10 ("Silver
Lifesaver"), and 25 ("Gold Lifesaver"). A donor's city for the leaderboard is resolved from
Donor Service the first time they earn points.

## 7. Frontend

React 18 + TypeScript + Vite + Tailwind CSS, talking to every service through the API
Gateway only (no service is called directly from the browser).

**Design system**: clinical-trust palette — deep teal-green primary (`#0B6E4F`), blood-red
reserved for urgent actions/alerts (`#D6224C`), warm off-white background. Headlines in
Fraunces (a distinctive serif), body text in Inter, data/stats in IBM Plex Mono. A drawn
"pulse line" (heartbeat) is the app's signature motif, used in the navbar and landing page.

**Pages, by role**:

| Route | Role | Purpose |
|---|---|---|
| `/register`, `/login` | anyone | Account creation / auth (issues the JWT) |
| `/donor/register` | DONOR | Register donor profile (blood group, location) |
| `/rewards` | DONOR | Points, badges, city leaderboard |
| `/requests/new` | REQUESTER | Raise an emergency blood request |
| `/requests` | REQUESTER | List of requests they've raised |
| `/requests/:id` | any authenticated user | Live status, trigger matching, accept/decline, mark fulfilled |
| `/bank/portal` | BLOOD_BANK | Blood bank stock: add, search, low-stock alerts |
| `/admin/dashboard` | ADMIN | Analytics dashboard + CSV export (403 for non-admins, enforced at the Gateway) |

The JWT is stored client-side and attached to every request via an axios interceptor;
a 401 response anywhere logs the user out automatically.

## 8. Branching Strategy

| Branch | Purpose | Who Merges |
|---|---|---|
| `main` | Production-ready, stable code only | Team Lead (PR + review) |
| `develop` | Integration branch for all features | Team Lead |
| `feature/<story-id>` | One branch per user story | Developer (PR → develop) |
| `hotfix/<issue>` | Critical bug fixes only | Team Lead |

**Never commit directly to `main`.**

## 9. Commit Convention (Conventional Commits)

`feat(US-00X): ...`, `fix(...): ...`, `test(...): ...`, `docs: ...`, `chore: ...`,
`refactor: ...` — scoped to the user story ID where applicable.

## 10. Repository Structure (grows one module per commit)

```
bloodbridge/
├── pom.xml                 (parent — module list grows each commit)
├── scripts/
│   ├── start-all.ps1       (Kafka + all services + frontend)
│   ├── start-all.bat
│   └── stop-all.ps1
├── eureka-server/
├── api-gateway/
├── auth-service/            ✅ landed commit 2
├── donor-service/           ✅ landed commit 3
├── request-service/          ✅ landed commit 4
├── matching-service/       ✅ landed commit 5
├── notification-service/   ✅ landed commit 6
├── inventory-service/      ✅ landed commit 7
├── analytics-service/      ✅ landed commit 8
├── rewards-service/        ✅ landed commit 9
└── frontend/                ✅ landed commit 10
```

## 11. User Stories Implemented (tracked as they land)

| ID | Story | Landed In |
|---|---|---|
| US-006 | Secure Login & Role-Based Access | Commit 2 ✅ |
| US-001 | Donor Registration | Commit 3 ✅ |
| US-002 | Create Emergency Blood Request | Commit 4 ✅ |
| US-005 | Track Emergency Request Status | Commit 4 ✅ |
| US-003 | Donor Matching by Blood Group & Location | Commit 5 ✅ |
| US-004 | Donor Accepts / Declines Match Request | Commit 5 ✅ |
| US-007 | Multi-Channel Donor Notification | Commit 6 ✅ |
| US-008 | Blood Bank Inventory Management | Commit 7 ✅ |
| US-009 | Admin Analytics & Reporting Dashboard | Commit 8 ✅ |
| US-006 AC3 (role-based 403) | Admin-only route enforcement | Commit 8 ✅ (retrofitted onto Gateway) |
| US-010 | Donor Rewards & Recognition | Commit 9 ✅ |

All 10 user stories are now live end-to-end, across 8 microservices, an API Gateway, Eureka,
Kafka, and a React frontend — built and committed incrementally, one working slice per day.
