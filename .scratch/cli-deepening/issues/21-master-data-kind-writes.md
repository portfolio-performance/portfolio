# 21: MasterDataKind writes

Type: task
Status: ready-for-agent
Blocked by: 20

Spec: [spec.md](../spec.md), "Master data kinds". Decision: [ticket 03](03-master-data-kind.md), "Members are the read item", "Checks", "Write interface".

## What to build

Give writable members a writer, and derive the master data operations from the kinds.

- **Writer**: the write description, required, optional or flag, the setter, the "needs" phrase, and single-member checks (`invalid-currency`, `invalid-isin`, the type checks). Read and write descriptions stay separate strings.
- **Validator**: each kind's named validator holds the checks that look beyond one member: `duplicate-isin`, `currency-immutable`, and the rule that the reference cash account must be an **Existing entity**.
- **Write interface**: `kind.add(JsonObject)` and `kind.update(JsonObject, Claims)` return `Outcome`. Reading `uuid`, finding the existing entity and claiming it stay private to the update.
- **Derived**: the add and update `OperationSpec`s and their schemas. The handlers are thin adapters.
- **Removed**: `EntityOperations` (with `Definition`, `Field` and the ad-hoc `EntityKind.of(...)` in `find`), `InstrumentOperations` and `AccountOperations`. Master data code no longer calls `OperationContext.fail`, `created` or `updated`.

## Acceptance criteria

- [ ] Every write field is a read member by construction: there is no separate list of write field names
- [ ] Per kind, at the interface: the render → add → render round trip, `update(render)` → `Unchanged`, required, type, currency, ISIN and reference-cash-account failures, and claims
- [ ] `apply --help <op>` for every master data operation is byte-identical (golden help unchanged)
- [ ] CLI suite green

## Tests deleted

- The cases in `AccountOperationsTest` that the interface tests duplicate
- The instrument cases of `ApplyCommandTest`
- Keep one or two CliFixture smoke runs per master data operation.
