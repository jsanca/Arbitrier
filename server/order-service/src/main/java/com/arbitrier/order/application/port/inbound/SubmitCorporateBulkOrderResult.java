package com.arbitrier.order.application.port.inbound;

/**
 * Result returned after a corporate bulk order is successfully submitted.
 *
 * <p>Layer: application/port/inbound
 * <p>Module: order-service
 */
public record SubmitCorporateBulkOrderResult(String orderId, String status) {}
