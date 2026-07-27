# TICKET-ADV016 — ReconX Day 1 Delivery Board Setup

## Why this file exists
GitHub Project automation could not be completed from this environment due to access restrictions (GitHub Projects API/CLI blocked). Manual project creation is required.

## Project definition
- **Project name:** `ReconX Day 1 Delivery Board`
- **Board columns / Status values:**
  - Backlog
  - To Do
  - In Progress
  - In Review
  - Done

## Epics

### RECONX-E1 — Day 1: Architecture & Setup
**Purpose:** Establish repository governance, C4 diagrams, and architecture documentation.
- TICKET-ADV001 — Create GitHub repo with branch protection
- TICKET-ADV002 — Design C4 Context diagram
- TICKET-ADV003 — Design C4 Container diagram
- TICKET-ADV004 — Design C4 Component diagram

### RECONX-E2 — Day 1: Schema & Analytics
**Purpose:** Design the reconciliation data model and Day 1 analytical SQL foundations.
- TICKET-ADV006 — Design ER model
- TICKET-ADV007 — CREATE TABLE with monthly partitioning
- TICKET-ADV008 — Materialised view: mv_daily_recon_summary
- TICKET-ADV009 — Add JSONB column to instruments
- TICKET-ADV010 — Window Function: VWAP per instrument per day
- TICKET-ADV011 — Recursive CTE: trade lifecycle rollup

### RECONX-E3 — Day 1: Liquibase & Tooling
**Purpose:** Establish Liquibase governance, rollback safety, ADRs, project tracking, and seed data.
- TICKET-ADV012 — Liquibase master changelog
- TICKET-ADV013 — Add rollback tags
- TICKET-ADV014 — Add preconditions
- TICKET-ADV015 — Use Claude to generate ADRs
- TICKET-ADV016 — Set up Jira / Kanban with epics
- TICKET-ADV017 — Seed data: 10 counterparties, 50 instruments, 500 trades

## Field schema (create custom fields if available)
- **Exercise ID**: text
- **Estimate**: single select (`1`, `2`, `3`, `5`, `8`)
- **Owner**: assignee
- **Linked PR**: text
- **Epic**: text (or parent-link field)
- **Status**: single select (`Backlog`, `To Do`, `In Progress`, `In Review`, `Done`) or mapped board column

## Story-point estimates
- ADV001: 2
- ADV002: 2
- ADV003: 3
- ADV004: 3
- ADV006: 3
- ADV007: 8
- ADV008: 5
- ADV009: 3
- ADV010: 2
- ADV011: 3
- ADV012: 5
- ADV013: 2
- ADV014: 3
- ADV015: 2
- ADV016: 2
- ADV017: 5

## Initial status mapping
- **Done:** TICKET-ADV001, TICKET-ADV002, TICKET-ADV003, TICKET-ADV004, TICKET-ADV006, TICKET-ADV007, TICKET-ADV008, TICKET-ADV015
- **In Progress:** TICKET-ADV016
- **To Do:** TICKET-ADV009, TICKET-ADV010, TICKET-ADV011, TICKET-ADV012, TICKET-ADV013, TICKET-ADV014, TICKET-ADV017

## Note about Day 1 ticket list
- **ADV005 is not present in the supplied Day 1 student guide.**
- This board tracks the published Day 1 tickets: **ADV001–ADV004 and ADV006–ADV017**.

## Card checklist to create manually
For each card, set:
- Exercise ID
- Epic
- Estimate
- Owner: `Unassigned` (unless known)
- Linked PR: placeholder (`TBD`)
- Status (from initial status mapping above)
- Acceptance criteria (from Day 1 guide)

## Full card register (copy into GitHub Project)

### RECONX-E1 — Day 1: Architecture & Setup

#### TICKET-ADV001 — Create GitHub repo with branch protection
- Exercise ID: TICKET-ADV001
- Epic: RECONX-E1
- Estimate: 2
- Owner: Unassigned
- Linked PR: TBD
- Initial Status: Done
- Acceptance criteria:
  - Repo is private and team members push only to feature branches.
  - Direct push to `main` is blocked; merges go through PRs.
  - `main` requires 2 approvals, stale approval dismissal, and required checks.
  - `.github/CODEOWNERS` routes backend/frontend/db ownership.

#### TICKET-ADV002 — Design C4 Context diagram
- Exercise ID: TICKET-ADV002
- Epic: RECONX-E1
- Estimate: 2
- Owner: Unassigned
- Linked PR: TBD
- Initial Status: Done
- Acceptance criteria:
  - Context diagram shows one ReconX system with external actors/systems only.
  - Relationships are labelled with protocol and intent.
  - Artifact committed under `db/diagrams/` or `docs/architecture/`.

#### TICKET-ADV003 — Design C4 Container diagram
- Exercise ID: TICKET-ADV003
- Epic: RECONX-E1
- Estimate: 3
- Owner: Unassigned
- Linked PR: TBD
- Initial Status: Done
- Acceptance criteria:
  - ReconX boundary contains SPA, API, recon engine, DB, Kafka, observability stack.
  - Arrows specify protocol and intent.
  - External context actors/systems remain outside the boundary.

#### TICKET-ADV004 — Design C4 Component diagram
- Exercise ID: TICKET-ADV004
- Epic: RECONX-E1
- Estimate: 3
- Owner: Unassigned
- Linked PR: TBD
- Initial Status: Done
- Acceptance criteria:
  - Diagram title states the target container.
  - ~10–15 components grouped by Spring stereotypes.
  - UI/Postgres/Kafka shown as external referenced containers.

### RECONX-E2 — Day 1: Schema & Analytics

#### TICKET-ADV006 — Design ER model
- Exercise ID: TICKET-ADV006
- Epic: RECONX-E2
- Estimate: 3
- Owner: Unassigned
- Linked PR: TBD
- Initial Status: Done
- Acceptance criteria:
  - Exactly 8 entities are modelled.
  - FK arrows and `trades` partition-key column are shown.
  - Diagram committed at `db/erd.md` (or equivalent) and reproducible.

#### TICKET-ADV007 — CREATE TABLE with monthly partitioning
- Exercise ID: TICKET-ADV007
- Epic: RECONX-E2
- Estimate: 8
- Owner: Unassigned
- Linked PR: TBD
- Initial Status: Done
- Acceptance criteria:
  - `trades` is range-partitioned by `trade_date` with visible child partitions.
  - Explain plan for date filter shows partition pruning.
  - Partitions include Apr–Jul 2026 plus `trades_default` catch-all.

#### TICKET-ADV008 — Materialised view: mv_daily_recon_summary
- Exercise ID: TICKET-ADV008
- Epic: RECONX-E2
- Estimate: 5
- Owner: Unassigned
- Linked PR: TBD
- Initial Status: Done
- Acceptance criteria:
  - `mv_daily_recon_summary` exposes daily recon summary fields.
  - Unique index on `(trade_date, region, asset_class)` enables concurrent refresh.
  - Implemented via Liquibase versioned `<sql>` change.

#### TICKET-ADV009 — Add JSONB column to instruments
- Exercise ID: TICKET-ADV009
- Epic: RECONX-E2
- Estimate: 3
- Owner: Unassigned
- Linked PR: TBD
- Initial Status: To Do
- Acceptance criteria:
  - `instruments.metadata` exists as `jsonb NOT NULL DEFAULT '{}'::jsonb`.
  - GIN index exists on `metadata`.
  - Containment query uses the GIN index.

#### TICKET-ADV010 — Window Function: VWAP per instrument per day
- Exercise ID: TICKET-ADV010
- Epic: RECONX-E2
- Estimate: 2
- Owner: Unassigned
- Linked PR: TBD
- Initial Status: To Do
- Acceptance criteria:
  - Query returns one row per trade with qty, price, notional, and VWAP.
  - VWAP uses windowed `SUM(price*qty)/SUM(qty)` by `(instrument_id, trade_date)`.
  - SQL committed in `db/queries.sql` and runs on seeded data.

#### TICKET-ADV011 — Recursive CTE: trade lifecycle rollup
- Exercise ID: TICKET-ADV011
- Epic: RECONX-E2
- Estimate: 3
- Owner: Unassigned
- Linked PR: TBD
- Initial Status: To Do
- Acceptance criteria:
  - Recursive CTE has clear anchor and recursive step.
  - Includes termination guard.
  - Result includes trade_id, stage, stage_name, event_at, event_status.

### RECONX-E3 — Day 1: Liquibase & Tooling

#### TICKET-ADV012 — Liquibase master changelog
- Exercise ID: TICKET-ADV012
- Epic: RECONX-E3
- Estimate: 5
- Owner: Unassigned
- Linked PR: TBD
- Initial Status: To Do
- Acceptance criteria:
  - `db.changelog-master.xml` exists and parses cleanly with correct schema refs.
  - Each business object has its own numbered chapter file.
  - `application.yml` points Liquibase to the master changelog and app starts.

#### TICKET-ADV013 — Add rollback tags
- Exercise ID: TICKET-ADV013
- Epic: RECONX-E3
- Estimate: 2
- Owner: Unassigned
- Linked PR: TBD
- Initial Status: To Do
- Acceptance criteria:
  - Every `<sql>` changeset has matching rollback logic.
  - At least one release tag (e.g., `release-1.0`) exists.
  - `liquibase:rollbackSQL` for the tag runs and emits valid reverse DDL.

#### TICKET-ADV014 — Add preconditions
- Exercise ID: TICKET-ADV014
- Epic: RECONX-E3
- Estimate: 3
- Owner: Unassigned
- Linked PR: TBD
- Initial Status: To Do
- Acceptance criteria:
  - Idempotent schema changesets include existence preconditions.
  - Env-gated data changes use `context="dev,test"` plus row-existence checks.
  - Correct `onFail` mode is used for each precondition.

#### TICKET-ADV015 — Use Claude to generate ADRs
- Exercise ID: TICKET-ADV015
- Epic: RECONX-E3
- Estimate: 2
- Owner: Unassigned
- Linked PR: TBD
- Initial Status: Done
- Acceptance criteria:
  - At least 3 ADRs exist under `docs/adr/0001-*.md`, `0002-*.md`, `0003-*.md`.
  - Each ADR uses Nygard sections (Title, Status, Context, Decision, Consequences).
  - ADR prompt template is committed at `docs/adr/README.md`.

#### TICKET-ADV016 — Set up Jira / Kanban with epics
- Exercise ID: TICKET-ADV016
- Epic: RECONX-E3
- Estimate: 2
- Owner: Unassigned
- Linked PR: TBD
- Initial Status: In Progress
- Acceptance criteria:
  - At least 3 epics cover Day 1 exercises.
  - One card exists per exercise with ID, owner, estimate, acceptance criteria.
  - Board uses Backlog/To Do/In Progress/In Review/Done; at least one card moves end-to-end.

#### TICKET-ADV017 — Seed data: 10 counterparties, 50 instruments, 500 trades
- Exercise ID: TICKET-ADV017
- Epic: RECONX-E3
- Estimate: 5
- Owner: Unassigned
- Linked PR: TBD
- Initial Status: To Do
- Acceptance criteria:
  - Seed counts: 10 counterparties, 50 instruments, 500 trades.
  - Trades spread roughly evenly across Apr–Jul 2026 partitions.
  - Seed is FK-safe and gated away from production.

## Manual setup steps (copy/paste playbook)
1. Create a new GitHub Project named **ReconX Day 1 Delivery Board**.
2. Configure status values/columns: **Backlog, To Do, In Progress, In Review, Done**.
3. Add custom fields: **Exercise ID** (text), **Estimate** (single-select 1/2/3/5/8), **Owner** (assignee), **Linked PR** (text), **Epic** (text or parent), **Status**.
4. Create the three epics exactly as listed above.
5. Create one card for each published Day 1 ticket: **ADV001–ADV004 and ADV006–ADV017**.
6. For each card, populate Exercise ID, Epic, Estimate, Owner (`Unassigned` unless known), Linked PR (`TBD`), Status, and acceptance criteria from this file.
7. Apply initial statuses exactly as listed in the Initial status mapping section.
