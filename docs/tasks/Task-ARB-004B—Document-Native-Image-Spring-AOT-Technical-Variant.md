Task: ARB-004B — Document Native Image / Spring AOT Technical Variant

Status:
[COMPLETE]

Owner:
Elito

Context:
Arbitrier currently targets Java 25 and Spring Boot 4.1.0.
Before ARB-005 Domain Model, we want to document a new deployment/runtime variant:
Spring AOT + GraalVM Native Image.

Goal:
Document how Arbitrier will support JVM mode and Native Image mode without changing the business model.

Create:
- docs/adr/ADR-0007-spring-aot-graalvm-native-image.md
- docs/rnf/RNF-0002-native-image-runtime.md
- docs/implementation/ARB-004B-native-image-variant.md

Update:
- README.md
- CLAUDE.md
- AGENTS.md
- server/README.md
- docs/okf/index.md

Key decisions to document:
1. Arbitrier supports two runtime modes:
    - JVM mode for development and regular execution.
    - Native Image mode for deployment/performance experiments.

2. Spring AOT / GraalVM Native Image is a first-class technical variant.

3. Native Image compatibility becomes a cross-cutting constraint for future backend tasks.

4. Future code must be careful with:
    - reflection
    - dynamic proxies
    - classpath scanning assumptions
    - serialization/deserialization
    - JPA/Hibernate native hints
    - Kafka/Avro serialization
    - OpenTelemetry instrumentation
    - resource loading
    - runtime-generated classes

5. Do not implement native build yet unless explicitly requested.
   This task is documentation only.

6. Do not modify production code.

7. Do not add GraalVM Maven plugin yet unless the document explicitly marks it as a future implementation step.

8. Mark unknown compatibility details as OPEN QUESTION.

Required sections:

ADR-0007:
- Context
- Decision
- Consequences
- Accepted constraints
- Native Image risks
- Libraries to validate
- Development mode vs deployment mode
- Rollback strategy

RNF-0002:
- Startup expectations
- Memory expectations
- Build-time expectations
- Native smoke test expectations
- Reflection/proxy/resource constraints
- Observability constraints
- CI/CD constraints

Implementation note:
- What was documented
- What was intentionally not implemented
- Future task candidates

Acceptance Criteria:
- ADR-0007 exists.
- RNF-0002 exists.
- ARB-004B implementation note exists.
- README/CLAUDE/AGENTS/server README mention Native Image as a supported technical variant.
- No Java code is changed.
- No POM dependency/plugin is changed unless clearly documented as a placeholder.
- Future agents are warned not to introduce native-hostile patterns without documenting hints.
- OPEN QUESTIONS are explicit.
