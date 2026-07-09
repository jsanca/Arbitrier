package com.arbitrier.order.adapter.outbound.customer;

import com.arbitrier.order.application.port.outbound.CustomerAccessPort;

/**
 * Permissive {@link CustomerAccessPort} adapter — allows any user to submit orders for any customer.
 *
 * <p>This is a placeholder wired until the real customer-membership integration is implemented.
 * Replace with a proper adapter when customer authorisation requirements are finalised.
 *
 * <p>Layer: adapter/outbound/customer
 * <p>Module: order-service
 */
public class AllowAllCustomerAccessAdapter implements CustomerAccessPort {

    @Override
    public boolean canSubmitOrder(String userId, String customerId) {
        return true;
    }
}
