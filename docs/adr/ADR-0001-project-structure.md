# ADR-0001 — Project Structure

| Field | Value |
|-------|-------|
| Status | Accepted |
| Date | 2026-07-07 |

## Context

Arbitrier needs a repository structure that separates backend services, frontend code, infrastructure, contracts, and documentation.

## Decision

Use the repository layout documented in `README.md`: `server/` for services and shared contracts, `client/` for the React application, `docs/` for OKF/RF/RNF/ADR/test-case material, `infra/` for local and deployable infrastructure, and `.github/workflows/` for CI.

## Consequences

- Backend service boundaries are visible from the filesystem.
- Documentation has a stable home before implementation.
- Infrastructure and contracts can evolve without being hidden inside service modules.

## Open Questions

- OPEN QUESTION: Exact module build wiring will be documented when production code is introduced.
