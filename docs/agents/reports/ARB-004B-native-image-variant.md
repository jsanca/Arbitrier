# ARB-004B — Native Image / Spring AOT Technical Variant

| Field  | Value      |
|--------|------------|
| Task   | ARB-004B   |
| Status | Implemented |
| Date   | 2026-07-07 |

## What Was Documented

| Document | Location | Purpose |
|----------|----------|---------|
| ADR-0007 | `docs/adr/ADR-0007-spring-aot-graalvm-native-image.md` | Decision record for Native Image as a supported runtime variant; records constraints, risks, libraries to validate, and rollback strategy |
| RNF-0002 | `docs/rnf/RNF-0002-native-image-runtime.md` | Non-functional requirements for startup, memory, build, smoke tests, reflection/proxy/resource constraints, observability, and CI/CD |
| README.md | Root | Added Native Image as a supported technical variant in the Technical Baseline table |
| CLAUDE.md | Root | Added Native Image compatibility guardrail under Scope Guardrails |
| AGENTS.md | Root | Added Native Image constraint section warning future agents about native-hostile patterns |
| server/README.md | `server/` | Added Runtime Modes section |
| docs/okf/index.md | `docs/okf/` | Added ADR-0007 and RNF-0002 to the index tables |

## What Was Intentionally Not Implemented

| Area | Reason |
|------|--------|
| `native-maven-plugin` not added to any POM | Task explicitly prohibits POM changes |
| No `RuntimeHintsRegistrar` classes written | No production code; hints are written at the time entities, Avro classes, and JPA adapters are added |
| No `reflect-config.json` or `resource-config.json` | Generated from hints at build time; not hand-authored |
| No GraalVM CI job | Deferred to native build activation task |
| No native Dockerfile | Deferred; base image is an open question per ADR-0007 |
| No smoke test suite | Deferred; requires at least one working HTTP endpoint per service |

## Future Task Candidates

| Candidate Task | Trigger |
|----------------|---------|
| ARB-native-01 — Activate native build for platform and one service | After first JPA entity + Kafka consumer exist |
| ARB-native-02 — Native CI pipeline | After ARB-native-01 passes locally |
| ARB-native-03 — Native Docker image and Kubernetes deployment | After ARB-native-02 is stable |

## Cross-Cutting Constraint Summary

Added to CLAUDE.md and AGENTS.md for all future implementation tasks:

> **Native Image compatibility is a cross-cutting constraint.** Do not introduce reflection, dynamic proxies, runtime classpath scanning, or `Class.forName()` without registering a `RuntimeHintsRegistrar` or equivalent native hint. Document incompatibilities as `OPEN QUESTION`.
