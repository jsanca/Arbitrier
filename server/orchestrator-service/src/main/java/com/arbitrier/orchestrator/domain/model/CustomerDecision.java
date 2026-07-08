package com.arbitrier.orchestrator.domain.model;

/**
 * Decision submitted by a customer when an order is partially reservable.
 *
 * <p>Layer: domain/model
 * <p>Module: orchestrator-service
 */
public enum CustomerDecision {
    ACCEPT_PARTIAL,
    WAIT_BACKORDER,
    CANCEL_ORDER
}
