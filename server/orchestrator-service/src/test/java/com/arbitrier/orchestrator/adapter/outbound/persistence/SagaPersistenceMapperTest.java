package com.arbitrier.orchestrator.adapter.outbound.persistence;

import com.arbitrier.orchestrator.domain.model.CustomerDecision;
import com.arbitrier.orchestrator.domain.model.Saga;
import com.arbitrier.orchestrator.domain.model.SagaId;
import com.arbitrier.orchestrator.domain.model.SagaStatus;
import com.arbitrier.orchestrator.domain.model.SagaStep;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SagaPersistenceMapperTest {

    private static final SagaPersistenceMapper MAPPER = new SagaPersistenceMapper();

    private static final SagaId SAGA_ID = SagaId.of("saga-1");
    private static final String ORDER_ID = "order-1";
    private static final String CUSTOMER_ID = "cust-1";

    // ── toEntity / new saga ───────────────────────────────────────────────────

    @Test
    void new_saga_maps_to_entity_with_null_version_and_null_optional_fields() {
        Saga saga = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID);

        SagaEntity entity = MAPPER.toEntity(saga);

        assertThat(entity.getId()).isEqualTo("saga-1");
        assertThat(entity.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(entity.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(entity.getStatus()).isEqualTo("STARTED");
        assertThat(entity.getCurrentStep()).isEqualTo("ORDER_CREATED");
        assertThat(entity.getCustomerDecision()).isNull();
        assertThat(entity.getStockReservationId()).isNull();
        assertThat(entity.getCreditReservationId()).isNull();
        assertThat(entity.getVersion()).isNull();
    }

    @Test
    void toEntity_maps_optional_fields_when_present() {
        Saga saga = Saga.reconstruct(SAGA_ID, ORDER_ID, CUSTOMER_ID,
                SagaStatus.AWAITING_CUSTOMER_DECISION, SagaStep.AWAIT_CUSTOMER_DECISION,
                null, "stock-res-1", null, null);

        SagaEntity entity = MAPPER.toEntity(saga);

        assertThat(entity.getStockReservationId()).isEqualTo("stock-res-1");
        assertThat(entity.getCustomerDecision()).isNull();
    }

    @Test
    void toEntity_maps_customer_decision_when_present() {
        Saga saga = Saga.reconstruct(SAGA_ID, ORDER_ID, CUSTOMER_ID,
                SagaStatus.STARTED, SagaStep.VALIDATE_CREDIT,
                CustomerDecision.ACCEPT_PARTIAL, "stock-res-1", null, 2L);

        SagaEntity entity = MAPPER.toEntity(saga);

        assertThat(entity.getCustomerDecision()).isEqualTo("ACCEPT_PARTIAL");
    }

    // ── toDomain ─────────────────────────────────────────────────────────────

    @Test
    void entity_round_trips_to_domain_and_back() {
        Saga original = Saga.reconstruct(SAGA_ID, ORDER_ID, CUSTOMER_ID,
                SagaStatus.STARTED, SagaStep.RESERVE_INVENTORY,
                null, null, null, 7L);

        SagaEntity entity = MAPPER.toEntity(original);
        entity.setVersion(7L);
        Saga restored = MAPPER.toDomain(entity);

        assertThat(restored.id()).isEqualTo(SAGA_ID);
        assertThat(restored.orderId()).isEqualTo(ORDER_ID);
        assertThat(restored.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(restored.status()).isEqualTo(SagaStatus.STARTED);
        assertThat(restored.currentStep()).isEqualTo(SagaStep.RESERVE_INVENTORY);
        assertThat(restored.customerDecision()).isNull();
        assertThat(restored.stockReservationId()).isNull();
        assertThat(restored.creditReservationId()).isNull();
        assertThat(restored.version()).isEqualTo(7L);
    }

    @Test
    void toDomain_restores_all_optional_fields() {
        Saga original = Saga.reconstruct(SAGA_ID, ORDER_ID, CUSTOMER_ID,
                SagaStatus.COMPLETED, SagaStep.COMPLETE_ORDER,
                CustomerDecision.ACCEPT_PARTIAL, "stock-res-1", "credit-res-1", 10L);

        SagaEntity entity = MAPPER.toEntity(original);
        entity.setVersion(10L);
        Saga restored = MAPPER.toDomain(entity);

        assertThat(restored.customerDecision()).isEqualTo(CustomerDecision.ACCEPT_PARTIAL);
        assertThat(restored.stockReservationId()).isEqualTo("stock-res-1");
        assertThat(restored.creditReservationId()).isEqualTo("credit-res-1");
    }

    @Test
    void toDomain_restores_version() {
        Saga original = Saga.reconstruct(SAGA_ID, ORDER_ID, CUSTOMER_ID,
                SagaStatus.CANCELLED, SagaStep.ORDER_CREATED,
                null, null, null, 55L);

        SagaEntity entity = MAPPER.toEntity(original);
        entity.setVersion(55L);

        assertThat(MAPPER.toDomain(entity).version()).isEqualTo(55L);
    }

    // ── updateEntity ─────────────────────────────────────────────────────────

    @Test
    void updateEntity_updates_fields_and_preserves_entity_version() {
        Saga initial = Saga.start(SAGA_ID, ORDER_ID, CUSTOMER_ID);
        SagaEntity entity = MAPPER.toEntity(initial);
        entity.setVersion(3L);

        Saga advanced = Saga.reconstruct(SAGA_ID, ORDER_ID, CUSTOMER_ID,
                SagaStatus.WAITING_FOR_INVENTORY, SagaStep.RESERVE_INVENTORY,
                null, null, null, 3L);

        MAPPER.updateEntity(entity, advanced);

        assertThat(entity.getVersion()).isEqualTo(3L);
        assertThat(entity.getStatus()).isEqualTo("WAITING_FOR_INVENTORY");
        assertThat(entity.getCurrentStep()).isEqualTo("RESERVE_INVENTORY");
    }

    // ── corrupt data ─────────────────────────────────────────────────────────

    @Test
    void toDomain_rejects_unknown_status() {
        SagaEntity entity = buildEntity("UNKNOWN_STATUS", "ORDER_CREATED", null);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> MAPPER.toDomain(entity))
                .withMessageContaining("UNKNOWN_STATUS");
    }

    @Test
    void toDomain_rejects_unknown_step() {
        SagaEntity entity = buildEntity("STARTED", "UNKNOWN_STEP", null);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> MAPPER.toDomain(entity))
                .withMessageContaining("UNKNOWN_STEP");
    }

    @Test
    void toDomain_rejects_unknown_customer_decision() {
        SagaEntity entity = buildEntity("STARTED", "ORDER_CREATED", "UNKNOWN_DECISION");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> MAPPER.toDomain(entity))
                .withMessageContaining("UNKNOWN_DECISION");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static SagaEntity buildEntity(String status, String step, String customerDecision) {
        SagaEntity entity = new SagaEntity();
        entity.setId("saga-1");
        entity.setOrderId("order-1");
        entity.setCustomerId("cust-1");
        entity.setStatus(status);
        entity.setCurrentStep(step);
        entity.setCustomerDecision(customerDecision);
        entity.setVersion(1L);
        return entity;
    }
}
