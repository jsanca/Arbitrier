# Arbitrier Customer Portal

React 19 + TypeScript + Vite prototype for the Arbitrier B2B customer portal.

No backend required — all data is served from local mock services backed by browser localStorage.

---

## Run

```bash
npm ci
npm run dev
```

Open http://localhost:5173

**Default login**: `brio@arbitrier.com` (any password)

---

## Build

```bash
npm run build   # production build → dist/
```

Requires no external services.

---

## Test

```bash
npm test        # run once
npm test -- --watch  # watch mode
```

Tests use Vitest + jsdom. No backend, Docker, or external services needed.

---

## Stack

| Concern | Technology |
|---------|------------|
| Framework | React 19 + TypeScript (strict) |
| Bundler | Vite 8 |
| Styling | Tailwind CSS 4 + Material Symbols |
| Routing | React Router v7 |
| Testing | Vitest + Testing Library |
| Linting | Oxlint |
| State | localStorage (mock services) |

---

## Architecture

All data access goes through typed service interfaces in `src/services/`. Mock implementations (`mockServices.ts`) are swappable with real REST adapters — components never call `fetch()` directly.

See [`docs/implementation/ARB-UI-001-customer-portal-react-prototype.md`](../docs/agents/reports/ARB-UI-001-customer-portal-react-prototype.md) for full implementation details.

---

## Routes

| Path | Screen |
|------|--------|
| `/login` | Login |
| `/dashboard` | Dashboard |
| `/orders` | Orders list |
| `/orders/new` | Create new order |
| `/orders/review` | Availability review (ARB-017) |
| `/orders/submitting` | Submission progress |
| `/orders/outcome` | Submission acknowledgement + final result |
| `/orders/:orderId` | Order detail |
| `/company` | Company profile |
| `/profile` | User settings |

The current application is the Customer Portal only. An internal Admin Console is a future product surface, not a hidden route or partially implemented feature in this prototype.
