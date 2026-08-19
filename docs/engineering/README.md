# Engineering History

## Purpose

Store the evidence and execution record of how the project changed.

## What belongs here

`ENGINEERING_LOG.md` is the compact index of material work. `agents/tasks/` preserves task specifications when useful; `agents/reports/` holds durable completion reports; `agents/reviews/` holds review records; and `agents/checkpoints/` holds recovery/continuation state. Investigations, validation records, task outcomes, and execution discoveries also belong here.

**Example:** Put an implementation report that records how a payment-state discovery was verified here; put the durable payment-state rule in `../knowledge/`.

## What does not belong here

The durable project/domain explanation itself. Put current reusable understanding in `../knowledge/` and decision rationale in `../adr/`. Do not put a full report body in `ENGINEERING_LOG.md`; link its durable record instead.
