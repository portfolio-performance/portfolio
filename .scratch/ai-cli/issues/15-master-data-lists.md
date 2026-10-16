# 15: Master-data lists with pagination

Type: task
Status: ready-for-agent
Blocked by: 13

Spec: [spec.md](../spec.md), user stories 28–31 and 62–63.

## What to build

An agent can list the **master data** in a client file:

- `pp-cli instruments`: UUID, name, currency, ISIN, ticker, WKN, note, retired flag and user-defined attributes.
- `pp-cli investment-accounts`: UUID, name, reference cash account, note and retired flag.
- `pp-cli cash-accounts`: UUID, name, currency, note and retired flag.

UUIDs are canonical in all output. List commands share pagination: `--limit` (default 200) and `--offset`, returning `total` and `nextOffset`, with stable ordering. Field names are camelCase and match the upstream #5870 names where an equivalent exists. The exact contract is under "Decisions" below and is binding.

## Acceptance criteria

- [ ] All three commands return the documented fields for a fixture built with the shared JUnit builders
- [ ] `--limit` and `--offset` page through results, and `total` and `nextOffset` are correct, with `nextOffset` absent on the last page
- [ ] Ordering is stable across calls: name (case-insensitive, ordinal), then UUID
- [ ] Invalid `--limit`/`--offset` values give the `invalid-option-value` usage error; an offset past the end gives an empty page
- [ ] Attributes use the typed encoding below; unsupported, orphaned and mismatched values are skipped without failing the command
- [ ] All three commands are declared, appear in `describe` (including option types, defaults and ranges) and pass its consistency test

## Decisions

Settled with the owner in a design session (2026-10-16). Implement them as written; don't re-ask.

### Response shape (shared by every list command, including ticket 17)

```json
{
  "data": { "items": [ ... ], "total": 412, "nextOffset": 200 },
  "meta": { "fileVersion": 66, "asOf": "2026-10-16", "limit": 200, "offset": 0 },
  "warnings": []
}
```

- `data` is an object `{items, total, nextOffset}`, not a bare array. Upstream #5870 wraps lists in `{items}` too.
- `nextOffset` is **omitted** on the last page.
- `meta` echoes the resolved `limit` and `offset`, alongside `fileVersion` and `asOf` as in `info`.

### Paging input

- `--limit`: integer, 1–1000, default 200. `--offset`: integer ≥ 0, default 0.
- Anything else (0, negative, non-integer, over 1000) is a usage error: code `invalid-option-value`, exit code 2 (`CliException.usage`), with `option` and `value` extensions (`CliException.with`).
- An offset at or past the end returns empty `items`, the correct `total`, and no `nextOffset`.
- `OptionSpec` gains an optional type, default and range, so that `describe` publishes them.

### Ordering

- Master data is ordered by name, case-insensitive with ordinal comparison (e.g. `String.CASE_INSENSITIVE_ORDER`, **not** a locale `Collator`), then by UUID as the tie-break.
- The spec's "date then UUID" applies only to dated records (transactions).

### Field rules

- Unset optional fields are **omitted**, never `null` (`Schema.optionalProperty`). Upstream does the same.
- `retired` is always present as a boolean on all three entities. The core getter is `isRetired()`. The UI calls it "deactivated"; we use **Retired** (see `GLOSSARY.md`). Upstream doesn't expose it.
- `note` is included on all three entities when set.

### Instruments

- Fields: `uuid`, `name`, `currencyCode`, `isin`, `wkn`, `tickerSymbol`, `note`, `retired`, `attributes`.
- List **every** security in the client, including indices and exchange-rate securities. This matches `info`'s `counts.instruments`. Indices have no currency, so `currencyCode` is optional and omitted for them. There are no filters or flags for these kinds in this ticket.

### Investment accounts

- Fields: `uuid`, `name`, `referenceCashAccount`, `note`, `retired`.
- `referenceCashAccount` is a bare UUID string, as upstream does it, and is omitted if the portfolio has none.

### Cash accounts

- Fields: `uuid`, `name`, `currencyCode`, `note`, `retired`.

### Instrument attributes

- `attributes` is an array of `{id, name, type, value}`, a deliberate deviation from upstream's id-keyed map. User-created attribute type ids are random UUIDs and there is no `attribute-types` command, so an agent needs `name` and `type` inline.
- Iterate `client.getSettings().getAttributeTypes()` filtered by `supports(Security.class)`. This gives a stable order and skips orphaned stored entries. Include only attributes with a non-null stored value.
- Value encoding copies upstream #5870's `AttributeCodec`. It deliberately avoids core's `AttributeType.Converter.toString`, which is locale-dependent. Our pinned core has no `AttributeFieldType`, so map from `AttributeType.getConverter()`'s class:

  | Converter | `type` | `value` |
  |---|---|---|
  | `StringConverter` | `string` | string |
  | `BooleanConverter` | `boolean` | boolean |
  | `DateConverter` | `date` | ISO `yyyy-MM-dd` string |
  | `AmountConverter`, `AmountPlainConverter` | `amount` | decimal: stored long ÷ 10^precision of `Values.Amount` / `Values.AmountPlain` |
  | `QuoteConverter` | `quote` | decimal, `Values.Quote` precision |
  | `ShareConverter` | `shares` | decimal, `Values.Share` precision |
  | `PercentConverter` | `percent` | stored double as is (a fraction: 0.0022 means 0.22%) |
  | `PercentPlainConverter` | `number` | stored double as is (unscaled; not a percent despite the name) |
  | `LimitPriceConverter`, `BookmarkConverter`, `ImageConverter`, unknown | skipped | n/a |

- A value that doesn't match its declared type (e.g. a type edited after values were stored, or a non-finite double) is skipped silently. It must not fail the command.
- `type` is published as an enum in `describe`. `value` needs a multi-type schema (`string | number | boolean`), so extend `Schema` and the test-side `SchemaValidator` accordingly.

### Tests

- Test seam: the in-process `PpCli.execute`, with fixtures from the shared JUnit builders.
- Each new command needs a case in `DescribeConsistencyTest.sampleArguments` and a declaration in `Commands.ALL`.

## Open questions (settle with the owner before implementing)

Each has a recommendation; if the owner has already answered, the answers are recorded below.

- **Empty attributes**: always emit `attributes: []` (recommended), or omit when empty as upstream does?
- **Attributes on accounts**: instruments only in this ticket (recommended), although core also supports attributes on cash and investment accounts?
- **Shared `meta` schema**: move the duplicated `fileVersion`/`asOf` meta schema into one helper shared with `info` (recommended)?
- **ADR**: record the deviations from upstream (attribute array, paging fields in `data`) in `docs/adr/0004-...` (recommended)?
