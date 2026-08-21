# Review: OSK-CODE-DOCS-EXP-001 — Cross-Agent Review of `osk-code-docs`

## Review Scope

This review evaluates the installed `osk-code-docs` contract and the available experiment evidence for OSK-CODE-DOCS-EXP-001. It does not change the skill, its manifests or references, Maven configuration, project knowledge, application code, the existing experiment report, or the integration handoff.

Primary records reviewed:

* `.osk/skills/osk-code-docs/SKILL.md`, `skill.yaml`, `README.md`, and both references.
* `docs/engineering/agents/reports/OSK-CODE-DOCS-EXP-001-deep-opencode.md`.
* `docs/engineering/agents/tasks/OSK-CODE-DOCS-INT-001-maven-site-integration.md`.
* `docs/engineering/ENGINEERING_LOG.md`, the current repository state, and generated probe outputs.

## Verdict

`PASS WITH WARNINGS`

The evidence supports `osk-code-docs` as a useful operational contract and supports the experiment's non-publication outcome for the observed Arbitrier state. It does not support strong claims of blind cross-model independence, a documented Qwen result, or a general OpenCode-specific causal explanation for every observed execution issue.

## Findings

| ID | Severity | Classification | Category | Evidence | Impact | Recommendation | Fix / defer |
| --- | --- | --- | --- | --- | --- | --- | --- |
| CDOC-REV-001 | P1 | OBSERVED | Experimental isolation | The committed report identifies Muse Spark. The Kimi third execution is a 69-line uncommitted addition to that report, explicitly re-verifies prior artifacts and says no new publication was attempted. No Qwen-labelled experiment record was found. | The evidence does not demonstrate blind, independent model behavior; later agreement may be verification of shared state. | Future comparisons should use fresh sessions, clean worktrees/local repositories, unique output directories, and model-labelled raw command logs. | Defer to future experiment design. |
| CDOC-REV-002 | P1 | NOT SUPPORTED | Causal generalization | The report's claim that the outcome is a property of repository state rather than model behavior exceeds its contamination controls. The Kimi record starts after prior report, handoff, target/site, and Javadoc artifacts existed. | Compatible outcomes are meaningful but cannot establish model-independent causality. | Describe the observed convergence as state-contaminated re-verification, not an independent reproduction. | Defer; preserve historical wording. |
| CDOC-REV-003 | P2 | OBSERVED | Historical evidence correction | INT-001 preserves an initial `EXIT 0` claim and adds a correction identifying pipe-status capture as the cause; the later report records an `EXIT 1` with log redirect. | The corrected value is traceable, but a reader can still misread the original bullet without its annotation. | Keep the correction annotation and use the corrected result in all future summaries. | Already mitigated; no rewrite. |
| CDOC-REV-004 | P2 | NOT SUPPORTED | Harness attribution | The record demonstrates a shell pipeline-status mistake and reports a dot-directory search workaround. It does not preserve raw OpenCode tool logs sufficient to establish that either is a general OpenCode harness property. Current shell inspection can search dot-directories. | Harness-specific claims risk misattributing shell/tool semantics to OpenCode. | Record these as observed execution/tooling conditions; test them with controlled harness probes before generalizing. | Defer to harness experiment. |
| CDOC-REV-005 | P2 | OBSERVED | Contract-reference integrity | `publication-model.md` and `maven-site.md` link to exact ADR-008 and ADR-011 paths that are absent. | The skill's escalation rationale has stale local references, though its operational boundary remains readable. | Resolve or replace the references only in an authorized OSK maintenance task. | Defer; outside review scope. |
| CDOC-REV-006 | P2 | SUPPORTED | Dependency semantics | The skill declares `osk-engineering-reporting` and `osk-execution-timebox` as required; it declares `osk-knowledge-curator` as a complement. Its workflow routes missing or ambiguous canon to the curator. Arbitrier currently has no installed curator skill and no curated knowledge Markdown beyond the directory README. | The curator is statically complementary but operationally needed before useful narrative publication in this repository state. | Treat this as conditional routing/readiness behavior, not evidence that the manifest relationship is wrong in every repository. | Defer to skill-model research. |

## Skill-Contract Behavior

| Claim | Classification | Assessment |
| --- | --- | --- |
| The skill supplies an operational capability contract rather than only a procedural prompt. | SUPPORTED | The contract defines authority classes, required inputs, expected outcomes, workflow, escalation, quality checks, and explicit non-goals. The experiment followed those structures by classifying sources, declining to edit `pom.xml`, and recording a handoff. Causation is not proven: task constraints and repository instructions could also have produced the same behavior. |
| The skill respects authority classes and prevents invented knowledge. | SUPPORTED | The contract explicitly separates current knowledge, source/API authority, executable evidence, ADRs, historical evidence, and generated publication. The report classified the derived HTML as non-canonical and did not manufacture a Markdown knowledge base. |
| The skill prefers deterministic structural evidence. | OBSERVED | The contract says to prefer deterministic mechanisms, and the report uses file searches, Maven commands, exit codes, and output existence. Structural validation is correctly distinguished from semantic correctness in the contract. |
| The skill prevents project-owned configuration changes without authority. | SUPPORTED | The contract expressly prohibits arbitrary build mutation and the report/handoff records no `pom.xml` change. |
| The skill supports an explicit non-success outcome. | OBSERVED | `SKILL.md` explicitly names `ready to publish`, `published with recorded limitations`, and `INSTALLED_PENDING_INTEGRATION`; the report and engineering log use the third outcome. |

## Readiness and Preconditions

The contract does not define a formal readiness-state taxonomy. The experiment nevertheless supports the following evidence distinctions:

| Condition | Classification | Basis |
| --- | --- | --- |
| Tool availability | EXPLICIT IN CONTRACT | Required inputs include runtime/tool availability. Maven-generated outputs currently exist, but availability alone did not establish an owned workflow. |
| Native integration readiness | EXPLICIT IN CONTRACT | Required inputs require evidence of configured native workflow, command, output location, and validation expectations. Current POM inspection found no configured site/Javadoc/reporting markers and no site descriptor. |
| Knowledge/content readiness | IMPLICIT CONSEQUENCE | The contract requires selected authoritative knowledge and forbids inventing missing facts. `docs/knowledge/` currently contains only its README plus derived HTML, while `docs/PROJECT.md` remains placeholder text. |
| Authorization readiness | EXPLICIT IN CONTRACT | Required inputs include authority to change project-owned configuration; the contract requires a handoff rather than an unauthorized integration change. |
| Publication readiness | EXPLICIT OUTCOME CONDITION | The contract allows only a readiness assessment or handoff when required inputs are unavailable. |

The broader labels `needs knowledge`, `needs authorization`, `blocked`, and `handoff required` are useful behavioral hypotheses, not declared outcome-schema fields in the current skill.

## Outcomes and Handoffs

`INSTALLED_PENDING_INTEGRATION` is an observed, routable outcome: it is declared by the skill, recorded in the experiment report and engineering log, and accompanied by the INT-001 integration handoff. This supports the behavioral concept that an unsatisfied precondition need not be reported as a generic failure.

It does not yet support a generalized list of capability states beyond the three declared outcomes. In particular, `needs knowledge`, `needs authorization`, and `blocked` should remain hypotheses until separate controlled examples demonstrate their semantics and routing.

## Behavioral Portability

The committed experiment record supports one Muse-labelled execution with a later build-mode extension. The working copy adds a Kimi-labelled re-verification, but it was not blind: the report, handoff, target/site output, and Javadoc outputs already existed. No Qwen-labelled execution record was located.

Therefore, the evidence supports bounded conformance to the same contract in the documented runs, but not a strong portability claim across independently isolated agents or models. The observed compatible result is best described as **state-contaminated convergence on an existing pending-integration classification**.

## Deterministic Evidence

Directly observable current evidence supports these structural facts:

* No configured Maven Site/Javadoc/reporting marker was found in the root or server POMs.
* `src/site`, `site`, and `docs/site` are absent.
* Derived site indexes and five service/library Javadoc indexes exist; the Order Service Javadoc index is absent.
* Generated root and module site pages identify themselves as Maven `About` pages.
* The skill's exact ADR-008 and ADR-011 reference paths are absent.

These mechanisms provide a useful verification boundary: they establish file/configuration/output conditions regardless of the agent's prose. They do not establish source correctness, a stable network/runtime environment, native-workflow suitability, or reader usefulness. The historical Maven exit-code and doclint details remain report evidence; this review did not re-run Maven.

## Harness Findings

* **OBSERVED:** the first report's pipeline captured the downstream command's status rather than Maven's; a redirected rerun reported a different Maven result.
* **SUPPORTED:** execution records should preserve the command, direct exit code, and log/output location for later review.
* **HYPOTHESIS:** OpenCode model switching preserves session context or otherwise defeats isolation. This is supplied as experiment context and is compatible with the later state-aware execution, but no raw session transcript or controlled comparison is retained here.
* **NOT SUPPORTED:** that shell pipeline status or dot-directory discovery limitations are general OpenCode-harness properties. Controlled tests are required.

## Repository-Specific Findings

These findings are about Arbitrier, not general OSK principles:

* Curated knowledge Markdown is absent from `docs/knowledge/`; the existing architecture HTML is derived output rather than a canonical Mermaid/Markdown source.
* `docs/PROJECT.md` remains a placeholder template.
* Maven's default Site output exists from probes but lacks evidence of project-owned documentation navigation or source integration.
* The historical report records stale SNAPSHOT resolution and Javadoc/doclint failures; this review did not independently re-run those commands.
* Exact ADR-008/ADR-011 skill references are absent in this repository.

## Positive Findings

* The skill cleanly separates canonical narrative, decisions, historical records, source/API documentation, and derived publication.
* The experiment preserved the project-owned build boundary by creating an integration handoff rather than modifying `pom.xml`.
* The correction annotation preserved historical evidence instead of silently rewriting it.
* Current filesystem evidence still supports the pending-integration assessment: default generated pages exist, but configured native publication inputs do not.

## Deferred Findings

* Whether the outcome vocabulary should be expanded beyond the three current declarations.
* Whether `osk-knowledge-curator` should be modelled as a conditional prerequisite in a future capability system.
* Whether Maven Site/Javadoc is the appropriate project-owned integration after authorization and knowledge curation.
* Controlled evidence for session isolation, tool discovery behavior, and cross-model portability.

## Conclusion

The experiment is valuable evidence that a portable skill can encode authority boundaries, readiness assessment, deterministic validation expectations, and an honest pending-integration handoff. The robust conclusion is about **contract-conformant behavior under the observed repository state**, not about independent model equivalence or harness causality.

Future OSK experiments should pre-register the input commit, use fresh sessions and worktrees, isolate local Maven repositories and output paths, capture raw command logs with direct exit codes, and retain model/harness identifiers. That would turn the current compatible observations into stronger portability evidence.

## References

* [Review task](../tasks/OSK-CODE-DOCS-EXP-001-REVIEW.md)
* [Experiment report](../reports/OSK-CODE-DOCS-EXP-001-deep-opencode.md)
* [Integration handoff](../tasks/OSK-CODE-DOCS-INT-001-maven-site-integration.md)
* [Engineering log](../../ENGINEERING_LOG.md)
* `.osk/skills/osk-code-docs/SKILL.md`
* `.osk/skills/osk-code-docs/references/publication-model.md`
* `.osk/skills/osk-code-docs/references/maven-site.md`
