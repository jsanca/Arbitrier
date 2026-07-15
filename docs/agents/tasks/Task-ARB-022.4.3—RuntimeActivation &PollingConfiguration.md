Task: ARB-022.4.3 — Runtime Activation & Polling Configuration

Status:
[PLANNED]

Owner:
Clio

Role:
Implementation

Timebox:
Target: 20–30 minutes
Hard stop: 45 minutes

Apply:

.claude/skills/execution-timebox/SKILL.md
.claude/skills/engineering-reporting/SKILL.md

If interrupted:

Create:

docs/agents/checkpoints/CHECKPOINT-ARB-022.4.3.md

and stop.

--------------------------------------------------

Context

Completed:

ARB-022.4.1
Polling Cycle Overlap Prevention

ARB-022.4.2
Spring Scheduled Polling Adapter

The scheduler currently exists and invokes OutboxPollingService.

The remaining responsibility is runtime activation and configuration.

This slice must remain infrastructure-only.

--------------------------------------------------

Goal

Complete the polling runtime by introducing a dedicated configuration model
and runtime activation policy.

The scheduler should become configurable without code changes.

--------------------------------------------------

1. Configuration Properties

Create a strongly typed configuration class.

Suggested prefix:

arbitrier.messaging.outbox.polling

Suggested properties:

enabled

fixed-delay-ms

initial-delay-ms

batch-size

Use Spring Boot configuration properties.

Avoid scattered @Value usage.

--------------------------------------------------

2. Runtime Activation

The scheduler should only be active when:

enabled=true

Use Spring conditional configuration.

The polling runtime should disappear cleanly when disabled.

Do not register a no-op scheduler.

--------------------------------------------------

3. Batch Size

Move any remaining hard-coded polling batch size into the configuration model.

OutboxPollingService should receive the configured batch size.

The scheduler must remain unaware of batch size.

--------------------------------------------------

4. Defaults

Provide conservative defaults.

Suggested direction:

enabled=true

initial-delay=5 seconds

fixed-delay=10 seconds

batch-size=100

Document every default.

--------------------------------------------------

5. Validation

Validate configuration binding.

Include tests covering:

- default values
- custom property binding
- scheduler disabled
- scheduler enabled
- batch-size propagation

Avoid starting Kafka.

Avoid database access.

--------------------------------------------------

6. Architecture

Verify:

- scheduler owns time only

- polling service owns polling behavior

- configuration owns runtime tuning

- no business logic moves into configuration

--------------------------------------------------

7. Documentation

Create:

docs/agents/reports/ARB-022.4.3-runtime-activation-and-polling-configuration.md

Document:

- configuration model
- activation policy
- defaults
- property namespace
- tests executed

--------------------------------------------------

Out of Scope

Do NOT implement:

- retry

- backoff

- DLQ

- graceful shutdown

- metrics

- tracing

- dynamic reload

- distributed coordination

- claim semantics

- multiple workers

--------------------------------------------------

Acceptance Criteria

✓ ConfigurationProperties class exists.

✓ Scheduler can be enabled or disabled through configuration.

✓ Batch size is configurable.

✓ No @Value scattering.

✓ Tests pass.

✓ Completion report exists.

Do not begin ARB-022.5.