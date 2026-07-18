package com.arbitrier.inventory.adapter.inbound.grpc;

import com.arbitrier.contracts.inventory.v1.CheckAvailabilityRequest;
import com.arbitrier.contracts.inventory.v1.CheckAvailabilityResponse;
import com.arbitrier.contracts.inventory.v1.InventoryAvailabilityServiceGrpc;
import com.arbitrier.inventory.application.port.inbound.CheckInventoryAvailabilityUseCase;
import com.arbitrier.inventory.application.port.inbound.InventoryAvailabilityQuery;
import com.arbitrier.inventory.application.port.inbound.InventoryAvailabilityResult;
import com.arbitrier.platform.validation.Require;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC inbound adapter implementing {@code InventoryAvailabilityService.CheckAvailability}.
 *
 * <p>Translates and delegates: maps the protobuf request to an application query, invokes
 * the use case, and maps the result back to a protobuf response. No business logic lives here.
 *
 * <p>Layer: adapter/inbound/grpc
 * <p>Module: inventory-service
 */
@GrpcService
public class InventoryAvailabilityGrpcService
        extends InventoryAvailabilityServiceGrpc.InventoryAvailabilityServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(InventoryAvailabilityGrpcService.class);

    private final CheckInventoryAvailabilityUseCase useCase;
    private final InventoryAvailabilityGrpcRequestMapper requestMapper;
    private final InventoryAvailabilityGrpcResponseMapper responseMapper;
    private final InventoryGrpcExceptionMapper exceptionMapper;

    public InventoryAvailabilityGrpcService(
            final CheckInventoryAvailabilityUseCase useCase,
            final InventoryAvailabilityGrpcRequestMapper requestMapper,
            final InventoryAvailabilityGrpcResponseMapper responseMapper,
            final InventoryGrpcExceptionMapper exceptionMapper) {
        this.useCase = Require.notNull(useCase, "InventoryAvailabilityGrpcService.useCase");
        this.requestMapper = Require.notNull(requestMapper, "InventoryAvailabilityGrpcService.requestMapper");
        this.responseMapper = Require.notNull(responseMapper, "InventoryAvailabilityGrpcService.responseMapper");
        this.exceptionMapper = Require.notNull(exceptionMapper, "InventoryAvailabilityGrpcService.exceptionMapper");
    }

    @Override
    public void checkAvailability(
            final CheckAvailabilityRequest request,
            final StreamObserver<CheckAvailabilityResponse> responseObserver) {

        try {
            final InventoryAvailabilityQuery query = requestMapper.toQuery(request);
            final InventoryAvailabilityResult result = useCase.check(query);
            final CheckAvailabilityResponse response = responseMapper.toResponse(result);

            log.debug("Availability check completed: requestId={}, status={}",
                    query.requestId(), response.getStatus());

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (final Exception e) {
            log.warn("Availability check failed: {}", e.getMessage());
            responseObserver.onError(exceptionMapper.toStatusException(e));
        }
    }
}
