# OSK-CODE-DOCS-EXP-001 — Apply `osk-code-docs` to Arbitrier

| Field | Value |
|-------|-------|
| Task ID | OSK-CODE-DOCS-EXP-001 |
| Agent | Deep running through OpenCode (muse-spark-1.2-contributor) |
| Date | 2026-08-19 |
| Skill contract | `.osk/skills/osk-code-docs/SKILL.md:1` + `references/publication-model.md` + `references/maven-site.md` (adapter shims at `.opencode/skills/osk-code-docs/SKILL.md:1`, `.claude/skills/osk-code-docs/SKILL.md:1`) |
| Mode | Build (plan → build transition on user `proceed with the plan`) |
| Commit policy | Do not commit changes (per experiment constraints) |

## Objective

Attempt the useful static engineering documentation experience that `osk-code-docs` considers appropriate for Arbitrier, using the installed skill as the operational contract and repository evidence as the driver. Where the skill permits judgment, exercise it. Where integration is missing, produce the readiness/handoff outcome rather than forcing publication.

---

## Repository Observations

### Documentation ecosystem detected

- **Workspace model:** `docs/OSK.md:1` (Information Model: `docs/knowledge/` = current durable truth, `docs/adr/` = decisions/why, `docs/engineering/` = what-happened, `docs/roadmap/` = intent, `docs/roadmap/future/` = non-committed ideas, `.osk/` = OSK-managed state). Classification by purpose required, not by task.
- **Knowledge location:** `docs/knowledge/README.md:1` defines durable domain/architecture store; `docs/knowledge/hexagonal-architecture.html:1` is the only other file in that directory (HTML, not Markdown). No `docs/knowledge/*.md` curated file exists. `docs/PROJECT.md:1` is template placeholder (Mission/Scope/Architecture/Technology all `<!-- placeholder -->`).
- **Decisions:** `docs/adr/` contains ADR-0001 to 0010 (hexagonal, saga/Kafka, schema-per-service, Avro, outbox/inbox, SSE/WebSocket, AOT/GraalVM, W3C trace, inventory ownership, external API entry). Each follows Context→Decision→Consequences. No ADR-008 / ADR-011 as referenced by the skill.
- **Requirements & use-cases:** `docs/okf/UC-01-corporate-bulk-order.md` + `docs/rf/RF-UC-01-corporate-bulk-order.md` + `docs/rnf/` present; `docs/test-cases/` + `docs/diagrams/` + `docs/ui/` present but not published as current knowledge.
- **Engineering history:** `docs/agents/reports/` (75 reports, 60+ ARB-xxx historical), `docs/agents/checkpoints/` (8 OPEN-like checkpoint markers per prior `ls`), `docs/engineering/ENGINEERING_LOG.md:1` (placeholder index table, no entries), `docs/engineering/documentation-ownership.md:1` (AGENTS.md = review/auxiliary, CLAUDE.md = implementation), `docs/engineering/knowledge-curator.md:1` (curator owns reconciliation; must not invent evidence).
- **Stray derived artifact:** `docs/knowledge/hexagonal-architecture.html:1` is 14285-byte standalone HTML with embedded SVG/CSS/fonts, marked `CURRENT-STATE OVERVIEW · UC-01`. It is a derived publication output checked into the knowledge folder, not a canonical Markdown/Mermaid source. Treats generated HTML as authority would violate skill anti-pattern "Site as source of truth".

### Relevant authoritative or candidate sources discovered

| Source path | Authority class (per publication-model.md:7) | Useful? | Evidence |
|-------------|----------------------------------------------|---------|----------|
| `README.md:1` | System description / build metadata | Useful as orientation entry (not curated knowledge) | 77 lines, accurate quick-start + architecture-at-a-glance table + runtime guide pointer |
| `docs/knowledge/*.md` | Current durable knowledge | **Absent** — none beyond README placeholder | `ls docs/knowledge/` = `README.md`, `hexagonal-architecture.html` |
| `docs/knowledge/hexagonal-architecture.html` | Generated publication (derived) | **Must not publish as source** — trace to Markdown/Mermaid missing | Inline SVG, no Mermaid source embedded, no `<!-- Source: -->` provenance |
| `docs/ui/ux_strategy_navigation_map.md:7` | Example / UI material | Candidate — contains canonical Mermaid `flowchart TD` (Login→Dashboard→Orders…) | `grep mermaid docs` hit only this file; valid ` ```mermaid` block, but scope is client prototype, not server domain |
| `docs/adr/ADR-000*` (10 files) | Decision/reference | Candidate — link as Decisions, not current knowledge | Sampled `ADR-0001-project-structure.md:1` is accepted but contains OPEN QUESTION about module wiring |
| `docs/okf/UC-01-corporate-bulk-order.md` | Requirements narrative | Candidate — but OKF is precursor, not curated `knowledge/` | Useful for domain context if knowledge curation elects it |
| `docs/rf/RF-UC-01-corporate-bulk-order.md` | Functional requirement | Candidate — decision-style RF, not current-state knowledge | 12 RF items (negotiation, reservation ordering, confirmation, etc.) |
| `server/*/src/main/java/**/*.java` | Source/API authority | Candidate — public APIs carry Javadoc headers | Sampled `Order.java:1` has class-level Javadoc + invariants; `package-info.java:1` present per `CLAUDE.md` policy |
| `docs/engineering/agents/reports/*.md` | Historical engineering evidence | **Exclude by default** — do not elevate to current truth | 60+ reports; publishing would flatten authority |

### Existing build/documentation integration discovered

- **Maven native ecosystem:** `pom.xml:1` (`arbitrier` aggregator, parent `spring-boot-starter-parent:4.1.0`, `java.version:25`, properties `archunit`, `avro`, `grpc`), `server/pom.xml:1` (Surefire config includes `*IT.java`, no Failsafe), `server/platform/pom.xml`, `server/contracts/pom.xml`, etc. Maven wrapper: not found (`find . -name mvnw` empty). CLI available: `mvn -v` = `Apache Maven 3.9.15`, `java -version` = `Temurin-25.0.2` (both captured 2026-08-19).
- **Site/Javadoc integration:** **Not configured.** Grep for `maven-site-plugin`, `maven-javadoc-plugin`, `maven-project-info-reports-plugin`, `src/site` returned zero hits in any `pom.xml`. No `src/site/site.xml`, no `site/` or `docs/site/` directory, no `mkdocs.yml`/`docusaurus.config.*`. This matches `references/maven-site.md:7` Detect step — Maven presence alone does not imply Site is configured.
- **Client publishing:** `client/package.json:1` scripts `dev/build/lint/test`; Vite + oxlint + vitest. No TypeDoc, VitePress, or Docusaurus docs workflow.
- **Native determinism available:** `mvn javadoc:javadoc` can be invoked ad-hoc by plugin coordinate without `pom.xml` declaration (demonstrated below), but that is not a project-owned reproducible workflow.

### Important missing, ambiguous, stale, or conflicting inputs

| Issue | Severity | Evidence |
|-------|----------|----------|
| No curated `docs/knowledge/*.md` narrative | **Missing — blocks publication of current-state section** | `ls docs/knowledge/` only README + derived HTML; `PROJECT.md` placeholder |
| Canonical diagram source missing | **Missing — blocks standard Mermaid rendering** | `hexagonal-architecture.html` is SVG output, no ` ```mermaid` source; `grep mermaid docs` found only `docs/ui/ux_strategy_navigation_map.md` unrelated to server hex diagram |
| No project-owned Site/Javadoc config | **Missing integration** | Grep 0 hits; `ls src/site` `site` `docs/site` all `No such file` |
| Skill references stale: `ADR-008` and `ADR-011` | **Stale/missing** | Referenced in `.osk/skills/osk-code-docs/references/publication-model.md:86` and `SKILL.md:39` but `docs/adr/` has only ADR-0001–0010 |
| Decision vs. knowledge boundary risk | Ambiguous | `README.md:61` says `docs/implementation/` has no active artifacts after migration; status is described in README + ADRs + RF but not reconciled into `knowledge/` — curator must decide |
| Publication output location & retention | Ambiguous | No `site.publish` or `docs` deploy config; commit policy unknown (commit `target/site` vs. deploy artifact). Skill requires this as Required Input — unavailable, so handoff required |
| Warnings in source Javadoc | Not blocking but noisy | `mvn javadoc:javadoc -pl server/platform` emitted `100 warnings` (no comment) + `2 errors` (invalid input markers) despite `EXIT:0`; indicates incomplete Javadoc coverage, not semantic correctness |

---

## Decisions

| # | Decision | Evidence used | Rationale |
|---|----------|---------------|-----------|
| 1 | Select **Maven Site + Javadoc** as native workflow if any integration is proposed. | `pom.xml:1` aggregator, `java.version:25`, Boot 4.1 parent, no Node docs toolchain in client | Per `references/maven-site.md` ("Read this reference only when Maven is the selected project-native ecosystem") — Maven is the authoritative server ecosystem. Client docs would require a separate TypeDoc/VitePress choice, out of Maven-first experiment scope. |
| 2 | **Do not publish HTML** in this run; produce `INSTALLED_PENDING_INTEGRATION` handoff. | Grep 0 hits for site/javadoc plugins, missing `src/site`, missing knowledge base, placeholder `PROJECT.md` | Skill Escalation Rules: "Missing/ambiguous native integration … create/request an auditable integration task under ADR-008; do not silently install or merge configuration." Silent `pom.xml` mutation would violate Boundaries ("Do not modify project-owned build configuration without repository evidence and explicit authority"). |
| 3 | Route narrative/Mermaid gaps to `osk-knowledge-curator`, not fill with invented content. | `docs/knowledge` emptiness, derived HTML lacking source, Boundaries "Do not guess at missing project facts" | Skill Responsibilities: curator owns reconciliation; publication must not invent completeness. |
| 4 | Classify available sources as: `README.md` (system description, not knowledge), `docs/adr/*` + `docs/okf/index.md` (reference), `server/.../src/main/java` (API reference), `docs/ui/ux_strategy` (example diagram, low relevance to server docs) — and **exclude** engineering reports as flattening authority. | `references/publication-model.md:7` Authority Classes table | Preserves authority-class distinction and traceability; reporting history remains historical. |
| 5 | Judge that ad-hoc `mvn javadoc:javadoc` exit 0 does **not** equal readiness. | Observed warnings/errors + absence of `reporting`/`site.xml` + skill Validation section ("Structural success … does not prove semantic correctness") | Readiness requires project-owned config, version pinning, and site descriptor; ad-hoc CLI invocation is non-reproducible and unpinned. |
| 6 | Use deterministic command probes for structural evidence; reserve agent judgment for selection/synthesis/gap detection. | Skill Responsibilities ("Prefer deterministic mechanisms … reserve agent judgment for selection…") | Probes return reproducible file listings / exit codes; selection of what to publish stays judgmental. |

---

## Execution

### Commands executed

```bash
# Orientation (skill + workspace)
cat .osk/skills/osk-code-docs/SKILL.md
cat .osk/skills/osk-code-docs/references/publication-model.md
cat .osk/skills/osk-code-docs/references/maven-site.md
cat .osk/skills/osk-code-docs/README.md
cat .osk/skills/osk-code-docs/skill.yaml
cat docs/OSK.md; cat docs/PROJECT.md; cat docs/knowledge/README.md
ls -R docs/ | head -n 120
ls docs/adr/*.md
ls docs/knowledge/; file docs/knowledge/hexagonal-architecture.html

# Build-native detection
grep -R "maven-site|site|javadoc" pom.xml server/pom.xml server/*/pom.xml   # → 0 hits
grep -R "maven-javadoc|maven-site" .                                        # → only skill refs
find . -maxdepth 4 -name mvnw -print; ls src/site; ls site; ls docs/site    # → all "No such file"
grep -r mermaid docs 2>&1                                                    # → only docs/ui/ux_strategy_navigation_map.md
cat docs/ui/ux_strategy_navigation_map.md | head -n 80
mvn -v                                                                        # → Maven 3.9.15, Java 25.0.2 Temurin
java -version                                                                 # → 25.0.2 LTS
cat pom.xml | head -n 40; cat server/pom.xml | head -n 40
cat server/order-service/pom.xml | head -n 100
grep -R "site|documentation" .osk/skills/osk-code-docs/README.md

# Structural API probe (deterministic native generation without owned integration)
mvn javadoc:javadoc -pl server/platform -am -B --no-transfer-progress
# → EXIT 1 (CORRECTION: first run reported EXIT 0 — that was a pipe-to-tail artifact;
#   re-run with log redirect on second execution confirmed true exit code 1),
#   2 doclint errors, 100 warnings, plugin default 3.12.0 (not project-pinned)
ls server/platform/target/reports/apidocs  # → allclasses-index.html, index.html, overview-tree.html, etc.

# Site probe (expected pending-integration)
mvn site -B --no-transfer-progress
# → exceeded 120000ms timeout in dependency re-verification (see second execution for root cause)

# Domain source spot-checks
grep -R "package-info.java" server --include="*.java" | head
cat server/order-service/src/main/java/com/arbitrier/order/domain/model/Order.java | head -n 30
cat server/order-service/src/main/java/com/arbitrier/order/domain/model/package-info.java
ls docs/agents/tasks/ | head; ls docs/agents/reports/ | tail
```

### Files created

| Path | Kind | Notes |
|------|------|-------|
| `docs/engineering/agents/tasks/OSK-CODE-DOCS-INT-001-maven-site-integration.md` | Auditable integration handoff task | Created per skill Workflow Step 4 / Escalation Rules. Records detection evidence, proposed conservative `pom.xml`+`src/site` merge, constraints, verification expectations, ownership. No `pom.xml` modified. |
| `docs/engineering/agents/reports/OSK-CODE-DOCS-EXP-001-deep-opencode.md` | This report | Durable experiment report per task deliverable. |
| `server/*/target/reports/apidocs/` (5 of 6 code modules) and `*/target/site/` (all 8 modules) | Transient derived output | Produced by ad-hoc probes on first and second execution. Not claimed publications; untracked derived output under `target/`. Cleaning is safe; report records their presence structurally. |

### Files modified

None. No `pom.xml`, no `src/site/`, no `docs/knowledge/*.md`, no installed skill files were modified. Working tree modifications are limited to the two files above plus transient `target/` directories.

### Generated artifacts (claimed)

None claimed as publication. Generated directories inspected were the ad-hoc Javadoc probes (`server/*/target/reports/apidocs/`) and the default-plugin site probe (`*/target/site/`, second execution). The default site pages are generic Maven project-info output with no project knowledge, navigation descriptor, API binding, or Mermaid — they are probe evidence, not a documentation experience. No rendered Mermaid SVGs beyond the pre-existing checked-in `hexagonal-architecture.html`; no mirrors.

### Validation commands and results

| Validation | Command | Result | Interpretation |
|------------|---------|--------|----------------|
| Javadoc plugin ad-hoc | `mvn javadoc:javadoc -pl server/platform -am -B --no-transfer-progress` | **EXIT 1** (corrected from first-run "EXIT 0" pipe artifact), 2 doclint errors / 100 warnings; files at `server/platform/target/reports/apidocs/index.html` present | Structural Javadoc generation is **technically feasible** but fails strict doclint, is not project-pinned, and resolves sibling SNAPSHOTs from `~/.m2` (stale). Not a readiness signal. |
| Site generation | `mvn site -B --no-transfer-progress` | Timeout >120s stuck in dependency re-verification; offline variant completes (see second execution) | No project-owned site workflow — pending integration. Default-plugin site is generic project-info only. |
| Output presence | `ls server/platform/target/reports/apidocs`, `ls src/site`, `ls site` | apidocs present, sites absent | Confirms Javadoc probe output vs. missing site integration. |
| Mermaid source | `grep -r mermaid docs` | Only `docs/ui/ux_strategy_navigation_map.md:7` | No canonical Mermaid source for hexagonal architecture — rendering not applicable. |
| Knowledge base | `ls docs/knowledge/` | `README.md` + derived `hexagonal-architecture.html` | No narrative Markdown to publish — gap to curator. |

Structural success of `javadoc:javadoc` does **not** prove semantic documentation quality (skill Boundaries). No link-resolution check or navigation check was applicable beyond output presence.

### Failures or retries

- `grep -r "mermaid\|Mermaid\|```mermaid"` initially failed with `zsh: unmatched "` due to quoting; retried as `grep -r mermaid docs` — succeeded.
- `mvn site` timed out at default 120000ms — retried on second execution with 300s and offline mode; root cause identified as local-repository re-verification of cached BOMs (see second execution).
- First-run `mvn javadoc:javadoc ... | tail` reported `EXIT:0` — **incorrect**: `$?` captured `tail`'s status, not Maven's. Second execution re-ran with log-file redirect and captured the true exit code **1** (MojoExecutionException from 2 doclint errors). This report's first-run "EXIT 0" claims are corrected accordingly. Recorded as an OpenCode/harness observation: pipe-based exit capture is unreliable.

### Requested or required handoff

- **Handoff task:** `docs/engineering/agents/tasks/OSK-CODE-DOCS-INT-001-maven-site-integration.md` — auditable, authorized integration under ADR-008 equivalent.
- **Routing of gaps:** narrative/Mermaid gaps → `osk-knowledge-curator` (owner per `.osk/skills/osk-code-docs/SKILL.md:108` Dependencies); build integration gaps → maintainer via the handoff task; stale ADR references → curator/maintainer to create ADR-008/ADR-011 or correct skill references.
- **Timebox:** No hard-stop checkpoint needed; work completed inside the 20–30 min target for this experiment.

---

## Final Outcome

### Classification

**`INSTALLED_PENDING_INTEGRATION`**

One of the three outcomes defined in `.osk/skills/osk-code-docs/SKILL.md:55`:

> "A stated outcome: ready to publish; published with recorded limitations; or `INSTALLED_PENDING_INTEGRATION`."

### Why this outcome (not the other two)

- **Not `READY_TO_PUBLISH`:** The skill requires evidence of a native documentation workflow (Describe Required Inputs: "Evidence of the project's native documentation ecosystem, existing configuration, available commands, expected output location, and validation expectations"). No `maven-site-plugin`/`maven-javadoc-plugin` in `pom.xml`, no `src/site/site.xml`, no output location policy. Readiness cannot be claimed (Anti-Pattern "False readiness").
- **Not `PUBLISHED_WITH_RECORDED_LIMITATIONS`:** Publishing would require using an approved native workflow to render Markdown/Mermaid, generate API reference, and build the site (Workflow Step 5). No approved workflow exists. Forcing a site via ad-hoc CLI or synthetic Markdown would invent knowledge and flatten authority, violating "Do not guess at missing project facts" and "Do not claim that a successful render proves semantic correctness."
- **`INSTALLED_PENDING_INTEGRATION` fits:** The skill package is installed at `.osk/skills/osk-code-docs/` (verified), Maven (the Maven-first native direction) is the correct ecosystem for this Java project, but project-specific build configuration and knowledge curation are required before publication. The handoff preserves inspection evidence and requests auditable, authorized integration.

### What would change the classification

- Merging the handoff task's conservative `pom.xml` + `src/site` change and pinning versions with Java 25 validation (`mvn site` → `target/site/index.html` with navigation to apidocs), **and**
- Curator supplying at least one `docs/knowledge/*.md` durable narrative and a canonical Mermaid source for any published diagram,
would move the next run to `published with recorded limitations` or `ready to publish`.

---

## Experimental Integrity — Skill Applicability Observations

> "Do not modify `osk-code-docs`, its references, or its contract as part of this task." — Experiment constraints

1. **Stale ADR cross-references:** Publication model references `ADR-011` and `ADR-008` that do not exist in `docs/adr/` (only ADR-0001–0010). An agent cannot trace authority without the missing decision record. Recorded as conflicting input rather than corrected.
2. **Derived artifact in knowledge folder:** `docs/knowledge/hexagonal-architecture.html` is a styled HTML page with embedded SVG, not Markdown/Mermaid. Skill treats Mermaid in canonical Markdown as the textual diagram source. Preserving this file as-is risks the anti-pattern "Site as source of truth" unless a Markdown/Mermaid source is authored in `docs/knowledge/` and HTML is regenerated.
3. **Maven Site as first direction is appropriate here but not universal:** For this Java/Maven project with Java Javadoc discipline, Maven Site + Javadoc is the correct native choice. The skill's portability (polyglot, no universal plugin stack) prevented a wrong vendor lock-in; the Maven-first reference was read only after confirming Maven as ecosystem, as instructed.
4. **Ad-hoc `javadoc:javadoc` is a false-readiness trap in both directions:** With `failOnError=false` the reactor run reports BUILD SUCCESS while silently resolving **stale sibling SNAPSHOT jars from `~/.m2`** (order-service apidocs missing because the installed `arbitrier-platform` jar predates `messaging/event`); without it, the run fails on strict doclint. Either way, ad-hoc CLI results cannot be read as workflow readiness. Skill Boundaries correctly prevent that inference by requiring project-owned configuration and authority.
5. **Knowledge curation is the actual blocker, not toolchain:** Even with Site wired, a site without curated `docs/knowledge/*.md` would be an empty shell of ADRs and history. The dependency `osk-knowledge-curator` being marked `complements` understates the sequencing — knowledge must be curated before publication is useful.
6. **No OpenCode workaround needed:** All native publishers remain project-owned; no `.claude/skills` hook or `diagram-design` enrichment was required for v0.1. All inspection used deterministic CLI/grep/ls, per skill guidance.

---

## Success Criteria (per experiment definition)

| # | Criterion | Met? | Evidence |
|---|-----------|------|----------|
| 1 | Installed `osk-code-docs` skill actually used as operating contract | ✅ | All steps traced to `.osk/skills/osk-code-docs/SKILL.md` + references |
| 2 | Repository evidence drives execution | ✅ | Every decision cites `grep`, `ls`, `mvn -v`, file reads |
| 3 | Skill boundaries and authority rules respected | ✅ | No invented facts, no silent `pom.xml` edit, no generated-HTML hand-edit, authority classes preserved |
| 4 | Structural validation performed where possible | ✅ | Full-reactor `javadoc:javadoc` probe (5/6 apidocs present, order-service root-caused to stale `~/.m2` SNAPSHOT), `mvn site -o` completed (8/8 default entry pages), exit codes captured via log redirect |
| 5 | Unsupported assumptions not presented as facts | ✅ | Gaps explicitly listed as missing/ambiguous |
| 6 | Final state and limitations reported accurately | ✅ | No site claimed; `INSTALLED_PENDING_INTEGRATION` with limitations |
| 7 | Skill applicability observations preserved without modifying skill | ✅ | Section above; skill files untouched |

A generated documentation site is **not required** for success — correctly determined not ready.

---

## Second Execution (build mode, 2026-08-19 ~14:40)

The task was re-issued in build mode. Repository state relevant to readiness was unchanged (no site/javadoc plugin declarations in any `pom.xml`, no `src/site/`, knowledge base still empty). This execution deepened structural validation with correct exit-code capture and full-reactor probes. No additional files were created or modified beyond this report, the handoff task, and the engineering-log row; no `pom.xml`, `src/site/`, knowledge, or skill files were touched.

### Additional commands executed

```bash
# Correct exit-code capture (first run's EXIT:0 was a pipe-to-tail artifact)
mvn javadoc:javadoc -pl server/platform -am -B --no-transfer-progress > /tmp/javadoc-platform.log 2>&1; echo "EXIT:$?"
# → EXIT:1; maven-javadoc-plugin 3.12.0 (default-resolved, not project-pinned)
# → 2 errors: OutboxPollingProperties.java:18 "no caption for table"; SafeLoggable.java:7 "reference not found"; 100 warnings

# Full-reactor API-reference probe (lenient mode)
mvn javadoc:javadoc -B --no-transfer-progress -Dmaven.javadoc.failOnError=false > /tmp/javadoc-all.log 2>&1; echo "EXIT:$?"
# → EXIT:0, BUILD SUCCESS, 01:40 min, 8 modules (2 pom modules skipped)
# → apidocs/index.html PRESENT: platform, contracts, inventory-service, credit-service, orchestrator-service
# → apidocs/index.html MISSING: order-service — javadoc subprocess exit 1:
#   "package com.arbitrier.platform.messaging.event does not exist" (OrderCreatedDomainEvent.java:7, TransportEventMetadata.java:3)

# Root-cause check for order-service failure
unzip -l ~/.m2/repository/com/arbitrier/arbitrier-platform/0.0.1-SNAPSHOT/arbitrier-platform-0.0.1-SNAPSHOT.jar | grep -c "messaging/event"
# → 0 — installed SNAPSHOT jar is stale; predates DomainEvent/EventDescriptor (ARB-024.2)

# Site probes
mvn site -B --no-transfer-progress > /tmp/site.log 2>&1            # killed at 300s: stuck re-verifying cached BOMs
mvn site -o -B --no-transfer-progress > /tmp/site-offline.log 2>&1 # killed at 180s: mid-generation, module 5/8
mvn site -o -B --no-transfer-progress > /tmp/site-offline2.log 2>&1; echo "EXIT:$?"
# → EXIT:0, BUILD SUCCESS, 05:49 min; target/site/index.html PRESENT in all 8 modules
# → default binding: maven-site-plugin 3.12.1 + maven-project-info-reports-plugin 3.9.0
# → root site = 13 generic reports (dependencies, dependency-management, modules, plugins, scm, team, ...)
```

### Additional findings

1. **Default Maven site generation works offline** (`mvn site -o`, EXIT 0, all 8 entry pages present) but produces only generic project-info pages: no `src/site/site.xml` descriptor, no reader navigation, no knowledge/ADR content, no Javadoc binding, no Mermaid. It is default lifecycle behavior, not a project-owned documentation workflow.
2. **Online `mvn site` stalls in dependency re-verification**: the local repository was cached under a different repository ID, so Maven re-verifies hundreds of transitive BOM POMs against remotes (including `apache.snapshots`) before any generation. Environment gotcha; offline mode bypasses it.
3. **Ad-hoc Javadoc resolves sibling SNAPSHOTs from `~/.m2`, not the reactor.** The installed `arbitrier-platform` jar lacks `messaging/event` (0 entries), so order-service apidocs cannot generate until `mvn install` refreshes the local repo or a project-owned plugin configuration handles resolution. Any INT-001 verification must account for this.
4. **True platform Javadoc status is failing** (EXIT 1, 2 doclint errors) — the first execution's "EXIT 0" was a harness artifact (pipe-based `$?`). Project-owned integration must decide doclint policy (`-Xdoclint`, `failOnError`) explicitly.

### Outcome re-affirmed

**`INSTALLED_PENDING_INTEGRATION`** — unchanged. The second execution strengthened the evidence for the same classification: default-plugin generation is possible but is not an approved native workflow (no project configuration, no navigation, no knowledge/API integration), and the API-reference pillar fails without local-repo hygiene and doclint policy. Handoff `OSK-CODE-DOCS-INT-001` updated with these findings; no publication claimed.

---

## Third Execution (build mode, 2026-08-19, kimi-k3)

The same experiment was issued a third time under a different model — Deep running through OpenCode, model `opencode-go/kimi-k3` (prior executions: `muse-spark-1.2-contributor`) — to support the experiment's cross-agent/cross-harness comparison goal. Approach: independent verification of the previously recorded evidence before relying on it (per `osk-engineering-reporting` evidence rules), then annotation rather than rewrite of the committed prior record. No new publication attempt was made, because readiness state was re-confirmed as absent.

### Commands executed (third execution)

```bash
# Provenance of prior artifacts
git status --porcelain
# → only untracked: .osk/, .opencode/skills/{diagram-design,osk-code-docs}/, .claude/skills/{diagram-design,osk-code-docs}/, .github/, .env.example
git ls-files docs/engineering/agents/reports/OSK-CODE-DOCS-EXP-001-deep-opencode.md docs/engineering/agents/tasks/OSK-CODE-DOCS-INT-001-maven-site-integration.md
# → both tracked
git log -1 --format="%H %ad %s" -- docs/engineering/agents/reports/OSK-CODE-DOCS-EXP-001-deep-opencode.md
# → ee0270df3bbb5ffca18536669d72467fba837c1f Wed Aug 19 16:10:31 2026 -0600 (HEAD) — prior report + handoff committed by maintainer after the agent runs (agents themselves did not commit, per constraint)

# Readiness re-verification
grep -R "maven-site-plugin\|maven-javadoc-plugin\|maven-project-info-reports-plugin" pom.xml server/pom.xml server/*/pom.xml
# → 0 hits (grep EXIT 1) — still no project-owned site/javadoc/reporting configuration
ls src/site site docs/site
# → all "No such file or directory"
ls docs/knowledge/ ; head -30 docs/PROJECT.md
# → README.md + hexagonal-architecture.html only; PROJECT.md still all-placeholder
mvn -v ; java -version
# → Apache Maven 3.9.15, Temurin 25.0.2 — unchanged from prior record
ls mvnw
# → absent; system mvn remains the project command convention
grep -iE "typedoc|vitepress|docusaurus|mkdocs" client/package.json
# → 0 hits; client scripts remain dev/build/lint/test/preview only

# Checkpoint gate (osk-engineering-reporting: read OPEN checkpoints before reporting)
grep -l "OSK-CODE-DOCS" docs/agents/checkpoints/*.md
# → no match — no checkpoint exists for this task; ARB-022.x/ARB-OBS-001 checkpoints are unrelated (spot-checked one: RESOLVED)

# Persistence of prior derived outputs
ls -d server/*/target/reports/apidocs
# → 6 dirs persist from prior probes
ls server/order-service/target/reports/apidocs/index.html server/platform/target/reports/apidocs/index.html server/contracts/target/reports/apidocs/index.html
# → order-service index.html ABSENT (partial dir), platform + contracts PRESENT — matches recorded stale-SNAPSHOT failure
ls target/site/index.html server/target/site/index.html server/platform/target/site/index.html
# → all present — prior offline default-site probe output persists (root + server aggregator + 6 modules = 8 entry pages)
unzip -l ~/.m2/repository/com/arbitrier/arbitrier-platform/0.0.1-SNAPSHOT/arbitrier-platform-0.0.1-SNAPSHOT.jar | grep -c "messaging/event"
# → 0 — jar dated Jul 18 11:58, still stale; order-service apidocs root cause unremediated

# Independent reproduction of the platform Javadoc probe (log redirect, not pipe, to avoid the first execution's exit-code artifact)
mvn javadoc:javadoc -pl server/platform -am -B --no-transfer-progress > /tmp/osk-exp001-r3-javadoc-platform.log 2>&1; echo "EXIT:$?"
# → EXIT:1 — BUILD FAILURE
grep -E "error:" /tmp/osk-exp001-r3-javadoc-platform.log
# → OutboxPollingProperties.java:18 "error: no caption for table"; SafeLoggable.java:7 "error: reference not found"
grep -cE "warning:" /tmp/osk-exp001-r3-javadoc-platform.log ; grep -E "[0-9]+ error|[0-9]+ warning" /tmp/osk-exp001-r3-javadoc-platform.log | tail -2
# → 100 warnings; "2 errors" / "100 warnings" — identical to the recorded second-execution result; plugin default-resolved maven-javadoc-plugin:3.12.0
```

### Findings (third execution)

1. **All key structural claims reproduce exactly under a different model.** The platform Javadoc probe returned EXIT 1 with the identical 2 doclint errors and 100 warnings; the stale `~/.m2` platform SNAPSHOT (0 `messaging/event` entries, Jul 18 jar) still explains the missing order-service `apidocs/index.html`; prior offline-site and apidocs probe outputs persist on disk and match the recorded claims.
2. **Readiness state is unchanged.** No site/javadoc/reporting plugin in any `pom.xml`, no `src/site/`, `docs/knowledge/` still without curated Markdown, `docs/PROJECT.md` still placeholder, handoff INT-001 still "Proposed — awaiting authorization".
3. **Prior experiment artifacts are now committed history** (HEAD `ee0270d`, 2026-08-19 16:10 -0600). This execution therefore annotates rather than rewrites: this section was appended, and INT-001's stale "exited 0" bullet received a correction annotation with original text preserved.
4. **INT-001 internal inconsistency confirmed and annotated:** its evidence bullet 2 carries the first-run "exited 0" claim while a later bullet carries the corrected EXIT 1. Annotated in place; no historical text removed.
5. **Harness observation (OpenCode + kimi-k3):** the file-search tools do not match inside dot-directories (`.osk/`, `.opencode/`); skill/reference discovery fell back to shell `ls`. No semantic impact — recorded as an OpenCode/harness workaround observation per experiment integrity, mirroring the first execution's pipe-capture artifact observation.
6. **First execution's exit-code trap avoided by construction:** all exit codes captured via log redirect (`> log 2>&1; echo "EXIT:$?"`), never through a pipe.

### Outcome re-affirmed (third execution)

**`INSTALLED_PENDING_INTEGRATION`** — unchanged across three executions and two models. No approved native workflow exists (skill Required Inputs unmet: no project-owned configuration, no output-location/retention policy), no curated knowledge narrative exists, and no authorization exists to mutate project-owned build configuration (experiment constraints + skill Boundaries). No new handoff was required; `OSK-CODE-DOCS-INT-001` remains the open, auditable integration task and gap routing (`osk-knowledge-curator` for knowledge/Mermaid canon, maintainer for ADR-008/ADR-011 reference resolution) is unchanged.

Cross-agent comparison note: the second and third executions converge on the same classification, the same blocking inputs, and the same handoff, with independent reproduction of the decisive probe evidence — the outcome is a property of repository state, not of the executing model.

---

## References

- `.osk/skills/osk-code-docs/SKILL.md:1` — operating contract
- `.osk/skills/osk-code-docs/references/publication-model.md:1` — authority classes, lifecycle, traceability
- `.osk/skills/osk-code-docs/references/maven-site.md:1` — native mapping, preservation, verify
- `docs/OSK.md:1`, `docs/PROJECT.md:1`, `docs/knowledge/README.md:1`
- `docs/adr/ADR-0001-project-structure.md:1`, `docs/rf/RF-UC-01-corporate-bulk-order.md:1`
- `docs/ui/ux_strategy_navigation_map.md:1` (Mermaid source)
- `docs/engineering/documentation-ownership.md:1`, `docs/engineering/knowledge-curator.md:1`
- Integration handoff: `docs/engineering/agents/tasks/OSK-CODE-DOCS-INT-001-maven-site-integration.md`
