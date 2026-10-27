# 06: How do write operations declare their failure codes?

Type: grilling
Status: open
Blocked by: 01, 02

Map: [map.md](../map.md). Review card 5, "Declared failure codes for write operations", in [architecture-review.html](../architecture-review.html#c5). Contract change allowed (map Notes): the codes become listed in `apply --help <op>`.

## Question

Where are the field-level failure codes of `apply` operations declared, so that `apply --help <op>` lists them, `context.fail` can only raise a declared code, and SkillDriftTest knows them? Is it per OperationSpec, one shared catalog, or the deepened modules from tickets 01 and 03, following ticket 02's "declared by whatever raises it" rule?

Evidence from the review:

- About 54 free-string codes across `apply` (`negative-holding`, `currency-mismatch`, `type-immutable`, `duplicate-target`, ...).
- SkillDriftTest takes known codes only from ErrorSpec and WarningSpec (~148–155). The skill can only say "explain each entry of failures" (SKILL.md ~61).
- Some codes are shared across operations (`required`, `invalid-type`, `reference-not-found`, `duplicate-target`) and come from OperationInput or OperationContext, not from an operation.

Sub-questions to settle: how shared codes are declared once; whether each code's description doubles as its help text; whether a test probes that every declared code is reachable.
