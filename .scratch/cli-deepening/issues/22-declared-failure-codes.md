# 22: Declared failure codes

Type: task
Status: resolved
Blocked by: 15, 19, 21

Spec: [spec.md](../spec.md), "Declared failure codes" and "Contract-change ripple". Decision: [ticket 06](06-write-failure-codes.md).

## What to build

- **Field codes join `ErrorCode`**: each code has one generic description. `FieldFailure(field, ErrorCode code, message, check)` takes the enum, so a free-string field code no longer compiles. `check` stays a free string.
- **`OperationSpec.failureCodes()`** is derived. It is the union of:
  - the codes implied by the input `Schema`: `required`, `invalid-type`, `not-positive`, `invalid-amount`, `reference-not-found`, `unknown-field`, and `duplicate-target` when the operation claims;
  - the codes declared by its owning module:
    - `TransactionRecords`, per add, update and delete: the rule table's refusal codes, the named cross-field invariants, `type-immutable`/`kind-immutable`, `record-incomplete`, `import-check-failed` and `negative-holding`;
    - `MasterDataKind`, per kind: the writers' checks and the validator's codes;
    - `taxonomy.assign`: `not-a-leaf`, `duplicate-classification` and `weight-total-exceeded`.
- **`BATCH`** declares `invalid-operation` and `unknown-operation`.
- **Help** (allowed contract addition):
  - `apply --help <op>` gains a "Failure codes:" section, one `code  description` line per code.
  - `apply --help` lists the two batch codes and one line pointing to `apply --help <op>`.
  - The global `pp-cli --help` keeps listing only codes some facet declares.
- **Skill**: SKILL.md's `validation-failed` row becomes "Explain each entry of `failures`; `pp-cli apply --help <operation>` describes its code."
- **CliRun**: on every `validation-failed` run, it asserts that each `failures[].code` is in the declared set of the operation at its `operationIndex`, or in the `BATCH` set.
- **Static check**: ticket 15's check widens to "every `BY_USE` code is declared by some facet, operation or `BATCH`".

## Acceptance criteria

- [ ] No field-level code string literal remains in `cli` outside `ErrorCode`
- [ ] `apply --help <op>` shows each operation's failure codes; golden files updated in this commit
- [ ] The global help's code list is unchanged
- [ ] The CliRun failures check runs on every existing failing apply test
- [ ] SkillDriftTest green with the updated `validation-failed` row
- [ ] CLI suite green

## Tests deleted

None. There is no reachability probe; tickets 18, 19 and 21's interface tests already exercise the codes.

## Answer

Implemented on integration branch `feat/cli-deepening`, merge commit 32947c223. Field codes in ErrorCode; derived OperationSpec.failureCodes(); Failure codes sections in apply help; CliRun failures[].code check.
