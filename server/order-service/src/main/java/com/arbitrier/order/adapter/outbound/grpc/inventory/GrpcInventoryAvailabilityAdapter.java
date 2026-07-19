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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
 * <p>After mapping, the response is validated against the original query list: the response
 * must contain exactly one entry per requested SKU, with no duplicates and no unexpected SKUs.
 * Violations throw {@link InventoryAvailabilityProtocolException} — they are upstream integration
 * failures, not business unavailability.
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
        this(stub, deadline, new InventoryAvailabilityGrpcResponseMapper());
    }

    /** Package-private constructor for unit tests — allows injecting a mock response mapper. */
    GrpcInventoryAvailabilityAdapter(
            final InventoryAvailabilityServiceGrpc.InventoryAvailabilityServiceBlockingStub stub,
            final Duration deadline,
            final InventoryAvailabilityGrpcResponseMapper responseMapper) {
        this.stub = Require.notNull(stub, "GrpcInventoryAvailabilityAdapter.stub");
        this.deadline = Require.notNull(deadline, "GrpcInventoryAvailabilityAdapter.deadline");
        this.requestMapper = new InventoryAvailabilityGrpcRequestMapper();
        this.responseMapper = Require.notNull(responseMapper, "GrpcInventoryAvailabilityAdapter.responseMapper");
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
            validateResponseContract(result, lines);

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

    /**
     * Validates that the mapped response satisfies the port contract:
     * exactly one result per requested SKU, no duplicates, no unexpected SKUs.
     */
    private void validateResponseContract(
            final List<AvailabilityLineResponse> result,
            final List<AvailabilityLineQuery> queries) {

        if (result.size() != queries.size()) {
            throw new InventoryAvailabilityProtocolException(
                    "Inventory response line count mismatch: expected " + queries.size()
                    + ", got " + result.size());
        }

        final Set<String> requestedSkus = queries.stream()
                .map(AvailabilityLineQuery::sku)
                .collect(Collectors.toSet());

        final Set<String> seenSkus = new HashSet<>(result.size());
        for (final AvailabilityLineResponse r : result) {
            if (!seenSkus.add(r.sku())) {
                throw new InventoryAvailabilityProtocolException(
                        "Inventory response contains duplicate SKU: " + r.sku());
            }
            if (!requestedSkus.contains(r.sku())) {
                throw new InventoryAvailabilityProtocolException(
                        "Inventory response contains unexpected SKU: " + r.sku());
            }
        }
    }
}
