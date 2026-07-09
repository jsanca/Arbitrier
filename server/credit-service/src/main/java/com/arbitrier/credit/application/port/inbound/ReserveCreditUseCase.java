package com.arbitrier.credit.application.port.inbound;

/**
 * Inbound port: reserve credit for an order against the customer's credit limit.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: credit-service
 */
public interface ReserveCreditUseCase {

    /**
     * Attempts to reserve the requested amount of credit for the given order.
     *
     * @param command the reservation request
     * @return the result indicating APPROVED or REJECTED outcome
     */
    ReserveCreditResult reserve(ReserveCreditCommand command);
}
