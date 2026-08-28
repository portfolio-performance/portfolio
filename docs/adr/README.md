# Architecture decision records

Write an ADR only for a decision whose *reason* cannot be recovered from the code later — where
someone would stand in front of the result and ask "why was this chosen?" and find no answer in the
source, the tests or the API specification.

In practice that is three cases:

- **An absence.** Nothing that isn't there can be read off the code, so a field, endpoint or
  abstraction deliberately left out needs a record; otherwise the next person "fixes" it.
- **A deliberate inconsistency.** Where two places do the same thing differently on purpose, the
  code shows only that they differ.
- **A road not taken** whose alternative is plausible enough that someone will propose it again.

What the system *does* is not an ADR. Rules a client must obey belong in the README or
`openapi.yaml`; facts a maintainer needs at a specific line belong in a comment or javadoc. An ADR
that restates the contract is a copy that will rot.

Don't write one in anticipation. Record the rule where it applies, and promote it to an ADR when a
second case has to obey it and you catch yourself re-arguing it from scratch — that is the moment
the reasoning is worth preserving, and it keeps the ADR count tied to architectural decisions rather
than to features.

One screen is the norm. Keep the argument, cut the exposition.

| | |
|---|---|
| [0001](0001-feature-patch-for-patched-swt.md) | Ship patched SWT via a feature patch in the product build |
| [0002](0002-local-rest-api-for-scripts-and-agents.md) | Expose a local REST API for scripts and agents |
| [0003](0003-sign-monetary-fields-only-inside-reconciling-breakdowns.md) | Sign monetary fields only inside reconciling breakdowns |
| [0004](0004-derived-resources-have-no-synthetic-identity.md) | Derived resources have no synthetic identity |
