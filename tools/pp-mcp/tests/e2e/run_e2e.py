"""Live end-to-end run of the pp-mcp tools against a running Portfolio Performance.

Not collected by pytest. Requires PP_API_URL and PP_API_TOKEN and a PP instance
whose REST API has the given file enabled.

    uv run python tests/e2e/run_e2e.py --path <file> --mode full   # synthetic file: every tool
    uv run python tests/e2e/run_e2e.py --path <file> --mode copy   # copy of a real file: reads + a few writes

The script continues after a failed step and prints a summary; exit code 1 if any
step failed. It saves the file at the end and writes the UUIDs it created to
--state (JSON) so a second run with --verify can check them after a restart.
"""

import argparse
import asyncio
import csv
import json
import os
import sys
import tempfile
import traceback
from datetime import date, timedelta
from typing import Any

from fastmcp import Client

from pp_mcp.server import mcp

RESULTS: list[tuple[str, bool, str]] = []


def find_uuid(value: Any) -> str | None:
    if isinstance(value, dict):
        if isinstance(value.get("uuid"), str):
            return value["uuid"]
        for key in ("instrument", "account", "transaction", "entity", "item"):
            found = find_uuid(value.get(key))
            if found:
                return found
    return None


class Runner:
    def __init__(self, client: Client, file: str):
        self.client = client
        self.file = file

    async def call(self, tool: str, /, **args: Any) -> Any:
        args.setdefault("file", self.file)
        if tool in ("list_files", "open_file"):
            args.pop("file")
        result = await self.client.call_tool(tool, args)
        if result.structured_content is not None:
            data = result.structured_content
            return data.get("result", data) if set(data) == {"result"} else data
        return json.loads(result.content[0].text) if result.content else None

    async def step(self, label: str, tool: str, /, check=None, **args: Any) -> Any:
        try:
            value = await self.call(tool, **args)
            if check is not None:
                outcome = check(value)
                if outcome is False or isinstance(outcome, str):
                    raise AssertionError(outcome if isinstance(outcome, str) else "check failed")
            RESULTS.append((label, True, ""))
            print(f"PASS {label}")
            return value
        except Exception as e:  # noqa: BLE001 - report and continue
            detail = str(e).splitlines()[0] if str(e) else type(e).__name__
            RESULTS.append((label, False, detail))
            print(f"FAIL {label}: {str(e)[:600]}")
            if os.environ.get("E2E_TRACE"):
                traceback.print_exc()
            return None


async def expect_error(r: "Runner", label: str, tool: str, code: str, /, **args: Any) -> None:
    try:
        value = await r.call(tool, **args)
        RESULTS.append((label, False, f"expected {code}, got {str(value)[:200]}"))
        print(f"FAIL {label}: expected {code}")
    except Exception as e:  # noqa: BLE001
        ok = code in str(e)
        RESULTS.append((label, ok, "" if ok else str(e)[:300]))
        print(f"{'PASS' if ok else 'FAIL'} {label}" + ("" if ok else f": {str(e)[:300]}"))


def expect(cond: bool, message: str):
    return True if cond else message


async def full(r: Runner, state: dict) -> None:
    today = date.today()
    d = lambda days: (today - timedelta(days=days)).isoformat()  # noqa: E731

    accounts = await r.step("list_accounts", "list_accounts")
    eur_account = accounts["cashAccounts"][0]["uuid"] if accounts else None
    depot = accounts["investmentAccounts"][0]["uuid"] if accounts else None
    await r.step("list_instruments", "list_instruments", check=lambda v: expect(len(v["items"]) >= 1, "no instruments"))
    await r.step("find_instrument", "find_instrument", query="Commerzbank",
                 check=lambda v: expect(len(v["items"]) == 1, f"found {v}"))
    await r.step("list_attribute_types", "list_attribute_types")

    # --- instruments
    await r.step("create_instrument dry_run", "create_instrument", name="E2E Test AG", currency="EUR",
                 isin="DE000E2E0001", dry_run=True)
    inst = await r.step("create_instrument", "create_instrument", name="E2E Test AG", currency="EUR",
                        isin="DE000E2E0001", ticker="E2E.DE", note="created by e2e", client_ref="e2e-inst-1")
    inst_id = find_uuid(inst)
    await r.step("create_instrument duplicate check", "create_instrument", name="E2E Test AG", currency="EUR",
                 isin="DE000E2E0001", check=lambda v: expect(find_uuid(v) == inst_id, f"not deduplicated: {v}"))
    usd_inst = find_uuid(await r.step("create_instrument USD", "create_instrument", name="E2E US Corp",
                                      currency="USD", isin="US0000E2E002"))
    await r.step("update_instrument", "update_instrument", uuid=inst_id, wkn="E2E001", note="updated by e2e")
    await r.step("set_prices", "set_prices", uuid=inst_id,
                 prices=[{"date": d(30), "value": "10.00"}, {"date": d(10), "value": "11.50"},
                         {"date": d(1), "value": "12.25"}])
    await r.step("set_prices USD", "set_prices", uuid=usd_inst,
                 prices=[{"date": d(30), "value": "50.00"}, {"date": d(1), "value": "55.00"}])
    await r.step("get_instrument with prices", "get_instrument", uuid=inst_id, include_prices=True,
                 check=lambda v: expect("12.25" in json.dumps(v, default=str), f"price missing: {v}"))
    await r.step("delete_prices range", "delete_prices", uuid=inst_id, from_date=d(10), to_date=d(10))
    await r.step("add_instrument_event", "add_instrument_event", uuid=inst_id, type="note", date=d(5),
                 details="e2e note event")
    await r.step("list_instrument_events", "list_instrument_events", uuid=inst_id)
    await r.step("delete_instrument_event", "delete_instrument_event", uuid=inst_id, type="note", date=d(5))

    # --- accounts
    usd = find_uuid(await r.step("create_cash_account USD", "create_cash_account", name="E2E USD", currency="USD"))
    await r.step("update_cash_account", "update_cash_account", uuid=usd, note="usd account")
    depot2 = find_uuid(await r.step("create_investment_account", "create_investment_account", name="E2E Depot",
                                    reference_cash_account=eur_account))
    await r.step("get_account", "get_account", uuid=depot2, kind="investment")
    tmp_acc = find_uuid(await r.step("create_cash_account temp", "create_cash_account", name="E2E Temp",
                                     currency="EUR"))
    await r.step("delete_cash_account", "delete_cash_account", uuid=tmp_acc)

    # --- transactions
    before = await r.call("list_transactions")
    count_before = before.get("total", len(before.get("items", [])))
    await r.step("create_buy dry_run", "create_buy", investment_account=depot, cash_account=eur_account,
                 instrument=inst_id, date=d(20), shares="100", quote="10.00", fees="4.90", dry_run=True)
    after_dry = await r.call("list_transactions")
    await r.step("dry_run left transactions unchanged", "list_transactions",
                 check=lambda v: expect(v.get("total", len(v.get("items", []))) == count_before
                                        and after_dry.get("total") == before.get("total"), "count changed"))
    buy = await r.step("create_buy", "create_buy", investment_account=depot, cash_account=eur_account,
                       instrument=inst_id, date=d(20), shares="100", quote="10.00", fees="4.90",
                       client_ref="e2e-buy-1", note="e2e buy")
    buy_id = find_uuid(buy)
    await r.step("create_buy replay", "create_buy", investment_account=depot, cash_account=eur_account,
                 instrument=inst_id, date=d(20), shares="100", quote="10.00", fees="4.90", client_ref="e2e-buy-1",
                 check=lambda v: expect(find_uuid(v) == buy_id and "replayed" in json.dumps(v), f"no replay: {v}"))
    await r.step("get_transaction buy", "get_transaction", uuid=buy_id,
                 check=lambda v: expect("1,004.90" in json.dumps(v) or "1004.9" in json.dumps(v, default=str),
                                        f"total wrong: {v}"))
    fx_buy = find_uuid(await r.step("create_buy FX", "create_buy", investment_account=depot, cash_account=eur_account,
                                    instrument=usd_inst, date=d(15), shares="10", quote="50.00",
                                    exchange_rate="0.9", fees="1.00", client_ref="e2e-fxbuy-1"))
    await r.step("create_sell", "create_sell", investment_account=depot, cash_account=eur_account,
                 instrument=inst_id, date=d(3), shares="20", quote="12.00", taxes="2.00", client_ref="e2e-sell-1")
    await r.step("create_dividend", "create_dividend", cash_account=eur_account, instrument=inst_id, date=d(4),
                 gross_value="25.00", taxes="6.60", shares="80", ex_date=d(6), client_ref="e2e-div-1")
    dep = find_uuid(await r.step("create_cash_transaction deposit", "create_cash_transaction",
                                 cash_account=eur_account, kind="deposit", date=d(25), amount="5000.00",
                                 client_ref="e2e-dep-1"))
    await r.step("create_cash_transaction interest", "create_cash_transaction", cash_account=eur_account,
                 kind="interest", date=d(2), amount="3.10", taxes="0.90")
    await r.step("create_cash_transaction fees", "create_cash_transaction", cash_account=eur_account,
                 kind="fees", date=d(2), amount="1.50", instrument=inst_id)
    await r.step("create_transfer FX", "create_transfer", from_cash_account=eur_account, to_cash_account=usd,
                 date=d(12), amount="900.00", target_amount="1000.00", client_ref="e2e-xfer-1")
    await r.step("create_delivery inbound", "create_delivery", investment_account=depot2, instrument=inst_id,
                 direction="inbound", date=d(18), shares="5", quote="10.50")
    await r.step("create_security_transfer", "create_security_transfer", from_investment_account=depot,
                 to_investment_account=depot2, instrument=inst_id, date=d(8), shares="10", quote="11.00")
    await expect_error(r, "create_sell with zero shares rejected", "create_sell", "must-be-positive",
                       investment_account=depot, cash_account=eur_account, instrument=inst_id, date=d(3),
                       shares="0", quote="12.00")
    await r.step("update_transaction note+date", "update_transaction", uuid=buy_id, note="e2e buy (edited)",
                 date=d(21))
    await r.step("update_transaction move account", "update_transaction", uuid=buy_id, investment_account=depot2,
                 check=lambda v: expect(find_uuid(v) == buy_id, f"uuid changed: {v}"))
    await r.step("update_transaction dry_run", "update_transaction", uuid=fx_buy, fees="2.00", dry_run=True)
    await r.step("delete_transaction dry_run", "delete_transaction", uuid=dep, dry_run=True)
    await r.step("list_transactions filtered", "list_transactions", instrument=inst_id,
                 check=lambda v: expect(len(v["items"]) >= 5, f"only {len(v['items'])}"))

    # --- watchlists, plans, taxonomies
    await r.step("create_watchlist", "create_watchlist", name="E2E Watch", instruments=[inst_id])
    await r.step("add_to_watchlist", "add_to_watchlist", name="E2E Watch", instrument=usd_inst)
    await r.step("get_watchlist", "get_watchlist", name="E2E Watch",
                 check=lambda v: expect(len(json.dumps(v).split(usd_inst)) > 1, "usd missing"))
    await r.step("update_watchlist rename", "update_watchlist", name="E2E Watch", new_name="E2E Watch 2")
    await r.step("remove_from_watchlist", "remove_from_watchlist", name="E2E Watch 2", instrument=usd_inst)
    await r.step("list_watchlists", "list_watchlists")

    await r.step("create_investment_plan", "create_investment_plan", name="E2E Plan", kind="purchase",
                 amount="100.00", instrument=inst_id, investment_account=depot, cash_account=eur_account,
                 start=d(70), interval_months=1)
    await r.step("generate_plan_transactions dry_run", "generate_plan_transactions", name="E2E Plan", dry_run=True)
    await r.step("generate_plan_transactions", "generate_plan_transactions", name="E2E Plan")
    await r.step("update_investment_plan", "update_investment_plan", name="E2E Plan", note="monthly")
    await r.step("list_investment_plans", "list_investment_plans")

    tax = find_uuid(await r.step("create_taxonomy", "create_taxonomy", name="E2E Regions"))
    tax = tax or (await r.call("list_taxonomies"))["items"][-1].get("id")
    tax_obj = await r.call("get_taxonomy", taxonomy_id=tax)
    state["taxonomy"] = tax
    europe = await r.step("create_classification", "create_classification", taxonomy_id=tax, name="Europe",
                          color="#3366cc")
    europe_id = (europe or {}).get("id") or find_uuid(europe)
    await r.step("create_classification child", "create_classification", taxonomy_id=tax, name="Germany",
                 parent_id=europe_id)
    await r.step("assign_classification", "assign_classification", taxonomy_id=tax, classification_id=europe_id,
                 vehicle_uuid=inst_id, weight="100")
    await expect_error(r, "assign_classification over 100% rejected", "assign_classification", "weight",
                       taxonomy_id=tax, classification_id=europe_id, vehicle_uuid=inst_id, weight="150")
    await r.step("get_taxonomy_allocation", "get_taxonomy_allocation", taxonomy_id=tax)
    await r.step("update_classification", "update_classification", taxonomy_id=tax, classification_id=europe_id,
                 note="EU and friends")
    _ = tax_obj

    # --- actions
    await r.step("apply_stock_split dry_run", "apply_stock_split", uuid=inst_id, ex_date=d(7), new_shares="2",
                 old_shares="1", dry_run=True)
    await r.step("apply_stock_split", "apply_stock_split", uuid=inst_id, ex_date=d(7), new_shares="2",
                 old_shares="1", client_ref="e2e-split-1",
                 check=lambda v: expect(v.get("transactionsAffected", 0) > 0, f"nothing adjusted: {v}"))
    await r.step("update_quotes dry_run", "update_quotes", dry_run=True)
    await r.step("update_quotes (manual instrument)", "update_quotes", instruments=[inst_id], targets=["latest"],
                 wait_seconds=30, check=lambda v: expect(str(v.get("status", "")).lower() in ("done", "failed"),
                                                        f"not finished: {v}"))

    with tempfile.NamedTemporaryFile("w", suffix=".csv", delete=False, newline="", encoding="utf-8") as fh:
        w = csv.writer(fh, delimiter=";")
        w.writerow(["Date", "Value", "Note"])
        w.writerow([d(9), "123.45", "e2e csv deposit"])
        csv_path = fh.name
    config = {"target": "account-transaction", "delimiter": ";", "skipLines": 0, "isFirstLineHeader": True,
              "columns": [{"label": "Date", "field": "date", "format": "yyyy-MM-dd"},
                          {"label": "Value", "field": "value", "format": "0,000.00"},
                          {"label": "Note", "field": "note"}]}
    preview = await r.step("import_csv preview", "import_csv", path=csv_path, config=config, cash_account=eur_account)
    if preview:
        await r.step("commit_import", "commit_import", import_id=preview["importId"], cash_account=eur_account,
                     check=lambda v: expect(v.get("count") == 1, f"count {v.get('count')}"))
    await expect_error(r, "import_pdf missing file rejected", "import_pdf", "file-not-found",
                       paths=[os.path.join(tempfile.gettempdir(), "nope.pdf")])
    os.unlink(csv_path)

    # --- reports
    await report_steps(r)

    await r.step("delete_transaction", "delete_transaction", uuid=dep)
    await r.step("delete_watchlist", "delete_watchlist", name="E2E Watch 2")

    state["transactions"] = [u for u in (buy_id, fx_buy) if u]
    state["instrument"] = inst_id
    state["sources"] = ["api:e2e-buy-1", "api:e2e-fxbuy-1"]


async def report_steps(r: Runner) -> None:
    year_ago = (date.today() - timedelta(days=365)).isoformat()
    await r.step("get_holdings", "get_holdings")
    await r.step("get_performance", "get_performance", from_date=year_ago, include_series=True,
                 include_calendar=True)
    await r.step("get_security_performance", "get_security_performance", from_date=year_ago)
    await r.step("get_trades", "get_trades")
    await r.step("get_earnings", "get_earnings", from_date=year_ago)
    await r.step("list_taxonomies", "list_taxonomies")


async def copy_mode(r: Runner, state: dict) -> None:
    """Reads everything, then a few reversible-looking writes on the copy."""
    accounts = await r.step("list_accounts", "list_accounts")
    await r.step("list_instruments", "list_instruments")
    await r.step("list_transactions", "list_transactions", limit=20)
    await report_steps(r)
    taxonomies = await r.call("list_taxonomies")
    for t in (taxonomies or {}).get("items", [])[:3]:
        await r.step(f"get_taxonomy_allocation {t.get('name')}", "get_taxonomy_allocation", taxonomy_id=t["id"])
    await r.step("list_watchlists", "list_watchlists")
    await r.step("list_investment_plans", "list_investment_plans")

    cash = next((a for a in accounts["cashAccounts"] if not a.get("retired")), None) if accounts else None
    if cash is None:
        return
    inst = find_uuid(await r.step("create_instrument", "create_instrument", name="E2E Copy Test",
                                  currency=cash["currencyCode"], isin="XX000E2E0099"))
    dep = await r.step("create deposit", "create_cash_transaction", cash_account=cash["uuid"], kind="deposit",
                       date=date.today().isoformat(), amount="1.23", note="e2e on copy", client_ref="e2e-copy-dep")
    dep_id = find_uuid(dep)
    await r.step("update deposit", "update_transaction", uuid=dep_id, note="e2e on copy (edited)")
    await r.step("get_holdings after write", "get_holdings")
    state["transactions"] = [dep_id]
    state["instrument"] = inst
    state["sources"] = ["api:e2e-copy-dep"]


async def verify(r: Runner, state: dict) -> None:
    for uuid in state.get("transactions", []):
        await r.step(f"after restart: get_transaction {uuid[:8]}", "get_transaction", uuid=uuid)
    if state.get("instrument"):
        await r.step("after restart: get_instrument", "get_instrument", uuid=state["instrument"])
    # not asserted: files with online quote feeds become dirty right after opening
    info = await r.step("after restart: get_file", "get_file")
    if info:
        print(f"  dirty after open: {info['dirty']}")


async def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--path", required=True, help="absolute path of the portfolio file (enabled for the API)")
    parser.add_argument("--mode", choices=["full", "copy"], default="full")
    parser.add_argument("--state", default=None, help="JSON file to write/read created UUIDs")
    parser.add_argument("--verify", action="store_true", help="only check UUIDs from --state after a restart")
    args = parser.parse_args()

    async with Client(mcp) as client:
        r = Runner(client, file=None)  # type: ignore[arg-type]
        opened = await r.step("open_file", "open_file", path=args.path)
        if not opened:
            return 1
        r.file = opened.get("id") or opened.get("alias")
        await r.step("list_files", "list_files")

        if args.verify:
            state = json.load(open(args.state, encoding="utf-8"))
            await verify(r, state)
        else:
            state: dict = {}
            await (full(r, state) if args.mode == "full" else copy_mode(r, state))
            await r.step("get_file dirty", "get_file", check=lambda v: expect(v["dirty"] is True, "not dirty"))
            saved = await r.step("save_file", "save_file", check=lambda v: expect("savedAt" in v, f"not saved: {v}"))
            if saved and saved.get("dirty"):
                # a background price update (started when the file was opened) changed the
                # model during the save; save again once it has settled
                await asyncio.sleep(15)
                await r.step("save_file again", "save_file",
                             check=lambda v: expect(v["dirty"] is False, f"still dirty: {v}"))
            if args.state:
                json.dump(state, open(args.state, "w", encoding="utf-8"), indent=2)

    failed = [x for x in RESULTS if not x[1]]
    print(f"\n{len(RESULTS) - len(failed)}/{len(RESULTS)} steps passed")
    for label, _, detail in failed:
        print(f"  FAILED {label}: {detail}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(asyncio.run(main()))
