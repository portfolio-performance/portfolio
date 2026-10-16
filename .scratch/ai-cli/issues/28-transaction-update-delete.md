# 28: `transaction.update` and `transaction.delete`

Type: task
Status: ready-for-agent
Blocked by: 27

Spec: [spec.md](../spec.md), user stories 67–70 and 89.

## What to build

An agent can edit and remove whole transaction records.

- **`transaction.update`** takes either side's UUID and a patch. The patch is overlaid on the full current record and then validated exactly like an add, so a partial edit can never bypass validation. Nothing is recomputed silently. A patch that leaves fees, taxes or forex units inconsistent with a changed `shares` or `amount` is refused with a field-level error. Changing a record's `type` is refused; the agent deletes and re-adds instead.
- **`transaction.delete`** takes either side's UUID and removes the whole record, both sides.

The overselling check covers edits and deletes too: removing or shrinking an earlier buy is refused if it would make any end-of-date holding negative.

## Acceptance criteria

- [ ] Updating a field of each record kind (cash-only, buy/sell, transfer) works by either side's UUID, and both sides stay consistent
- [ ] A patch that breaks fee, tax or unit consistency is refused with a field-level error, and nothing is recomputed
- [ ] Changing `type` is refused
- [ ] Deleting by either side's UUID removes both sides
- [ ] An update or delete that would oversell at any date is refused
- [ ] Update and delete change records show before and after for the changed fields
- [ ] Both operations are declared with input schemas, appear in `describe` and pass its consistency test
