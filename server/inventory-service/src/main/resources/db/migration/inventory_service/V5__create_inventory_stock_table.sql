-- V2: Inventory stock table — source of on-hand quantities for availability queries.
-- Available quantity is derived: on_hand_quantity - SUM(active allocation quantities).
-- Active reservations are those with status RESERVED or PARTIALLY_RESERVED.
-- REJECTED and RELEASED reservations do not consume availability.

CREATE TABLE inventory_service.inventory_stock (
    product_id          VARCHAR(255)    NOT NULL,
    on_hand_quantity    INTEGER         NOT NULL DEFAULT 0,
    version             BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_inventory_stock PRIMARY KEY (product_id),
    CONSTRAINT inventory_stock_on_hand_non_negative CHECK (on_hand_quantity >= 0)
);

CREATE INDEX idx_inventory_stock_product_id ON inventory_service.inventory_stock (product_id);
