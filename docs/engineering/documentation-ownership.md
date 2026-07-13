# Documentation Ownership

## Canonical System Documentation

README, ADRs, RF/RNF, service READMEs, security/architecture material, and runtime documentation describe what Arbitrier currently is. Support claims with repository evidence. Put future behavior in the roadmap, planned tasks, open questions, or explicitly deferred decisions; do not present it as current capability.

## Role-Specific Guidance

- **CLAUDE.md** is for the primary implementation agent: coding constraints, build/test commands, and links to shared rules. It does not repeat the whole system description.
- **AGENTS.md** is for review and auxiliary agents: validation commands, safety/architecture checks, handoff expectations, and links to shared rules. It does not duplicate detailed implementation guidance.
- **`docs/engineering/`** contains role-independent rules: task design, timeboxing, reporting, testing strategy, documentation ownership, review taxonomy, and definition of done. Move detailed shared content here when both role guides need it.

## Historical Evidence

Implementation, review, fix, and checkpoint records preserve what happened at the time. Do not overwrite them to make history appear cleaner. Annotate or supersede historical material when later decisions change the current model.

See the [reporting protocol](../../.claude/skills/engineering-reporting/SKILL.md) and [Knowledge Curator role](knowledge-curator.md).
