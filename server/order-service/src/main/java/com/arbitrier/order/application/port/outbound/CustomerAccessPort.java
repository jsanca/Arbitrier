package com.arbitrier.order.application.port.outbound;

/**
 * Outbound port: checks whether a user is authorised to act on behalf of a customer.
 *
 * <p>Implemented by adapters that query a customer membership or authorization store.
 * In the current phase, a permissive placeholder adapter is wired until the real
 * customer-membership integration is implemented.
 *
 * <p>Layer: application/port/outbound
 * <p>Module: order-service
 */
public interface CustomerAccessPort {

    /**
     * Returns {@code true} if the given user may submit orders for the given customer.
     *
     * @param userId     the user identifier (JWT subject); must not be blank
     * @param customerId the customer identifier; must not be blank
     * @return {@code true} if access is permitted
     */
    boolean canSubmitOrder(String userId, String customerId);
}
