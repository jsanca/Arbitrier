package com.arbitrier.contracts;

import org.apache.avro.Schema;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class ContractsSchemaTest {

    private static final Map<String, Schema> SCHEMAS = new ConcurrentHashMap<>();
    private static final Schema.Parser PARSER = new Schema.Parser();

    @BeforeAll
    static void parseAllSchemas() throws IOException {
        // common — parse first (no dependencies)
        parse("common/MessageMetadata");
        parse("common/MoneyAmount");
        parse("common/OrderLineContract");
        parse("common/CancellationReason");
        // orchestrator enums (referenced by orchestrator records)
        parse("orchestrator/CustomerDecision");
        parse("orchestrator/CompensationAction");
        // order events
        parse("order/OrderCreated");
        parse("order/OrderConfirmed");
        parse("order/OrderPartiallyConfirmed");
        parse("order/OrderCancelled");
        // inventory contracts
        parse("inventory/ReserveStockRequested");
        parse("inventory/StockReserved");
        parse("inventory/StockPartiallyReserved");
        parse("inventory/StockRejected");
        parse("inventory/ReleaseStockRequested");
        parse("inventory/StockReleased");
        // credit contracts
        parse("credit/ReserveCreditRequested");
        parse("credit/CreditApproved");
        parse("credit/CreditRejected");
        parse("credit/ReleaseCreditRequested");
        parse("credit/CreditReleased");
        // orchestrator records
        parse("orchestrator/CustomerDecisionRequested");
        parse("orchestrator/CustomerDecisionSubmitted");
        parse("orchestrator/SagaCompleted");
        parse("orchestrator/SagaCancelled");
        parse("orchestrator/SagaCompensationFailed");
    }

    private static void parse(String relativePath) throws IOException {
        File file = new File("src/main/avro/" + relativePath + ".avsc");
        Schema schema = PARSER.parse(file);
        SCHEMAS.put(schema.getFullName(), schema);
    }

    // ── common ────────────────────────────────────────────────────────────────

    @Test void message_metadata_schema_exists() {
        assertThat(SCHEMAS).containsKey("com.arbitrier.contracts.common.MessageMetadata");
    }

    @Test void message_metadata_has_required_fields() {
        Schema s = SCHEMAS.get("com.arbitrier.contracts.common.MessageMetadata");
        assertThat(s.getField("messageId")).isNotNull();
        assertThat(s.getField("correlationId")).isNotNull();
        assertThat(s.getField("causationId")).isNotNull();
        assertThat(s.getField("occurredAt")).isNotNull();
        assertThat(s.getField("schemaVersion")).isNotNull();
    }

    @Test void money_amount_schema_exists() {
        assertThat(SCHEMAS).containsKey("com.arbitrier.contracts.common.MoneyAmount");
    }

    @Test void money_amount_has_amount_and_currency() {
        Schema s = SCHEMAS.get("com.arbitrier.contracts.common.MoneyAmount");
        assertThat(s.getField("amount")).isNotNull();
        assertThat(s.getField("currency")).isNotNull();
    }

    @Test void order_line_contract_schema_exists() {
        assertThat(SCHEMAS).containsKey("com.arbitrier.contracts.common.OrderLineContract");
    }

    @Test void cancellation_reason_enum_has_expected_symbols() {
        Schema s = SCHEMAS.get("com.arbitrier.contracts.common.CancellationReason");
        assertThat(s.getType()).isEqualTo(Schema.Type.ENUM);
        assertThat(s.getEnumSymbols()).containsExactlyInAnyOrder(
                "CUSTOMER_CANCELLED", "CUSTOMER_DEFERRED", "INSUFFICIENT_CREDIT", "SYSTEM_TIMEOUT");
    }

    // ── orchestrator enums ────────────────────────────────────────────────────

    @Test void customer_decision_enum_has_expected_symbols() {
        Schema s = SCHEMAS.get("com.arbitrier.contracts.orchestrator.CustomerDecision");
        assertThat(s.getType()).isEqualTo(Schema.Type.ENUM);
        assertThat(s.getEnumSymbols()).containsExactlyInAnyOrder(
                "ACCEPT_PARTIAL", "WAIT_BACKORDER", "CANCEL_ORDER");
    }

    @Test void compensation_action_enum_has_expected_symbols() {
        Schema s = SCHEMAS.get("com.arbitrier.contracts.orchestrator.CompensationAction");
        assertThat(s.getType()).isEqualTo(Schema.Type.ENUM);
        assertThat(s.getEnumSymbols()).containsExactlyInAnyOrder(
                "RELEASE_INVENTORY_RESERVATION", "RELEASE_CREDIT_RESERVATION", "NONE");
    }

    // ── order events ──────────────────────────────────────────────────────────

    @Test void every_order_schema_has_metadata_field() {
        for (String name : new String[]{
                "com.arbitrier.contracts.order.OrderCreated",
                "com.arbitrier.contracts.order.OrderConfirmed",
                "com.arbitrier.contracts.order.OrderPartiallyConfirmed",
                "com.arbitrier.contracts.order.OrderCancelled"}) {
            assertThat(SCHEMAS.get(name).getField("metadata"))
                    .as("metadata field in " + name).isNotNull();
        }
    }

    @Test void order_created_has_required_fields() {
        Schema s = SCHEMAS.get("com.arbitrier.contracts.order.OrderCreated");
        assertThat(s.getField("orderId")).isNotNull();
        assertThat(s.getField("customerId")).isNotNull();
        assertThat(s.getField("submittedByUserId")).isNotNull();
        assertThat(s.getField("lines")).isNotNull();
        assertThat(s.getField("requestedTotal")).isNotNull();
    }

    @Test void order_partially_confirmed_has_confirmed_and_backorder_lines() {
        Schema s = SCHEMAS.get("com.arbitrier.contracts.order.OrderPartiallyConfirmed");
        assertThat(s.getField("confirmedLines")).isNotNull();
        assertThat(s.getField("backorderLines")).isNotNull();
        assertThat(s.getField("confirmedTotal")).isNotNull();
    }

    @Test void order_cancelled_has_cancellation_reason() {
        Schema s = SCHEMAS.get("com.arbitrier.contracts.order.OrderCancelled");
        assertThat(s.getField("cancellationReason")).isNotNull();
    }

    // ── inventory contracts ───────────────────────────────────────────────────

    @Test void every_inventory_schema_has_metadata_field() {
        for (String name : new String[]{
                "com.arbitrier.contracts.inventory.ReserveStockRequested",
                "com.arbitrier.contracts.inventory.StockReserved",
                "com.arbitrier.contracts.inventory.StockPartiallyReserved",
                "com.arbitrier.contracts.inventory.StockRejected",
                "com.arbitrier.contracts.inventory.ReleaseStockRequested",
                "com.arbitrier.contracts.inventory.StockReleased"}) {
            assertThat(SCHEMAS.get(name).getField("metadata"))
                    .as("metadata field in " + name).isNotNull();
        }
    }

    @Test void stock_partially_reserved_has_reserved_and_backorder_lines() {
        Schema s = SCHEMAS.get("com.arbitrier.contracts.inventory.StockPartiallyReserved");
        assertThat(s.getField("reservedLines")).isNotNull();
        assertThat(s.getField("backorderLines")).isNotNull();
    }

    // ── credit contracts ──────────────────────────────────────────────────────

    @Test void every_credit_schema_has_metadata_field() {
        for (String name : new String[]{
                "com.arbitrier.contracts.credit.ReserveCreditRequested",
                "com.arbitrier.contracts.credit.CreditApproved",
                "com.arbitrier.contracts.credit.CreditRejected",
                "com.arbitrier.contracts.credit.ReleaseCreditRequested",
                "com.arbitrier.contracts.credit.CreditReleased"}) {
            assertThat(SCHEMAS.get(name).getField("metadata"))
                    .as("metadata field in " + name).isNotNull();
        }
    }

    // ── orchestrator events ───────────────────────────────────────────────────

    @Test void every_orchestrator_schema_has_metadata_field() {
        for (String name : new String[]{
                "com.arbitrier.contracts.orchestrator.CustomerDecisionRequested",
                "com.arbitrier.contracts.orchestrator.CustomerDecisionSubmitted",
                "com.arbitrier.contracts.orchestrator.SagaCompleted",
                "com.arbitrier.contracts.orchestrator.SagaCancelled",
                "com.arbitrier.contracts.orchestrator.SagaCompensationFailed"}) {
            assertThat(SCHEMAS.get(name).getField("metadata"))
                    .as("metadata field in " + name).isNotNull();
        }
    }

    @Test void customer_decision_submitted_has_decision_field() {
        Schema s = SCHEMAS.get("com.arbitrier.contracts.orchestrator.CustomerDecisionSubmitted");
        assertThat(s.getField("decision")).isNotNull();
    }

    @Test void saga_compensation_failed_has_failed_action() {
        Schema s = SCHEMAS.get("com.arbitrier.contracts.orchestrator.SagaCompensationFailed");
        assertThat(s.getField("failedAction")).isNotNull();
        assertThat(s.getField("reason")).isNotNull();
    }

    // ── negative assertions ───────────────────────────────────────────────────

    @Test void no_schema_contains_tenant_id() {
        for (Map.Entry<String, Schema> entry : SCHEMAS.entrySet()) {
            Schema s = entry.getValue();
            if (s.getType() == Schema.Type.RECORD) {
                assertThat(s.getField("tenantId"))
                        .as("schema " + entry.getKey() + " must not contain tenantId in v1")
                        .isNull();
            }
        }
    }

    @Test void no_schema_references_service_domain_packages() {
        for (String name : SCHEMAS.keySet()) {
            assertThat(name)
                    .as("schema name must be in com.arbitrier.contracts namespace")
                    .startsWith("com.arbitrier.contracts");
        }
    }
}
