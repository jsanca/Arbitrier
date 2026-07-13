-- Local-only synthetic seed data for Arbitrier development.
-- Run after all services have started and Flyway migrations have completed.
-- Safe to re-run: uses INSERT ... ON CONFLICT DO NOTHING.
--
-- Scenarios covered:
--   seed-001  Confirmed order (saga COMPLETED)
--   seed-002  Active saga AWAITING_CUSTOMER_DECISION (partial reservation)
--   seed-003  Cancelled order (insufficient credit)
--   seed-004  Failed compensation saga
--   Multi-warehouse allocations on seed-002
--   Approved/rejected credit reservations

-- ── Order Service ─────────────────────────────────────────────────────────────

INSERT INTO order_service.orders
    (id, customer_id, submitted_by, status, cancellation_reason, version)
VALUES
    ('order-seed-001', 'cust-acme-001', 'user-acme-buyer-001', 'CONFIRMED',    NULL,                   1),
    ('order-seed-002', 'cust-acme-001', 'user-acme-buyer-001', 'AWAITING_CUSTOMER_DECISION', NULL,     0),
    ('order-seed-003', 'cust-beta-001', 'user-beta-buyer-001', 'CANCELLED',    'INSUFFICIENT_CREDIT',  1),
    ('order-seed-004', 'cust-acme-001', 'user-acme-buyer-001', 'CANCELLED',    'CUSTOMER_CANCELLED',   1)
ON CONFLICT DO NOTHING;

INSERT INTO order_service.order_lines
    (order_id, sku, quantity)
VALUES
    ('order-seed-001', 'SKU-WIDGET-A', 100),
    ('order-seed-001', 'SKU-WIDGET-B', 50),
    ('order-seed-002', 'SKU-WIDGET-A', 200),
    ('order-seed-002', 'SKU-WIDGET-C', 75),
    ('order-seed-003', 'SKU-GADGET-X', 500),
    ('order-seed-004', 'SKU-WIDGET-A', 150)
ON CONFLICT DO NOTHING;

-- ── Inventory Service ─────────────────────────────────────────────────────────

INSERT INTO inventory_service.stock_reservations
    (id, order_id, status, version)
VALUES
    ('sr-seed-001', 'order-seed-001', 'RESERVED',           1),
    ('sr-seed-002', 'order-seed-002', 'PARTIALLY_RESERVED', 0),
    ('sr-seed-003', 'order-seed-003', 'RELEASED',           1),
    ('sr-seed-004', 'order-seed-004', 'RESERVED',           0)
ON CONFLICT DO NOTHING;

INSERT INTO inventory_service.stock_reservation_lines
    (reservation_id, sku_code, requested_quantity)
VALUES
    ('sr-seed-001', 'SKU-WIDGET-A', 100),
    ('sr-seed-001', 'SKU-WIDGET-B', 50),
    ('sr-seed-002', 'SKU-WIDGET-A', 200),
    ('sr-seed-002', 'SKU-WIDGET-C', 75),
    ('sr-seed-003', 'SKU-GADGET-X', 500),
    ('sr-seed-004', 'SKU-WIDGET-A', 150)
ON CONFLICT DO NOTHING;

-- Multi-warehouse allocations for seed-001 and seed-002
INSERT INTO inventory_service.stock_allocations
    (line_id, warehouse_id, sku, quantity)
SELECT l.id, 'WH-NORTH', l.sku_code, CASE WHEN l.sku_code = 'SKU-WIDGET-A' THEN 60 ELSE 30 END
FROM inventory_service.stock_reservation_lines l
JOIN inventory_service.stock_reservations r ON r.id = l.reservation_id
WHERE r.id = 'sr-seed-001'
ON CONFLICT DO NOTHING;

INSERT INTO inventory_service.stock_allocations
    (line_id, warehouse_id, sku, quantity)
SELECT l.id, 'WH-SOUTH', l.sku_code, CASE WHEN l.sku_code = 'SKU-WIDGET-A' THEN 40 ELSE 20 END
FROM inventory_service.stock_reservation_lines l
JOIN inventory_service.stock_reservations r ON r.id = l.reservation_id
WHERE r.id = 'sr-seed-001'
ON CONFLICT DO NOTHING;

-- Partial allocation for seed-002 (only 120 of 200 SKU-WIDGET-A available)
INSERT INTO inventory_service.stock_allocations
    (line_id, warehouse_id, sku, quantity)
SELECT l.id, 'WH-NORTH', 'SKU-WIDGET-A', 80
FROM inventory_service.stock_reservation_lines l
WHERE l.reservation_id = 'sr-seed-002' AND l.sku_code = 'SKU-WIDGET-A'
ON CONFLICT DO NOTHING;

INSERT INTO inventory_service.stock_allocations
    (line_id, warehouse_id, sku, quantity)
SELECT l.id, 'WH-EAST', 'SKU-WIDGET-A', 40
FROM inventory_service.stock_reservation_lines l
WHERE l.reservation_id = 'sr-seed-002' AND l.sku_code = 'SKU-WIDGET-A'
ON CONFLICT DO NOTHING;

-- ── Credit Service ────────────────────────────────────────────────────────────

INSERT INTO credit_service.credit_reservations
    (id, order_id, amount_value, amount_currency, status, version)
VALUES
    ('cr-seed-001', 'order-seed-001', 15000.00, 'EUR', 'APPROVED',  1),
    ('cr-seed-002', 'order-seed-002', 28000.00, 'EUR', 'APPROVED',  0),
    ('cr-seed-003', 'order-seed-003', 95000.00, 'EUR', 'REJECTED',  0),
    ('cr-seed-004', 'order-seed-004', 22500.00, 'EUR', 'RELEASED',  1)
ON CONFLICT DO NOTHING;

-- ── Orchestrator Service ──────────────────────────────────────────────────────

INSERT INTO orchestrator_service.sagas
    (id, order_id, customer_id, status, current_step,
     customer_decision, stock_reservation_id, credit_reservation_id, version)
VALUES
    ('saga-seed-001', 'order-seed-001', 'cust-acme-001',
     'COMPLETED',               'COMPLETE_ORDER',         NULL,             'sr-seed-001', 'cr-seed-001', 2),
    ('saga-seed-002', 'order-seed-002', 'cust-acme-001',
     'AWAITING_CUSTOMER_DECISION', 'AWAIT_CUSTOMER_DECISION', NULL,         'sr-seed-002', 'cr-seed-002', 1),
    ('saga-seed-003', 'order-seed-003', 'cust-beta-001',
     'CANCELLED',               'VALIDATE_CREDIT',        NULL,             'sr-seed-003', 'cr-seed-003', 1),
    ('saga-seed-004', 'order-seed-004', 'cust-acme-001',
     'FAILED_COMPENSATION',     'COMPENSATE_INVENTORY',   NULL,             'sr-seed-004', NULL,          1)
ON CONFLICT DO NOTHING;
