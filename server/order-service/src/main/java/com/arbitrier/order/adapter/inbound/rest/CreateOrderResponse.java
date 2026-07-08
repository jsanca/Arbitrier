package com.arbitrier.order.adapter.inbound.rest;

/**
 * REST response DTO returned after a corporate bulk order is submitted.
 *
 * <p>Layer: adapter/inbound/rest
 * <p>Module: order-service
 */
public record CreateOrderResponse(String orderId, String status) {}
