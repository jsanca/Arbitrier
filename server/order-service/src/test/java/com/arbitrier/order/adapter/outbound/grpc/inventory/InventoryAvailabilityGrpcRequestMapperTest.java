package com.arbitrier.order.adapter.outbound.grpc.inventory;

import com.arbitrier.contracts.inventory.v1.CheckAvailabilityRequest;
import com.arbitrier.order.application.port.outbound.AvailabilityLineQuery;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryAvailabilityGrpcRequestMapperTest {

    private final InventoryAvailabilityGrpcRequestMapper mapper = new InventoryAvailabilityGrpcRequestMapper();

    @Test
    void request_id_is_generated_and_non_blank() {
        final CheckAvailabilityRequest request = mapper.toRequest(List.of(
                new AvailabilityLineQuery("SKU-001", 5)));

        assertThat(request.getRequestId()).isNotBlank();
    }

    @Test
    void each_request_receives_a_unique_request_id() {
        final List<AvailabilityLineQuery> lines = List.of(new AvailabilityLineQuery("SKU-001", 1));

        final String id1 = mapper.toRequest(lines).getRequestId();
        final String id2 = mapper.toRequest(lines).getRequestId();

        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void single_item_maps_sku_and_quantity() {
        final CheckAvailabilityRequest request = mapper.toRequest(List.of(
                new AvailabilityLineQuery("SKU-ABC", 7)));

        assertThat(request.getItemsCount()).isEqualTo(1);
        assertThat(request.getItems(0).getProductId()).isEqualTo("SKU-ABC");
        assertThat(request.getItems(0).getQuantity()).isEqualTo(7);
    }

    @Test
    void multiple_items_preserve_order_and_quantities() {
        final CheckAvailabilityRequest request = mapper.toRequest(List.of(
                new AvailabilityLineQuery("SKU-001", 3),
                new AvailabilityLineQuery("SKU-002", 10),
                new AvailabilityLineQuery("SKU-003", 1)));

        assertThat(request.getItemsCount()).isEqualTo(3);
        assertThat(request.getItems(0).getProductId()).isEqualTo("SKU-001");
        assertThat(request.getItems(0).getQuantity()).isEqualTo(3);
        assertThat(request.getItems(1).getProductId()).isEqualTo("SKU-002");
        assertThat(request.getItems(1).getQuantity()).isEqualTo(10);
        assertThat(request.getItems(2).getProductId()).isEqualTo("SKU-003");
        assertThat(request.getItems(2).getQuantity()).isEqualTo(1);
    }

    @Test
    void no_protobuf_type_appears_in_application_query_model() {
        // AvailabilityLineQuery is a plain Java record — asserted by its type
        final AvailabilityLineQuery query = new AvailabilityLineQuery("SKU-001", 1);
        assertThat(query.getClass().getPackageName())
                .startsWith("com.arbitrier.order.application.port.outbound");
        assertThat(query.getClass().getPackageName())
                .doesNotContain("contracts");
    }
}
