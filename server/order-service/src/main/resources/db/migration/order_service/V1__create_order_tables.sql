-- V1: Order Service tables.

CREATE TABLE order_service.orders (
    id                  VARCHAR(255)    NOT NULL,
    customer_id         VARCHAR(255)    NOT NULL,
    submitted_by        VARCHAR(255)    NOT NULL,
    status              VARCHAR(50)     NOT NULL,
    cancellation_reason VARCHAR(50),
    version             BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT orders_status_chk CHECK (
        status IN (
            'PENDING', 'AWAITING_CUSTOMER_DECISION',
            'CONFIRMED', 'PARTIALLY_CONFIRMED', 'CANCELLED'
        )
    ),
    CONSTRAINT orders_cancellation_reason_chk CHECK (
        cancellation_reason IS NULL
        OR cancellation_reason IN (
            'CUSTOMER_CANCELLED', 'CUSTOMER_DEFERRED',
            'INSUFFICIENT_CREDIT', 'SYSTEM_TIMEOUT'
        )
    )
);

CREATE TABLE order_service.order_lines (
    id          BIGSERIAL       NOT NULL,
    order_id    VARCHAR(255)    NOT NULL,
    sku         VARCHAR(255)    NOT NULL,
    quantity    INTEGER         NOT NULL,
    CONSTRAINT pk_order_lines PRIMARY KEY (id),
    CONSTRAINT fk_order_lines_order FOREIGN KEY (order_id) REFERENCES order_service.orders (id),
    CONSTRAINT order_lines_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX idx_orders_customer_id   ON order_service.orders (customer_id);
CREATE INDEX idx_orders_status        ON order_service.orders (status);
CREATE INDEX idx_order_lines_order_id ON order_service.order_lines (order_id);
