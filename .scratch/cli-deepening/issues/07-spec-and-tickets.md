# 07: Write the refactoring spec and its implementation tickets

Type: task
Status: open
Blocked by: 01, 02, 03, 04, 05, 06

Map: [map.md](../map.md). This ticket reaches the map's destination.

Graduated from the map's fog after ticket 06: the spec must list the contract-change ripple as work, nothing left to decide:

- Help text, skill (`SKILL.md`) and SkillDriftTest updates for the `Change.entity` enum, the declared failure codes, the multi-choice `--type` help line, and any `Response meta:` line the golden help test shows changed (ticket 02). From ticket 05: the `--currency` help line, if the typed option renders differently, and Allocation's possible count-1 `exchange-rate-missing` when only core lacks a rate. The `Change.entity` enum (ticket 03) is now fixed. From ticket 06: the new "Failure codes:" section of `apply --help <op>`, the pointer line and two batch codes on `apply --help`, and SKILL.md's `validation-failed` row pointing at `apply --help <op>`.

## Question

With every interface decided or ruled out, what goes into the hand-off spec (`.scratch/cli-deepening/spec.md`), and in what order should the implementation tickets go so the suite stays green after each one and each fits in one agent session?

Gather the decisions from the resolved tickets into the spec. Settle the "Migration order" and "Contract-change ripple" patches from the map's **Not yet specified**, plus any others still open. Then write `issues/NN-<slug>.md` implementation tickets with `Status: ready-for-agent`, following the `ai-cli` effort's format (Spec link, "What to build", acceptance criteria, `Blocked by`). For each ticket, name the end-to-end tests it deletes under the map's test preference.

Also carry ticket 04's follow-up into the spec: the transfer-rate HALF_UP site (TransferOperations) and the allocation double `share` site (AllocationCommand) each get a one-line comment naming the core or UI site they mirror (ADR 0007).
