# ARB-UI-001 — Customer Portal React Prototype Implementation Note

| Field  | Value                                  |
|--------|----------------------------------------|
| Task   | ARB-UI-001                            |
| Status | Implemented                            |
| Date   | 2026-07-10                            |

## Project Structure

```
client/
├── src/
│   ├── app/                      # Application entry point
│   ├── components/                # Shared UI primitives
│   │   ├── AuthenticatedLayout.tsx   # Sidebar + topnav shell
│   │   ├── BusinessTimeline.tsx       # Order status stepper
│   │   ├── CreditSummary.tsx          # Credit utilization bar
│   │   ├── EmptyState.tsx
│   │   ├── ErrorState.tsx
│   │   ├── LoadingState.tsx
│   │   ├── OrderLineTable.tsx         # Line-item breakdown
│   │   └── StatusBadge.tsx            # Order status chip
│   ├── features/
│   │   ├── auth/Login.tsx
│   │   ├── company/Company.tsx
│   │   ├── dashboard/Dashboard.tsx
│   │   ├── orders/
│   │   │   ├── AvailabilityReview.tsx  # ARB-017 negotiation screen
│   │   │   ├── NewOrder.tsx
│   │   │   ├── OrderDetail.tsx
│   │   │   ├── OrdersList.tsx
│   │   │   ├── SubmissionOutcome.tsx   # Order submitted + final result
│   │   │   └── SubmittingOrder.tsx    # Progress screen
│   │   └── profile/Profile.tsx
│   ├── mocks/                    # Local JSON fixtures (no backend)
│   │   ├── auth.json
│   │   ├── company.json
│   │   ├── orders.json
│   │   └── products.json
│   ├── models/types.ts           # TypeScript interfaces
│   ├── routes/index.tsx          # React Router v7 definition
│   ├── services/
│   │   ├── interfaces.ts         # Service interface contracts
│   │   └── mockServices.ts       # LocalStorage-backed implementations
│   └── test/
│       ├── components.test.tsx
│       ├── features.test.tsx
│       └── setup.ts
├── public/
├── package.json
└── vite.config.ts
```

## Routes

| Path | Component | Description |
|------|-----------|-------------|
| `/login` | `Login` | Corporate login (mock SSO) |
| `/dashboard` | `Dashboard` | Credit, alerts, active/draft/recent orders |
| `/orders` | `OrdersList` | Full order history with filtering |
| `/orders/new` | `NewOrder` | Product search, cart, draft save |
| `/orders/review` | `AvailabilityReview` | ARB-017 availability negotiation |
| `/orders/submitting` | `SubmittingOrder` | Submission progress milestones |
| `/orders/outcome` | `SubmissionOutcome` | Acknowledgement + final result |
| `/orders/:orderId` | `OrderDetail` | Order timeline, line items, support |
| `/company` | `CompanyPage` | Company profile, credit, addresses |
| `/profile` | `Profile` | User settings, notifications, sign out |

## Mock Service Architecture

All data access flows through typed service interfaces. Mock implementations use browser `localStorage` as a persistence layer. This allows the entire prototype to run without a backend while preserving state across page navigations.

### Service Interfaces (`services/interfaces.ts`)

```typescript
AuthService               // login, logout, getCurrentUser
CustomerDashboardService   // credit, active orders, alerts
ProductCatalogService     // search, getAll
OrderPreparationService   // getDraftOrder, saveDraftOrder
OrderService              // getOrders, getOrderById, submitOrder, respondToPartialProposal
CompanyService            // getCompany
```

### Mock Implementations (`services/mockServices.ts`)

- `MockAuthService`: Stores user in localStorage; accepts any email/password
- `MockCustomerDashboardService`: Reads company/orders from localStorage; computes alerts
- `MockProductCatalogService`: Filters products.json by name/SKU/category
- `MockOrderPreparationService`: Drafts stored as DRAFT-status orders
- `MockOrderService`: Core submission simulation; deterministic partial-availability rules (prod-3 qty > 2 triggers partial); credit deduction on CONFIRMED
- `MockCompanyService`: Returns stored company

### Data Initialization (`initDB()`)

Called on module load. Populates localStorage from JSON fixtures if keys are absent. Ensures stable IDs and consistent relationships.

## Available Screens

### Login (`/login`)
- Email + password form
- Keycloak SSO mock (visual only)
- Prototype disclaimer footer
- Redirects to `/dashboard` on success

### Dashboard (`/dashboard`)
- Company credit summary (available vs. limit)
- Critical alerts panel (low credit, pending decisions)
- Active orders table (non-draft, non-terminal)
- Saved drafts panel
- Recent completed orders table
- Prominent "Create New Order" CTA

### New Order (`/orders/new`)
- Product catalog with search (name, SKU, category)
- Quick-add to draft cart
- Quantity increment/decrement per line
- Remove line
- Shipping address selector
- Delivery option (Standard/Expedited)
- Order summary with totals
- Save as Draft + Review Order actions

### Availability Review (`/orders/review`)
- Columns: Product | Requested | Available Now | Status | Actions
- Fully available: auto-confirmed
- Partially available: Accept Available + quantity editor + Remove
- Unavailable: Remove only
- Advisory notice: availability is provisional
- Confirm & Submit Purchase action

### Submitting Order (`/orders/submitting`)
- Animated spinner
- Business milestone steps: Checking availability → Validating credit → Preparing confirmation
- Non-blocking notice: "You may leave this page."

### Submission Outcome (`/orders/outcome`)
- Separate acknowledgment screen (reference ID, order summary)
- Final result states: CONFIRMED, PARTIALLY_CONFIRMED, REJECTED, CANCELLED
- Next actions per state
- Order line details

### Order Detail (`/orders/:orderId`)
- Business timeline (Order received → Availability confirmed → Credit approved → Order confirmed → Preparing delivery)
- Line item table with totals
- Shipping/billing info
- Contact Support action
- Download Invoice (only for CONFIRMED/PARTIALLY_CONFIRMED)
- Placeholder security features (Manifest Verification, Chain-of-Custody)

### Orders List (`/orders`)
- Status tabs: All, Drafts, Active, Completed, Cancelled
- Text search by reference ID, order ID, product name
- Row click → Order Detail

### Company (`/company`)
- Credit summary banner
- Corporate profile (name, account ID, payment terms)
- Billing contact
- Authorized buyers
- Shipping destinations

### Profile (`/profile`)
- User card (name, email, role)
- Notification toggles (email confirmations, credit warnings, backorder alerts)
- Language selector (placeholder)
- Sign Out

## Deviations from Stitch Mockups

| Topic | Decision |
|-------|----------|
| Dashboard: View Analytics, Download Report, inventory card, excessive delivery telemetry | Removed — out of scope per task item 4 |
| Login: SOC2 certification claims | Removed — replaced with prototype mode disclaimer |
| Submitting: "Please do not close this window" | Replaced with "You may leave this page. We'll keep processing your order." |
| Order Detail: Manifest Authentication, Digital Certificate | Replaced with neutral "Future Security Features" placeholder |
| Availability Review: Title "(ARB-017)" | Removed per task item 5 |
| Submitting: Technical milestone names (Inventory reserved, Saga started) | Replaced with business-level milestones |
| SubmissionAcknowledgement vs FinalResult | Separated into two distinct screens/states per task item 8 |

## Future REST Integration Points

When real REST APIs are introduced, only `services/interfaces.ts` and `services/mockServices.ts` need changes. No feature component should call `fetch()` directly.

| Service | Mock Method | REST Endpoint (expected) |
|---------|-------------|-------------------------|
| `AuthService` | `login()` | `POST /auth/login` |
| `CustomerDashboardService` | `getAvailableCredit()` | `GET /companies/{id}/credit` |
| `ProductCatalogService` | `searchProducts()` | `GET /products?search=` |
| `OrderService` | `submitOrder()` | `POST /orders` |
| `OrderService` | `respondToPartialProposal()` | `PATCH /orders/{id}/partial-response` |

## Known Gaps

| Gap | Impact | Mitigation |
|-----|--------|------------|
| No real Keycloak/OIDC integration | Auth is mock only | `MockAuthService` preserves session in localStorage |
| No real Kafka/backend | Order submission is simulated | `MockOrderService` has deterministic mock logic |
| No WebSocket/SSE | No real-time updates | Dashboard refreshes on navigation |
| No responsive/mobile-perfect design | Tablet only per task item 15 | Sidebar collapses to drawer on narrow viewports |
| No actual invoice generation | Invoice download is alert-only | Placeholder; real PDF generation deferred |
| Credit deduction is local-only | Company credit not persisted server-side | Mock updates localStorage only |

## Tests

| Test File | Coverage |
|-----------|----------|
| `components.test.tsx` | StatusBadge, LoadingState, EmptyState, ErrorState, CreditSummary, OrderLineTable, BusinessTimeline |
| `features.test.tsx` | Login flow, New Order cart, Availability Review, Orders list filtering |

Run: `cd client && npm test`
