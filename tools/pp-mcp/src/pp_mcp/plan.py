"""Validate (dry run) or apply a booking plan through the pp-mcp tools.

A plan is a JSON file that lists tool calls, so that a batch of changes can be reviewed, dry-run
and then applied in one go:

    {"file": "<file id or alias>",
     "steps": [{"id": "...", "tool": "<pp-mcp tool>", "args": {...}, "why": "<source of the booking>"}]}

String argument values may use placeholders, resolved when the plan runs:

    "@acct:<account name>"   account UUID. If a cash and an investment account share the name, the
                             parameter decides: names containing "investment" take the investment
                             account, names containing "cash" the cash account; for "uuid" the tool
                             name decides (update_cash_account, update_investment_account, ...)
    "@cash:<name>"           cash account UUID
    "@inv:<name>"            investment account UUID
    "@isin:<ISIN>"           instrument UUID by ISIN
    "@inst:<name>"           instrument UUID by name

Usage (PP_API_URL and PP_API_TOKEN as for the MCP server):

    pp-apply-plan plan.json                # dry run of every step, nothing changes
    pp-apply-plan plan.json --commit       # apply; does not save, call save_file afterwards
    pp-apply-plan plan.json --file other   # override the plan's file

A dry run changes nothing: every write tool is called with dry_run=true, overriding a step's own
dry_run, and tools that cannot dry-run (save_file, open_file) are skipped and reported as SKIP. With
--commit, every write tool is called with dry_run=false (import_pdf/import_csv then import directly).

In a dry run, steps that reference an account or instrument created by an earlier step of the same
plan cannot be validated and are reported as DEPENDS. The exit code is 1 if any step failed.
"""

import argparse
import asyncio
import json
import re
import sys
from typing import Any

from fastmcp import Client

from pp_mcp.server import mcp

CREATES = {"create_cash_account", "create_investment_account", "create_instrument"}
PLACEHOLDER = re.compile(r"^@([a-z]+):(.+)$")
WRITE_PREFIXES = ("create_", "update_", "delete_", "set_", "add_", "remove_", "assign_", "unassign_", "apply_",
                  "generate_", "rename_", "commit_", "import_")
# write tools without a dry run; a dry run skips them
NO_DRY_RUN = {"save_file", "open_file"}
# tools that do not address a file and take no `file` argument
NO_FILE = {"open_file", "list_files"}


class Missing(Exception):
    """A placeholder names an entity that does not exist (yet)."""


class Lookups:
    def __init__(self, cash: dict[str, str], investment: dict[str, str], by_isin: dict[str, str],
                 by_name: dict[str, str]):
        self.cash = cash
        self.investment = investment
        self.by_isin = by_isin
        self.by_name = by_name


def _data(result: Any) -> Any:
    d = result.structured_content
    return d["result"] if d is not None and set(d) == {"result"} else d


async def load_lookups(client: Client, file: str) -> Lookups:
    accounts = _data(await client.call_tool("list_accounts", {"file": file}))
    instruments = _data(await client.call_tool("list_instruments", {"file": file}))["items"]
    return Lookups(
        cash={a["name"]: a["uuid"] for a in accounts["cashAccounts"]},
        investment={a["name"]: a["uuid"] for a in accounts["investmentAccounts"]},
        by_isin={i["isin"]: i["uuid"] for i in instruments if i.get("isin")},
        by_name={i["name"]: i["uuid"] for i in instruments},
    )


def resolve(value: Any, lookups: Lookups, key: str = "", tool: str = "") -> Any:
    """Replaces placeholders in a step's arguments. `key` is the parameter name, `tool` the tool."""
    if isinstance(value, list):
        return [resolve(v, lookups, key, tool) for v in value]
    if isinstance(value, dict):
        return {k: resolve(v, lookups, k, tool) for k, v in value.items()}
    match = PLACEHOLDER.match(value) if isinstance(value, str) else None
    if match is None:
        return value

    kind, name = match.groups()
    if kind == "isin":
        table = lookups.by_isin
    elif kind == "inst":
        table = lookups.by_name
    elif kind == "cash":
        table = lookups.cash
    elif kind == "inv":
        table = lookups.investment
    elif kind == "acct":
        in_cash, in_investment = name in lookups.cash, name in lookups.investment
        if in_cash and in_investment:
            hint = tool if key == "uuid" else key
            if "investment" in hint:
                table = lookups.investment
            elif "cash" in hint:
                table = lookups.cash
            else:
                raise ValueError(f"{value} is both a cash and an investment account; use @cash: or @inv:")
        else:
            table = lookups.cash if in_cash else lookups.investment
    else:
        raise ValueError(f"unknown placeholder {value}")

    if name not in table:
        raise Missing(value)
    return table[name]


def prepare_args(tool: str, args: dict[str, Any], file: str, commit: bool) -> dict[str, Any] | None:
    """The arguments a step's tool is called with, or None if a dry run must skip the step."""
    if not commit and tool in NO_DRY_RUN:
        return None
    if tool not in NO_FILE:
        args = {"file": file, **args}
    if tool.startswith(WRITE_PREFIXES):
        args["dry_run"] = not commit
    return args


def _brief(result: Any) -> str:
    if not isinstance(result, dict):
        return str(result)[:200]
    t = result.get("transaction") or result.get("account") or result.get("instrument") or result
    if isinstance(t, dict) and "type" in t and "date" in t:
        value = t.get("value") or {}
        linked = ((t.get("linked") or {}).get("owner") or {}).get("name")
        return (f"{str(t.get('date'))[:16]} {t.get('type')} {(t.get('security') or {}).get('name', '')} "
                f"sh={t.get('shares')} {value.get('value')} {value.get('currency')} "
                f"@ {(t.get('owner') or {}).get('name')}" + (f" <-> {linked}" if linked else ""))
    return json.dumps(t, default=str, ensure_ascii=False)[:200]


async def run(plan: dict, file: str, commit: bool) -> int:
    created: set[str] = set()
    ok = depends = skipped = errors = 0
    async with Client(mcp) as client:
        lookups = await load_lookups(client, file)
        for step in plan["steps"]:
            tool, sid = step["tool"], step.get("id", "?")
            try:
                args = resolve(step.get("args", {}), lookups, "", tool)
            except Missing as missing:
                if not commit and any(str(missing).endswith(":" + name) for name in created):
                    depends += 1
                    print(f"DEPENDS {sid} {tool}: {missing}")
                else:
                    errors += 1
                    print(f"ERR {sid} {tool}: unresolved {missing}")
                continue
            except ValueError as invalid:
                errors += 1
                print(f"ERR {sid} {tool}: {invalid}")
                continue

            args = prepare_args(tool, args, file, commit)
            if args is None:
                skipped += 1
                print(f"SKIP {sid} {tool}: not run in a dry run")
                continue
            try:
                result = _data(await client.call_tool(tool, args))
            except Exception as e:  # noqa: BLE001 - report every failing step and continue
                errors += 1
                print(f"ERR {sid} {tool}: {str(e)[:600]}")
                continue

            ok += 1
            print(f"OK {sid} {tool}: {_brief(result)}")
            if tool in CREATES:
                created.add(step.get("args", {}).get("name"))
                if commit:
                    lookups = await load_lookups(client, file)

    print(f"\n{ok} ok, {depends} depend on new entities, {skipped} skipped, "
          f"{errors} errors ({'COMMIT' if commit else 'dry run'})")
    return 1 if errors else 0


def main() -> None:
    parser = argparse.ArgumentParser(prog="pp-apply-plan", description="Dry-run or apply a booking plan.")
    parser.add_argument("plan", help="plan JSON file")
    parser.add_argument("--commit", action="store_true", help="apply the steps (default: dry run)")
    parser.add_argument("--file", help="file id or alias; overrides the plan's file")
    args = parser.parse_args()

    with open(args.plan, encoding="utf-8") as fh:
        plan = json.load(fh)
    file = args.file or plan["file"]
    sys.exit(asyncio.run(run(plan, file, args.commit)))


if __name__ == "__main__":
    main()
