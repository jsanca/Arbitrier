Task: ARB-UI-001 — Customer Portal React Prototype

Status:
[COMPLETE]

Owner:
Brio

Context:
The repository contains a customer portal UX package under docs/ui:

- UX strategy and navigation map
- reference image for each screen
- reference HTML for each screen

The backend does not yet expose REST inbound adapters for the customer portal.

This task implements the Customer Portal in React using local JSON mocks and frontend service abstractions.

Goal:
Create a coherent, navigable React prototype matching the approved UX direction, while keeping data access replaceable when REST APIs are introduced.

Primary scope:
client/

Reference sources:
- docs/ui/ux_strategy_navigation_map.md
- all screen images under docs/ui
- all reference HTML files under docs/ui

Do not redesign from scratch.
Use the reference screens as the visual baseline, applying the refinements listed below.

--------------------------------------------------

1. Frontend architecture

Use the existing client stack and conventions.

Create a clean structure such as:

src/
app/
components/
features/
auth/
dashboard/
orders/
company/
profile/
services/
mocks/
models/
routes/

Keep UI components separate from data access.

Do not call fetch() directly from page components.

Define frontend service interfaces such as:

- AuthService
- CustomerDashboardService
- ProductCatalogService
- OrderPreparationService
- OrderService
- CompanyService

Provide mock implementations backed by local JSON.

Future REST implementations must be swappable without rewriting screens.

--------------------------------------------------

2. Routing

Implement routes for:

- /login
- /dashboard
- /orders
- /orders/new
- /orders/review
- /orders/submitting
- /orders/:orderId
- /company
- /profile

Add a simple authenticated layout with:

- sidebar
- top navigation
- current user/company area

Mock authentication is acceptable.

--------------------------------------------------

3. Login

Implement the reference login screen.

Important:
- Present a single login experience.
- Keycloak SSO button may remain visual/mock.
- Do not implement real Keycloak integration.
- Successful mock login redirects to dashboard.

Remove or avoid unsupported compliance claims unless clearly marked as placeholder.
Examples:
- SOC2 Certified
- uptime percentage
- security audit claims

Do not present fictional certifications as real production facts.

--------------------------------------------------

4. Dashboard

Implement the dashboard visual language from the reference.

Keep the primary focus on:

- available company credit
- active orders
- recent orders
- alerts requiring attention
- prominent New Order action

Simplify the page compared with the first mockup:

Move or de-emphasize:
- View Analytics
- Download Report
- inventory marketing card
- excessive delivery telemetry

The dashboard should answer:

“What needs my attention, and what is happening with my company?”

Add support for:
- draft orders
- active orders
- recent completed orders

Use mock JSON data.

--------------------------------------------------

5. Create New Order

Implement:

- product search
- quick-add products
- draft item list
- quantity increment/decrement
- remove line
- order summary
- delivery address placeholder
- delivery option placeholder
- Save as Draft
- Review Order

Use mock product catalog JSON.

Do not expose warehouse selection.

SKU may be shown as secondary information, but product name should remain primary.

--------------------------------------------------

6. Availability Review

Implement the ARB-017 pre-saga negotiation screen.

Use columns:

- Product
- Requested
- Available Now
- Decision / Status

Support outcomes:

- fully available
- partially available
- unavailable

For partial lines, provide explicit customer actions such as:

- Accept available quantity
- Change quantity
- Remove item

Do not expose:
- saga
- Kafka
- warehouse allocation
- compensation
- internal retry logic

Update the title to remove internal task identifiers.

Use:
“Availability Review”

Do not show:
“Availability Review (ARB-017)”

The screen must make clear that availability is advisory and may change before confirmation, using customer-friendly wording.

--------------------------------------------------

7. Submitting Order

Implement a business-level progress screen.

Use customer-facing milestones:

- Checking product availability
- Validating company credit
- Preparing confirmation

Avoid:
- Inventory reserved
- Saga started
- Credit event received
- technical status names

The screen must not depend on a real asynchronous backend yet.

Use a mock state sequence or timer only for demonstration.

Do not imply that the browser must remain open for the backend process to succeed.
Remove wording like:
“Please do not close this window.”

Prefer:
“You may leave this page. We’ll keep processing your order.”

--------------------------------------------------

8. Submission acknowledgement and final result

Separate two concepts.

A. Order Submitted / Received

Show:
- reference ID
- order summary
- processing has started
- View Order Status
- Back to Dashboard

Do not label it:
- Processed
- Confirmed

unless the mock order is actually in the final confirmed state.

B. Final Order Result

Add a new screen/state for:

- Order Confirmed
- Order Rejected
- Order Cancelled
- Partial quantity accepted, if applicable

Provide clear next actions.

This new result screen may share the order-detail route or use a dedicated component/state.

--------------------------------------------------

9. Order Detail

Implement:

- order summary
- business-level timeline
- quantities and totals
- company delivery/billing details placeholders
- Contact Support action
- Download Invoice placeholder only when status permits it

Use business milestone labels:

- Order received
- Availability confirmed
- Company credit approved
- Order confirmed
- Preparing delivery

Do not show:
- Kafka event names
- saga steps
- compensation states
- warehouse names unless the business explicitly needs them
- cryptographic chain-of-custody claims

Remove or replace unsupported features from the mockup:
- Manifest Authentication
- Digital Certificate
- real-time logistics network claims

These can become neutral placeholders only if clearly labeled as future features.

--------------------------------------------------

10. Company screen

Create the missing Company screen.

Include mock sections for:

- company profile
- available credit and credit limit
- billing contact
- shipping addresses
- payment terms
- authorized buyers

The screen is read-only for this prototype unless simple edit interactions are trivial.

--------------------------------------------------

11. Profile screen

Create a basic Profile screen:

- user details
- notification preferences
- language placeholder
- sign out

Use mock data.

--------------------------------------------------

12. Orders list

Create an Orders screen containing:

- drafts
- active orders
- completed orders
- cancelled/rejected orders

Support simple filtering by status and text search.

Each row should link to Order Detail.

--------------------------------------------------

13. Mock data

Create realistic local JSON fixtures for:

- authenticated user
- company
- credit summary
- products
- draft order
- active orders
- completed orders
- availability review
- submitting order progress
- final order outcomes
- notifications

Do not hardcode large data objects directly in components.

Mocks should use stable IDs and consistent relationships.

--------------------------------------------------

14. States

Every principal page must support:

- loading
- empty
- success
- error

At minimum, create reusable components for:

- LoadingState
- EmptyState
- ErrorState
- StatusBadge
- BusinessTimeline
- CreditSummary
- OrderLineTable

--------------------------------------------------

15. Responsive behavior

Primary:
- desktop

Secondary:
- tablet

Ensure:
- sidebar does not break smaller widths
- order tables remain usable
- order summary stacks appropriately
- availability review remains understandable

Mobile-perfect design is not required in this slice.

--------------------------------------------------

16. Accessibility

- semantic headings
- labeled form controls
- keyboard-operable buttons
- visible focus states
- adequate contrast
- do not encode status using color alone
- meaningful alt text for relevant images

--------------------------------------------------

17. Testing

Add appropriate frontend tests for:

- navigation
- mock login
- create-order quantity changes
- availability partial acceptance
- submission acknowledgement vs final confirmation distinction
- orders list filtering
- principal empty/error states

Do not require a backend.

--------------------------------------------------

18. Documentation

Create:

docs/implementation/ARB-UI-001-customer-portal-react-prototype.md

Document:

- project structure
- routes
- mock-service architecture
- available screens
- deviations from Stitch mockups
- future REST integration points
- known open questions

Update client/README.md with run and test instructions.

--------------------------------------------------

Out of scope

- No real REST integration.
- No Keycloak integration.
- No Kafka.
- No WebSocket/SSE.
- No backend changes.
- No admin/operations console.
- No email notifications.
- No invoice generation.
- No analytics implementation.
- No real reports.
- No shipping provider integration.
- No production security claims.
- No ARB infrastructure work.

--------------------------------------------------

Acceptance Criteria

- React application builds and runs.
- All customer portal routes are navigable.
- Reference visual language is preserved.
- UX refinements are applied.
- Company and final-result screens are added.
- Mock JSON drives all screens.
- Components do not directly depend on REST.
- No backend or infrastructure code is introduced.
- Tests pass.
- Documentation is complete.
- Ready for visual and architectural review.

After completion:

Report:
- created and modified files
- routes
- components
- mock fixtures
- tests run
- known gaps

Do not implement the admin UI.
