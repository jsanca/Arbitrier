-- V1: Inventory Service tables.

CREATE TABLE inventory_service.stock_reservations (
    id          VARCHAR(255)    NOT NULL,
    order_id    VARCHAR(255)    NOT NULL,
    status      VARCHAR(50)     NOT NULL,
    version     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_stock_reservations PRIMARY KEY (id),
    CONSTRAINT stock_reservations_status_chk CHECK (
        status IN ('RESERVED', 'PARTIALLY_RESERVED', 'REJECTED', 'RELEASED')
    )
);

CREATE TABLE inventory_service.stock_reservation_lines (
    id                  BIGSERIAL       NOT NULL,
    reservation_id      VARCHAR(255)    NOT NULL,
    sku_code            VARCHAR(255)    NOT NULL,
    requested_quantity  INTEGER         NOT NULL,
    CONSTRAINT pk_stock_reservation_lines PRIMARY KEY (id),
    CONSTRAINT fk_srl_reservation FOREIGN KEY (reservation_id) REFERENCES inventory_service.stock_reservations (id),
    CONSTRAINT srl_requested_quantity_positive CHECK (requested_quantity > 0)
);

CREATE TABLE inventory_service.stock_allocations (
    id              BIGSERIAL       NOT NULL,
    line_id         BIGINT          NOT NULL,
    warehouse_id    VARCHAR(255)    NOT NULL,
    sku             VARCHAR(255)    NOT NULL,
    quantity        INTEGER         NOT NULL,
    CONSTRAINT pk_stock_allocations PRIMARY KEY (id),
    CONSTRAINT fk_sa_line FOREIGN KEY (line_id) REFERENCES inventory_service.stock_reservation_lines (id),
    CONSTRAINT sa_quantity_non_negative CHECK (quantity >= 0)
);

CREATE INDEX idx_stock_reservations_order_id  ON inventory_service.stock_reservations (order_id);
CREATE INDEX idx_stock_reservations_status    ON inventory_service.stock_reservations (status);
CREATE INDEX idx_srl_reservation_id           ON inventory_service.stock_reservation_lines (reservation_id);
CREATE INDEX idx_sa_line_id                   ON inventory_service.stock_allocations (line_id);
CREATE INDEX idx_sa_warehouse_sku             ON inventory_service.stock_allocations (warehouse_id, sku);
