-- V1: Credit Service tables.

CREATE TABLE credit_service.credit_reservations (
    id              VARCHAR(255)    NOT NULL,
    order_id        VARCHAR(255)    NOT NULL,
    amount_value    NUMERIC(19, 4)  NOT NULL,
    amount_currency VARCHAR(3)      NOT NULL,
    status          VARCHAR(50)     NOT NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_credit_reservations PRIMARY KEY (id),
    CONSTRAINT credit_reservations_status_chk CHECK (
        status IN ('APPROVED', 'REJECTED', 'RELEASED')
    ),
    CONSTRAINT credit_reservations_amount_non_negative CHECK (amount_value >= 0)
);

CREATE INDEX idx_credit_reservations_order_id ON credit_service.credit_reservations (order_id);
CREATE INDEX idx_credit_reservations_status   ON credit_service.credit_reservations (status);
