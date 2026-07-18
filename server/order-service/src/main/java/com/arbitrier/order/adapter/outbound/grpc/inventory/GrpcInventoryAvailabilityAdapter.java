package com.arbitrier.order.adapter.outbound.grpc.inventory;

import com.arbitrier.contracts.inventory.v1.CheckAvailabilityRequest;
import com.arbitrier.contracts.inventory.v1.CheckAvailabilityResponse;
import com.arbitrier.contracts.inventory.v1.InventoryAvailabilityServiceGrpc;
import com.arbitrier.order.application.port.outbound.AvailabilityLineQuery;
import com.arbitrier.order.application.port.outbound.AvailabilityLineResponse;
import com.arbitrier.order.application.port.outbound.InventoryAvailabilityPort;
import com.arbitrier.platform.validation.Require;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Outbound gRPC adapter implementing {@link InventoryAvailabilityPort} for Order Service.
 *
 * <p>Translates and delegates: maps the Order application query to a protobuf request,
 * invokes {@code InventoryAvailabilityService.CheckAvailability} via a blocking stub with
 * a configured deadline, and maps the protobuf response back to Order application types.
 *
 * <p>A blocking stub is used because the Order application is not reactive and Java virtual
 * threads can absorb blocking I/O (ADR-0007, {@code spring.threads.virtual.enabled=true}).
 *
 * <p>No business decisions are made here. The adapter only translates the remote response
 * into the existing {@link AvailabilityLineResponse} model consumed by the application service.
 *
 * <p>{@code StatusRuntimeException} is never exposed outside this class; it is always
 * converted by {@link InventoryAvailabilityGrpcExceptionMapper}.
 *
 * <p>Layer: adapter/outbound/grpc/inventory
 * <p>Module: order-service
 */
public class GrpcInventoryAvailabilityAdapter implements InventoryAvailabilityPort {

    private static final Logger log = LoggerFactory.getLogger(GrpcInventoryAvailabilityAdapter.class);

    private final InventoryAvailabilityServiceGrpc.InventoryAvailabilityServiceBlockingStub stub;
    private final Duration deadline;
    private final InventoryAvailabilityGrpcRequestMapper requestMapper;
    private final InventoryAvailabilityGrpcResponseMapper responseMapper;
    private final InventoryAvailabilityGrpcExceptionMapper exceptionMapper;

    public GrpcInventoryAvailabilityAdapter(
            final InventoryAvailabilityServiceGrpc.InventoryAvailabilityServiceBlockingStub stub,
            final Duration deadline) {
        this.stub = Require.notNull(stub, "GrpcInventoryAvailabilityAdapter.stub");
        this.deadline = Require.notNull(deadline, "GrpcInventoryAvailabilityAdapter.deadline");
        this.requestMapper = new InventoryAvailabilityGrpcRequestMapper();
        this.responseMapper = new InventoryAvailabilityGrpcResponseMapper();
        this.exceptionMapper = new InventoryAvailabilityGrpcExceptionMapper();
    }

    @Override
    public List<AvailabilityLineResponse> checkAvailability(final List<AvailabilityLineQuery> lines) {
        final CheckAvailabilityRequest request = requestMapper.toRequest(lines);

        log.debug("Sending inventory availability query: requestId={}, itemCount={}",
                request.getRequestId(), lines.size());

        try {
            final CheckAvailabilityResponse response = stub
                    .withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS)
                    .checkAvailability(request);

            final List<AvailabilityLineResponse> result = responseMapper.toLines(response, lines);

            log.debug("Inventory availability response: requestId={}, status={}, unavailableItems={}",
                    request.getRequestId(), response.getStatus(), response.getUnavailableItemsCount());

            return result;
        } catch (final InventoryAvailabilityException e) {
            // Re-throw protocol exceptions from the response mapper without wrapping
            throw e;
        } catch (final Exception e) {
            final InventoryAvailabilityException mapped = exceptionMapper.toAdapterException(e);
            log.warn("Inventory availability query failed: requestId={}, error={}",
                    request.getRequestId(), mapped.getMessage());
            throw mapped;
        }
    }
}
