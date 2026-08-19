# Task: OSK-CODE-DOCS-INT-001 — Maven Site and Javadoc Integration for Arbitrier

| Field | Value |
|-------|-------|
| Task ID | OSK-CODE-DOCS-INT-001 |
| Parent Experiment | OSK-CODE-DOCS-EXP-001 |
| Created | 2026-08-19 |
| Created by | Deep (OpenCode) — per `osk-code-docs` handoff |
| Status | Proposed — awaiting authorization |
| ADR Basis | ADR-008 (agent-performed project integration) — to be created/adopted; publication model at `.osk/skills/osk-code-docs/references/publication-model.md` |

## Objective

Provide the project-owned, auditable Maven Site and Javadoc build integration that `osk-code-docs` requires before any static engineering publication can be claimed ready. Do not publish HTML until this integration is merged and validated.

## Detected Evidence (why this task exists)

- **Maven Present, Site Absent:** `pom.xml:1` declares `arbitrier` parent `spring-boot-starter-parent:4.1.0`, Java 25, aggregator `server/pom.xml`. No `maven-site-plugin`, `maven-javadoc-plugin`, or `maven-project-info-reports-plugin` in any `pom.xml` (`grep -R maven-site\|maven-javadoc` across `.` returned only skill references). No `src/site/`, `site/`, or `docs/site/` directory.
- **Ad-hoc Javadoc Succeeds with Warnings:** `mvn javadoc:javadoc -pl server/platform -am -B --no-transfer-progress` invoked without project-owned config **exited 0** but emitted `2 errors` + `100 warnings` (`no comment` / `use of default constructor`) and produced `server/platform/target/reports/apidocs/` (observed 2026-08-19). This proves the toolchain can generate API reference but is not project-pinned, not configured for multi-module aggregation, and not validated for site navigation.
- **Site Generation Not Wired:** `mvn site -B --no-transfer-progress` at repository root stalls in dependency re-verification (local repo cached under a different repository ID; Maven re-verifies hundreds of transitive BOM POMs against remotes). Offline variant `mvn site -o -B --no-transfer-progress` completes: EXIT 0, 05:49 min, default `maven-site-plugin:3.12.1` + `maven-project-info-reports-plugin:3.9.0`, `target/site/index.html` present in all 8 modules — but output is 13 generic project-info reports only (no descriptor, navigation, knowledge, Javadoc, or Mermaid). No `site.xml`; no reporting section binding Javadoc (observed 2026-08-19, second execution).
- **Stale SNAPSHOT Hazard for API Reference:** `mvn javadoc:javadoc -B -Dmaven.javadoc.failOnError=false` → BUILD SUCCESS (01:40 min) with `apidocs/index.html` in 5 of 6 code modules; **order-service missing** because ad-hoc javadoc resolves sibling SNAPSHOTs from `~/.m2`, and the installed `arbitrier-platform-0.0.1-SNAPSHOT.jar` contains 0 `messaging/event` entries (predates `DomainEvent`/`EventDescriptor`). Integration verification must `mvn install` first or otherwise guarantee reactor-fresh resolution.
- **Doclint Policy Required:** `mvn javadoc:javadoc -pl server/platform -am` → EXIT 1 with default-resolved plugin 3.12.0: 2 errors (`OutboxPollingProperties.java:18` no caption for table; `SafeLoggable.java:7` reference not found) + 100 warnings. Integration must pin the plugin version and declare doclint/`failOnError` policy explicitly.
- **Canonical Knowledge Gaps:** `docs/knowledge/` contains only `README.md:1` (purpose statement) and `hexagonal-architecture.html:1` (14285 bytes, generated HTML with inline SVG — derived output, not Markdown/Mermaid source). `docs/PROJECT.md:1` is placeholder. No `docs/knowledge/*.md` durable narrative exists, so site publication has no authoritative narrative input. `docs/ui/ux_strategy_navigation_map.md:1` contains one canonical Mermaid `flowchart TD` diagram but is classified as UI material, not durable knowledge.
- **Stale Skill References:** `.osk/skills/osk-code-docs/references/publication-model.md:86` references `ADR-011` and `ADR-008`; `.osk/skills/osk-code-docs/SKILL.md` references same. Neither ADR exists in `docs/adr/` (observed `ls docs/adr/*.md` lists ADR-0001 through ADR-0010 only).

## Constraints

- Java 25 toolchain must remain (`pom.xml:26` `java.version:25`); any plugin version must declare Java 25 support.
- Do not replace existing `pom.xml` parent inheritance, `pluginManagement`, or `flyway`/`jpa` config. Minimal conservative merge only.
- Do not invent canonical Markdown/Mermaid to fill gaps. Site must publish only selected authoritative sources.
- Generated `target/site/` / `target/reports/apidocs` remain derived, not canonical. Correct source, then regenerate.
- Requires explicit authorization before modifying `pom.xml` (per skill Boundaries: "Do not modify project-owned build configuration without repository evidence and explicit authority. Follow ADR-008 when integration remains necessary").

## Proposed Integration (smallest conservative merge)

1. **Parent `pom.xml`** — add `build/pluginManagement` declaration pinning compatible versions (example candidates to verify against Boot 4.1 / Java 25): `maven-site-plugin:3.9.1` (Doxia Site 1.9.x), `maven-javadoc-plugin:3.11.x`, `maven-project-info-reports-plugin:3.6.x`. Verify each declares Java 25 support before pinning; record verification command `mvn -v` / plugin docs link.
2. **`server/pom.xml` or parent `reporting` section** — add `<reporting>` binding Javadoc aggregates for `order-service`, `inventory-service`, `credit-service`, `orchestrator-service`, `platform`, `contracts`. Keep `platform`/`contracts` as library jars; exclude `client` (Vite/React — not Maven).
3. **`src/site/site.xml`** (or aggregator-level) — minimal site descriptor with reader-oriented navigation: `index` (README-derived orientation, flagged as system description not knowledge), `architecture`, `decisions` (linked ADRs), `api` (aggregated Javadoc), `history` (engineering log, flagged historical). Omit curated knowledge sections until `osk-knowledge-curator` supplies `docs/knowledge/*.md`.
4. **`src/site/markdown/index.md`** — traceable markdown that links to `README.md` and `docs/knowledge/README.md` as source, not a copy. Preserve source-to-output traceability.
5. Validation before claiming ready: `mvn -B site`, `mvn -B javadoc:aggregate-javadoc`, check `target/site/index.html` presence, `apidocs/` presence, internal link resolution, Mermaid rendering for any selected canonical diagram (none currently qualified — standard rendering only if Mermaid source supplied).

## Verification Expectations

```
mvn -B install -DskipTests --no-transfer-progress   # refresh ~/.m2 SNAPSHOTs first (stale-jar hazard, see evidence)
mvn -B site --no-transfer-progress                  # expect 0, generates target/site/index.html (use -o if remote re-verification stalls)
mvn -B javadoc:aggregate-javadoc                    # expect 0 with project-pinned doclint policy
ls target/site/index.html && ls target/site/apidocs/index.html
# Optional: link check where practical; no semantic correctness claim from passing build
```

Record actual exit codes, output paths, warnings (existing Javadoc shows 100 warnings — acceptable structural success, not semantic approval).

## Out of Scope

- Curating `docs/knowledge/*.md` narrative or recreating Mermaid source for `hexagonal-architecture.html`. Route to `osk-knowledge-curator`.
- Installing Node-based publishers (Docusaurus/MkDocs/VitePress) for client docs. Java site does not own `client/` docs workflow.
- Rich `diagram-design` SVG generation. Standard Mermaid rendering is v0.1 baseline.

## Handoff Ownership

- **Owner:** Maintainer / Knowledge Curator (per `docs/engineering/knowledge-curator.md:4`).
- **Next reviewer compares:** This task vs. report at `docs/engineering/agents/reports/OSK-CODE-DOCS-EXP-001-deep-opencode.md` to see detection → pending-integration → validation flow without silent pom mutation.
- **Blocked until:** Authorization to create ADR-008 equivalent or explicit approval to modify `pom.xml`.

## Evidence to Preserve on Completion

- Diff of changed `pom.xml` / new `src/site/` files.
- `mvn -v` / `java -version` captures.
- `mvn site` and `mvn javadoc:aggregate-javadoc` logs (exit code, warnings).
- Output directory listing and entry-page screenshot or `ls`.
- Remaining gaps explicitly listed (knowledge base, Mermaid source, stale ADR refs).
