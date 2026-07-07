# client

React 19 / TypeScript corporate portal for the Arbitrier platform.

## Responsibility

- Corporate buyer UI: submit bulk orders, track order status.
- Human approver UI: review and accept/reject partial backorder proposals.
- Admin UI: manage organizations, credit limits, inventory visibility.

## Planned Stack

| Tool          | Purpose                                    |
|---------------|--------------------------------------------|
| React 19      | UI framework (TypeScript strict mode)      |
| Vite          | Build tool and dev server                  |
| Keycloak JS   | OIDC authentication (auth code + PKCE)     |
| Playwright    | E2E test suite                             |

## Planned Structure

```
client/
├── src/
│   ├── pages/          # Route-level components
│   ├── features/       # Feature slices (orders, approvals, admin)
│   ├── components/     # Shared UI components
│   ├── api/            # REST client (generated from OpenAPI specs)
│   ├── auth/           # Keycloak adapter setup
│   └── main.tsx        # App entry point
├── e2e/                # Playwright test specs
│   ├── uc01-confirmed.spec.ts        # TC-UC01-E01
│   ├── uc01-partial-accepted.spec.ts # TC-UC01-E02
│   └── uc01-partial-rejected.spec.ts # TC-UC01-E03
├── public/
├── index.html
├── vite.config.ts
├── tsconfig.json
└── playwright.config.ts
```

## Status

`ARB-001` — Structure placeholder. No implementation yet.
