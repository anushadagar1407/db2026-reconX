# ReconX — Enterprise Trade Reconciliation Platform (Student Starter)

> Deutsche Bank — TDI 2026 Graduate Technical Training Programme
> **Advanced Track (Intermediate-Hybrid)** | 10-Day Case Study | Version 1.0

This repository is the **starter scaffold** for the ReconX case study. Each day
of the programme adds another layer to the system. By Day 10 you and your team
will have built, dockerised, tested, and monitored a near-production-grade
trade reconciliation platform with Kafka event streaming, JWT-backed RBAC, a
React 19 dashboard, and a CI/CD pipeline that ships Docker images to GHCR.

---

## What you will build

A near-production-grade trade reconciliation platform used (in concept) by an
Ops team to detect and resolve mismatches between internal trade records and
external counterparty/custodian feeds — built across 10 days, 165 tickets.

```
       ┌──────────┐        ┌──────────────────────────┐        ┌────────────┐
       │  React   │  HTTPS │  Spring Boot REST API    │  JDBC  │ PostgreSQL │
       │ Frontend │ ─────▶ │  recon-service (Java 25) │ ─────▶ │  (Liqui-   │
       │  + Vite  │        │  + Spring Security/JWT   │        │   base     │
       └────┬─────┘        │  + Actuator/Micrometer   │        │   migs)    │
            │              └────────┬─────────────────┘        └─────┬──────┘
            │ SSE                   │  KafkaTemplate / @KafkaListener│
            │                       ▼                                ▼
            │              ┌──────────────────┐               ┌────────────┐
            └──────────────│  Apache Kafka    │               │ recon_*    │
                           │  trade-events    │               │ audit_log  │
                           │  recon-results   │               │ mat. views │
                           │  system-alerts   │               └────────────┘
                           │  + DLQ topics    │
                           └────────┬─────────┘
                                    ▼
                           ┌─────────────────────────┐
                           │ ReconConsumer (auto-rec)│
                           │ AuditConsumer (history) │
                           │ AlertConsumer  (notify) │
                           └─────────────────────────┘

  /actuator/prometheus ─▶ Prometheus (scrape) ─▶ Grafana dashboards + alerts
```

---

## Repository layout

```
reconx-studentCopy/
├── db/                            ← Day 1: standalone SQL assets
│   ├── queries.sql                ← Analytical queries (window fns, CTEs, JSONB)
│   ├── partitioning.sql           ← Monthly trade partitions
│   └── erd.md                     ← Mermaid ER diagram
│
│   NOTE: Liquibase changelogs live on the JVM classpath at
│         backend/src/main/resources/db/changelog/ — not here.
│
├── backend/                       ← Days 2-6, 9: Java 25 + Spring Boot 3 + Kafka
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/dbtraining/reconx/
│       ├── ReconxApplication.java
│       ├── model/                 ← Day 2-3: sealed TradeType hierarchy, value objects
│       ├── repository/            ← Day 4-5: Spring Data JPA + Specifications
│       ├── service/               ← Day 3-6: reconciliation engine, analytics
│       ├── controller/            ← Day 5: REST API endpoints
│       ├── dto/                   ← Request/response DTOs, TradeEvent, MapStruct mappers
│       ├── exception/             ← Custom hierarchy + @RestControllerAdvice
│       ├── config/                ← Swagger, JPA, Liquibase, Cache, Kafka config
│       ├── security/              ← Day 5: JWT filter, RBAC
│       ├── kafka/                 ← Day 9: producers, consumers, DLQ
│       └── observability/         ← Day 6: custom Micrometer metrics
│
├── static-dashboard/              ← Day 7: vanilla HTML/CSS/JS (pre-React exercise)
│   ├── dashboard.html
│   ├── trades.html
│   ├── recon.html
│   ├── css/style.css
│   └── js/*.js
│
├── frontend/                      ← Day 8-9: React 19 + Vite recon-ui
│   ├── package.json
│   ├── vite.config.js
│   ├── Dockerfile
│   └── src/
│       ├── App.jsx
│       ├── components/            ← DataTable (compound), TradeRow, StatCard, …
│       ├── hooks/                 ← useWebSocket, useTradeStream, useDebouncedSearch
│       ├── context/               ← ThemeProvider, AuthProvider
│       ├── services/              ← apiService.js
│       └── pages/                 ← Dashboard, Trades, Login, AddTrade
│
├── monitoring/                    ← Day 6 + 10: Prometheus / Grafana
│   ├── prometheus/prometheus.yml
│   └── grafana/provisioning/
│
├── .postman/                      ← Postman Git-connected workspace mapping
├── postman/                       ← Local API collection + Docker/H2 environments
├── .github/workflows/build.yml    ← Day 10: GitHub Actions pipeline
├── docker-compose.yml             ← Day 10: 7-service stack
├── .env.example                   ← Sample environment variables
└── student-guides/                ← What you read each day
```

The full per-day walkthrough lives in
[`./student-guides/`](./student-guides/README.md).
**Read [`student-guides/day0/README.md`](./student-guides/day0/README.md)
before you start.**

---

## Prerequisites

- **Java 25** (Temurin recommended — the Advanced Track uses sealed classes, records, virtual threads where they fit)
- **Maven 3.9+**
- **Node.js 20+** and npm
- **Docker Desktop** (allocate ≥ 6 GB RAM — Kafka + Postgres + Prometheus + Grafana is heavier than Intermediate)
- **PostgreSQL 16** client tools (or use the bundled Docker container)
- **Postman Desktop** (optional, for local API exploration)
- **Git**
- IDE: IntelliJ IDEA Ultimate (backend) + VS Code (frontend) recommended

---

## Quick start (after Day 4)

```bash
# 1. Bring up infrastructure (Postgres + Kafka + Prometheus + Grafana + Kafdrop)
docker compose up -d postgres kafka zookeeper prometheus grafana kafdrop

# 2. Run the backend (Liquibase runs migrations automatically on startup)
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Run the frontend
cd ../frontend
npm install
npm run dev

# 4. Open
# - Swagger UI:      http://localhost:8080/api/swagger-ui.html
# - Frontend:        http://localhost:5173
# - Prometheus:      http://localhost:9090
# - Grafana:         http://localhost:3000   (admin / admin)
# - Kafdrop:         http://localhost:9000
# - Actuator health: http://localhost:8080/api/actuator/health
```

### Default credentials (dev profile only, after you implement Day 5)

| Role          | Username        | Password     |
|---------------|-----------------|--------------|
| ADMIN         | `admin@db.com`  | `admin123`   |
| TRADER        | `trader@db.com` | `trader123`  |
| VIEWER        | `viewer@db.com` | `viewer123`  |
| RECON_ANALYST | `recon@db.com`  | `recon123`   |

JWT issued from `POST /api/auth/login` is valid for 60 minutes. There is no
refresh-token endpoint yet; authenticate again when a local token expires.

### Local API testing with Postman

The repository is connected to a Postman Desktop project workspace through
`.postman/resources.yaml`. Postman automatically registers the split YAML
resources under `postman/` in Local View and writes desktop edits back to those
files, so collection changes are reviewed and shared through normal Git commits.

After cloning, open the repository folder in the same Postman project workspace
and switch to Local View. Select `ReconX - Local Docker` when using the Compose
stack or `ReconX - Local H2` when running the backend with the dev profile. Run
folder `01 Authentication` first to save the role-specific JWTs as local values
in the selected environment, then send individual requests or use Postman's
Collection Runner. The checked-in shared token values remain blank.

Folders `00` through `07` cover the current runnable API. Folder `90` contains
manual or destructive operations, and folder `99` records API surfaces blocked
by future tickets. Do not add deployed credentials or generated JWT values to
the checked-in workspace files.

---

## Containerized verification

Docker Compose is the standard verification path; the wrappers do not require
host Java, Maven, Node, or npm. Run these commands from the repository root:

```bash
./scripts/verify              # backend and frontend (also: ./scripts/verify all)
./scripts/verify backend
./scripts/verify frontend
./scripts/verify load         # explicit TICKET-ADV097 k6 + Prometheus evidence
```

On Windows PowerShell, use the equivalent `.\scripts\verify.ps1`,
`.\scripts\verify.ps1 backend`, `.\scripts\verify.ps1 frontend`, or
`.\scripts\verify.ps1 load`.
The `all` mode runs both suites even if the first fails, labels each Compose
log section, preserves the exit status, copies reports, and removes only the
test service containers. It never runs `docker compose down`.

The raw Compose alternatives target the profiled one-shot services explicitly:

```bash
mkdir -p .verification-reports/backend/target
docker compose up --build --force-recreate --abort-on-container-exit --exit-code-from test-backend test-backend
docker compose cp test-backend:/workspace/backend/target/. .verification-reports/backend/target/
docker compose rm --force --stop test-backend test-postgres test-kafka test-zookeeper

mkdir -p .verification-reports/frontend/test-results
docker compose up --build --force-recreate --abort-on-container-exit --exit-code-from test-frontend test-frontend
docker compose cp test-frontend:/app/test-results/. .verification-reports/frontend/test-results/
docker compose rm --force --stop test-frontend
```

The raw `all` equivalent is the two target sequences above, run in order with
each `docker compose up` status saved before its copy and cleanup commands.
Preserve each `docker compose up` exit code before copying and cleaning when
using the raw commands. Explicitly naming `test-backend` or `test-frontend`
auto-enables its Compose profile and leaves the stopped container available for
`docker compose cp`; `docker compose run --rm` would remove it too early.

The wrapper currently expects backend reports at
`/workspace/backend/target` and the Vitest report at
`/app/test-results/vitest-junit.xml`. If the backend test image later uses an
`/app` path, adjust `RECONX_BACKEND_REPORTS_PATH` (and the matching CI job env)
before running verification. Reports are copied to the ignored
`.verification-reports/` directory.

### Native fallbacks and test phases

The workflow uses the container jobs by default. `workflow_dispatch` exposes
separate backend/frontend runner inputs for the manual native fallbacks: Java
25 plus Testcontainers for the backend, and Node 22 for the frontend. Those
fallbacks use the host runtime; the local equivalents are `cd backend &&
./mvnw verify` and `cd frontend && npm ci && npm run verify`.

Maven Surefire runs the ordinary `*Test` classes during `test`. Maven Failsafe
uses the `*IT` convention for integration tests and is reached by `verify`,
so `mvn test` alone does not run `*IT`; `verify` also produces the JaCoCo
report/check. The Compose backend uses its
external `test-postgres` dependency; the native fallback instead relies on
tests' Testcontainers setup and still needs a working Docker daemon. Do not
point either path at a developer database.

CI uploads raw Surefire/Failsafe XML, the Vitest JUnit XML, and JaCoCo HTML as
artifacts. The pinned JUnit reporter adds readable checks and job summaries and
fails closed on missing, malformed, or failing reports. Console output remains
in the job log. Frontend verification runs lint, Vitest, and the production
build even if an earlier phase fails, then returns one aggregate status.

The explicit `load` mode starts a separate Compose project with ephemeral
PostgreSQL data, pinned k6/Python images, and the API, Prometheus, and Grafana
services on dynamically assigned host ports. It authenticates once through
`POST /api/auth/login`, then drives exactly 100 unique trade creations with 10
k6 VUs. The ignored `.verification-reports/load/` directory receives the k6
summary, raw Prometheus panel queries, and the Grafana observation URL. Set
`RECONX_LOAD_KEEP_STACK=1` to leave that isolated project running for manual
dashboard observation; otherwise the wrapper removes only that project and its
ephemeral volumes.

The load evidence uses a 60-second paced run interval for both k6 and the
Prometheus queries. The wrapper fails unless their trade throughput values are
within 20%, both client-side k6 and server-side histogram P95 values are finite
and non-zero, and pre/post Prometheus deltas prove exactly 100 HTTP 201
responses and exactly 100 `trade_created_total` increments. Client P95 includes
network/client timing; server P95 is the endpoint-wide Micrometer HTTP
histogram, so the two values are recorded with separate labels and units rather
than presented as identical measurements. Both k6 JSON summaries are exported
from a project-scoped named volume, parsed and cross-checked, stripped of setup
credentials, and written as host-user-writable files.

The ADV097 workflow is an explicit `workflow_dispatch` load option rather than
a default pull-request job because it starts Kafka, PostgreSQL, Prometheus, and
Grafana and is intentionally a runtime evidence run, not a flaky universal
performance gate. The job uploads the tool and panel-query artifacts whenever
the option is selected.

Pull requests targeting either `develop` or `main` run full containerized
backend and frontend verification alongside the production image builds.
Manual dispatch can select `verify` or explicit image-build-only behavior
independently of the selected runner and can opt into the separate ADV097 load
job. A selected load job runs and uploads its static-validation log and all
available runtime evidence even when either validation phase fails.

---

### API versioning and deprecation

Public domain endpoints use the `/api/v1/...` prefix. Breaking changes ship
under a new version segment such as `/api/v2/...`; the existing version keeps
working until its announced `Sunset` date.

---

## Deploy to the demo laptop (Day 10)

The deploy story is **GitHub Actions builds + pushes Docker images to GHCR;
the demo laptop pulls them and runs the full stack via `docker compose up`.**
No cloud hosting, no PaaS — the demo laptop *is* the deploy target.

```bash
# One-time on the demo laptop (uses a GitHub PAT with read:packages scope):
echo "<your-PAT>" | docker login ghcr.io -u <gh-username> --password-stdin

# Each deploy:
docker compose pull        # fetches the latest CI-tested images from GHCR
docker compose up -d       # brings up all 7 services
```

Full walkthrough: [`student-guides/day10/README.md`](./student-guides/day10/README.md).

---

## How to read the TODOs in this codebase

Every place you must write code has a comment block that looks like this:

```java
// ============================================================================
// TICKET-ADV019 — Build EquityTrade with the Builder pattern
//
// WHAT:    A concrete EquityTrade record/class that extends Trade and is
//          constructed via an immutable builder.
// HOW:     Use a static inner Builder with fluent setters returning `this`;
//          build() validates and returns an EquityTrade. Mark final fields.
// WHY:     Builder pattern keeps the call-site readable for trades with 8+
//          fields and gives us a single place to enforce invariants.
// OBSERVE: A trade missing required fields throws IllegalStateException at
//          build(), NOT at field-set time. Verify with the unit test in
//          EquityTradeTest.builder_missingPrice_throws.
// HINT:    See ../model/FXTrade.java for the same pattern applied to a
//          two-currency trade.
// ============================================================================
```

Below each block the method body is replaced with `// TODO(TICKET-IHxxx)` and
either an `UnsupportedOperationException` or a minimal placeholder return.
Your job is to remove the TODO and implement the body.

The full ticket text, acceptance criteria, and step-by-step hints live in the
matching day's README under [`./student-guides/`](./student-guides/README.md).

---

## Daily flow

| Day | Theme | New Tickets | Headline new-2026 topic |
|----:|-------|-------------|--------------------------|
| 0   | Introduction & onboarding | — | — |
| 1   | PostgreSQL + Liquibase Deep Dive | ADV001–ADV017 | ★ Liquibase, ★ AI for ADR |
| 2   | Java OOP + sealed classes + SOLID | ADV018–ADV032 | sealed-class trade hierarchy |
| 3   | Functional Java + JUnit 5 + Testcontainers | ADV033–ADV047 | parallel recon with CompletableFuture |
| 4   | Spring Boot enterprise setup | ADV048–ADV062 | multi-module Maven, Hibernate Envers, MapStruct |
| 5   | REST + JWT + RBAC + Testcontainers tests | ADV063–ADV080 | API versioning |
| 6   | Caching + Prometheus + Grafana | ADV081–ADV097 | ★ Observability deep dive |
| 7   | HTML5 + CSS Grid + SSE feed + ARIA | ADV098–ADV110 | ★ live SSE trade feed |
| 8   | JS ES6+ + React patterns (HOC, hooks, RHF) | ADV111–ADV125 (+ ADV127 stretch) | React performance profiling |
| 9   | React Context + Kafka multi-topic + DLQ | ADV128–ADV145 | ★ Kafka deep dive, event sourcing |
| 10  | Docker (7-svc) + GH Actions + load test + demo | ADV146–ADV165 | ★ Liquibase-in-CI, ★ AI in DevOps |

---

## Branching

Use **GitFlow**:

```
main      ← only release tags (v1.0.0 at end of Day 10)
develop   ← integration branch — your team merges here
feature/* ← one branch per ticket (e.g. feature/ADV019-equity-builder)
```

Open a Pull Request from each `feature/*` branch into `develop`. Two approvals
required before merge (advanced track convention — Intermediate only required
one).

---

## Final demo (Day 10)

A 20-minute end-to-end walkthrough:

| Minutes | Content |
|--------:|---------|
| 3       | Problem statement + C4 architecture diagram |
| 8       | Live demo: JWT login → post trade → Kafka event → auto-recon → resolve break → Grafana metric ticks |
| 5       | Code walkthrough (one feature each team member is proud of) |
| 4       | Q&A |

---

## Good luck — and ask your instructors anything 🏦
