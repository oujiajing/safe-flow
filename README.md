# SafeTeam Portfolio Edition

SafeTeam is a safety-production workflow system that turns two high-risk field processes into traceable, role-aware digital workflows:

1. One-shift three-checks: team dispatch → pre-shift meeting → pre-shift inspection → mid-shift inspection → post-shift inspection.
2. Hazard rectification: report → assign → rectify → submit → accept/reject → close.

This repository is an independent portfolio candidate derived from the audited `codex/pingan-integrated` snapshot. It is intended for local technical review only. It does not represent production deployment, customer adoption, performance results, or authorization to publish the original private project.

## What this demonstrates

- Spring Boot + PostgreSQL + Flyway backend with explicit workflow states, organization data scope, JWT authentication, attachments, status history, idempotency and optimistic-concurrency checks.
- Vue Vben Admin PC application with workflow workbenches and permission-aware navigation.
- Native WeChat mini program sharing the same backend facts and workflow contracts.
- Product modeling for field responsibility, evidence collection, cross-device synchronization and closed-loop remediation.

## Repository map

| Path | Purpose |
| --- | --- |
| `backend/` | Spring Boot API, domain services, migrations and tests |
| `vben/apps/web-antd/` | SafeTeam PC application |
| `mini-program/` | WeChat mini program |
| `tools/three-check-generator/` | Three-check module templates and generator |
| `docs/` | Portfolio-facing audit, architecture, flow and verification notes |

## Quick start status

The portfolio candidate is currently `PARTIAL`, not READY. Backend compile/tests, Demo account lifecycle, isolated PostgreSQL startup, PC dependency installation/build/typecheck, PC Demo login and selected PC page actions are verified. The PC workflows are not yet complete end-to-end. The mini-program client is included as engineering evidence; a real WeChat runtime session is outside the verification scope of this public portfolio edition.

Do not connect this candidate to any production or enterprise service. Use only a new local Demo database and local attachment storage. See [docs/PORTFOLIO_AUDIT.md](docs/PORTFOLIO_AUDIT.md) and [PORTFOLIO_READINESS.md](PORTFOLIO_READINESS.md).

## Demo account flow

After the local Demo PostgreSQL schema is initialized and the backend dependencies have been built, set `PINGAN_DB_URL`, `PINGAN_DB_USERNAME` and `PINGAN_DB_PASSWORD` in the current PowerShell session, then run:

```powershell
.\scripts\init-demo.ps1 -ConfirmDemo
```

The command creates `portfolio-admin` only in a localhost `demo_safeteam` database and displays a newly generated password once. Re-running it does not overwrite or display the existing password. To explicitly rotate that local account:

```powershell
.\scripts\reset-demo.ps1 -ConfirmDemo
```

Generated passwords are not written to Git, README files or logs. The scripts refuse non-local or non-Demo database URLs.

## Feature Evidence

| Capability | Implementation | Backend/API verification | Page-level E2E | Overall |
| --- | --- | --- | --- | --- |
| One-shift three-checks | Implemented | PASS: five backend flow stages; PC dispatch submit changed `未生效` to `已生效` | PARTIAL: matching meeting form had no selectable Demo team; remaining page actions NOT_RUN | PARTIAL |
| Hazard rectification | Implemented | PASS: real database order reached `CLOSED`; PC create and issue changed `待派发` to `待整改` | PARTIAL: after-photo upload was not accepted by the browser session; completion actions NOT_RUN | PARTIAL |
| Authentication and invalid transitions | Implemented | PASS for tested login, 401 unauthenticated access and 400 closed-order action | PARTIAL: page action role matrix not fully run | PARTIAL |
| Mini-program client | Implemented | PASS: 241/241 tests and API contracts | NOT_INCLUDED_IN_PORTFOLIO_SCOPE: no real WeChat runtime claim | PASS for included evidence |

The evidence above deliberately separates implementation from verification. It does not claim production deployment, customer adoption or complete page-level E2E. The mini-program client implementation and automated tests are included as engineering evidence. A real WeChat runtime session is outside the verification scope of this public portfolio edition. Four real PC screenshots are retained under `screenshots/`.

For the Phase A2 evidence reconciliation, see [docs/EVIDENCE_RECONCILIATION.md](docs/EVIDENCE_RECONCILIATION.md).

## Third-party attribution

The PC application is based on Vue Vben Admin. Its MIT license and upstream attribution must remain with the candidate. Business-specific code, workflow modeling and verification notes are documented separately; framework code is not claimed as original work.
