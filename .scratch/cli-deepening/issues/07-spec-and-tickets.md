# 07: Write the refactoring spec and its implementation tickets

Type: task
Status: open
Blocked by: 01, 02, 03, 04, 05, 06

Map: [map.md](../map.md). This ticket reaches the map's destination.

## Question

With every interface decided or ruled out, what goes into the hand-off spec (`.scratch/cli-deepening/spec.md`), and in what order should the implementation tickets go so the suite stays green after each one and each fits in one agent session?

Gather the decisions from the resolved tickets into the spec. Settle the "Migration order" and "Contract-change ripple" patches from the map's **Not yet specified**, plus any others still open. Then write `issues/NN-<slug>.md` implementation tickets with `Status: ready-for-agent`, following the `ai-cli` effort's format (Spec link, "What to build", acceptance criteria, `Blocked by`). For each ticket, name the end-to-end tests it deletes under the map's test preference.
