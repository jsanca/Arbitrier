# Arbitrier Roadmap

---

# FOUNDATION

ARB-000 [DONE]
Project Definition & Vision

ARB-001 [DONE]
Repository Layout

ARB-002 [DONE]
Documentation Foundation

---

# CORE PLATFORM

ARB-003 [DONE]
Architecture Skeleton

ARB-004 [DONE]
Platform Foundation

ARB-004B [DONE]
Native Image / Spring AOT Technical Variant

ARB-005 [DONE]
Domain Model

ARB-006 [DONE]
Contracts

---

# ORDER DOMAIN & PLATFORM CAPABILITIES

ARB-007 [DONE]
Order Service

ARB-008 [DONE]
Shared Platform

ARB-009 [DONE]
Observability

ARB-010 [DONE]
Security Integration

ARB-011 [DONE]
Contracts & Messaging Foundation

---

# BUSINESS SERVICES

ARB-012 [DONE]
Inventory Service

ARB-013 [DONE]
Credit Service

---

# SAGA ORCHESTRATION

ARB-014 [DONE]
Saga Orchestrator

ARB-015 [DONE]
Saga Happy Path

ARB-016 [DONE]
Saga Compensation

ARB-017 [DONE]
Pre-Saga Availability Negotiation

ARB-017B [DONE]
Global Inventory Allocation Ownership

ARB-018 [DONE]
Saga Retry Decision Policy
(Runtime scheduling deferred)

---

=========================
SECURITY
=========================

ARB-018A [DONE]
Architecture Security Review

ARB-018B [PLANNED]
Threat Model

ARB-018C [PLANNED]
Security Hardening

---

# INFRASTRUCTURE ADAPTERS

ARB-019 [DONE]
Persistence Adapters

ARB-020 [DONE]
Database Migrations & Synthetic Data

ARB-021 [DONE]
Outbox / Inbox Foundation

---

# MESSAGING RUNTIME

ARB-022 [IN PROGRESS]
Outbound Messaging Runtime

## Foundation

ARB-022.1 [DONE]
Messaging Contracts & Publisher Foundation

ARB-022.2 [DONE]
Outbound Dispatch Foundation

- Publisher Port
- Kafka Publisher Adapter
- Payload Serialization Strategy
- Publication Acknowledgement
- Dispatch Service
- Architecture Review
- Review Fixes

## Runtime

ARB-022.3 [PLANNED]
Pending Message Polling

- findPending()
- Sequential dispatch
- Single worker

ARB-022.4 [PLANNED]
Scheduled Dispatcher

- Spring Scheduler
- Polling lifecycle
- Runtime activation

ARB-022.5 [PLANNED]
Concurrent Dispatch

- Claim semantics
- Multiple workers
- Duplicate dispatch protection

ARB-022.6 [PLANNED]
Dispatch Retry & Backoff

- Retry policy
- Exponential backoff
- Next attempt scheduling
- Dead message handling

ARB-022.7 [PLANNED]
Messaging Runtime Observability

- Metrics
- Tracing
- Runtime health
- Queue monitoring

---

# TRANSPORT

ARB-023 [PLANNED]
Avro Transport & Schema Registry

- Avro Publisher
- Schema Registry
- Subject Strategy
- Contract Evolution
- JSON Development Adapter
- Avro Production Adapter

---

# RESILIENCE

ARB-024 [PLANNED]
Runtime Resilience

- Circuit Breaker
- Bulkhead
- Rate Limiter
- Time Limiter

---

# USER INTERFACE

ARB-UI-001 [DONE]
Customer Portal Prototype

ARB-025 [PLANNED]
Dashboard Backend API

ARB-026 [PLANNED]
Production Customer Portal Integration

---

# DELIVERY & OPERATIONS

ARB-027 [DONE]
Local Runtime Stack

ARB-028 [PLANNED]
Cloud Infrastructure

ARB-029 [PLANNED]
Kubernetes Deployment

ARB-030 [PLANNED]
CI / CD

ARB-031 [PLANNED]
Performance & Resilience Validation

ARB-032 [PLANNED]
Production Readiness

---

# DOCUMENTATION

ARB-DOC-001 [DONE]
Documentation Audit & README Refresh