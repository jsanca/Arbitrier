-- V1: Orchestrator Service tables.

CREATE TABLE orchestrator_service.sagas (
    id                      VARCHAR(255)    NOT NULL,
    order_id                VARCHAR(255)    NOT NULL,
    customer_id             VARCHAR(255)    NOT NULL,
    status                  VARCHAR(50)     NOT NULL,
    current_step            VARCHAR(50)     NOT NULL,
    customer_decision       VARCHAR(50),
    stock_reservation_id    VARCHAR(255),
    credit_reservation_id   VARCHAR(255),
    version                 BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_sagas PRIMARY KEY (id),
    CONSTRAINT sagas_status_chk CHECK (
        status IN (
            'STARTED', 'WAITING_FOR_INVENTORY', 'WAITING_FOR_CREDIT',
            'AWAITING_CUSTOMER_DECISION', 'COMPENSATING',
            'COMPLETED', 'CANCELLED', 'FAILED_COMPENSATION'
        )
    ),
    CONSTRAINT sagas_current_step_chk CHECK (
        current_step IN (
            'ORDER_CREATED', 'RESERVE_INVENTORY', 'VALIDATE_CREDIT',
            'AWAIT_CUSTOMER_DECISION', 'COMPLETE_ORDER',
            'COMPENSATE_INVENTORY', 'COMPENSATE_CREDIT'
        )
    ),
    CONSTRAINT sagas_customer_decision_chk CHECK (
        customer_decision IS NULL
        OR customer_decision IN ('ACCEPT_PARTIAL', 'WAIT_BACKORDER', 'CANCEL_ORDER')
    )
);

-- Indexes supporting dashboard queries, status polling, and timeout scanning.
CREATE INDEX idx_sagas_order_id     ON orchestrator_service.sagas (order_id);
CREATE INDEX idx_sagas_status       ON orchestrator_service.sagas (status);
CREATE INDEX idx_sagas_current_step ON orchestrator_service.sagas (current_step);
CREATE INDEX idx_sagas_waiting      ON orchestrator_service.sagas (status)
    WHERE status IN ('AWAITING_CUSTOMER_DECISION', 'WAITING_FOR_INVENTORY', 'WAITING_FOR_CREDIT');
CREATE INDEX idx_sagas_failed_comp  ON orchestrator_service.sagas (status) WHERE status = 'FAILED_COMPENSATION';
