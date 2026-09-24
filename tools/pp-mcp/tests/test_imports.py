import os

import pytest
from fastmcp.exceptions import ToolError

from pp_mcp.tools import imports
from pp_mcp.tools.imports import importable

from conftest import F, problem, sent_json, sent_query

M = "/v1/files/main"
PDF = os.path.abspath("statement.pdf")
PREVIEW = {
    "importId": "imp1",
    "items": [
        {"index": 0, "kind": "buy", "status": "ok"},
        {"index": 1, "kind": "dividends", "status": "warning", "checks": [{"code": "duplicate", "status": "warning"}]},
        {"index": 2, "kind": "sell", "status": "error"},
        {"index": 3, "kind": "fees", "status": "ok", "checks": [{"code": "currency", "status": "error"}]},
    ],
}


def test_importable():
    assert [importable(i) for i in PREVIEW["items"]] == [True, True, False, False]
    assert not importable({"status": {"code": "ERROR"}})
    assert importable({})


async def test_import_pdf_preview_is_default_through_mcp(api, one_file, mcp_client):
    extract = api.post(f"{F}/imports/pdf").respond(200, json=PREVIEW)
    result = await mcp_client.call_tool("import_pdf", {"paths": ["statement.pdf"], "cash_account": "c1"})
    assert sent_json(extract) == {"paths": [PDF]}
    assert sent_query(extract) == {}
    assert result.data["importId"] == "imp1"
    assert "commit_import(import_id='imp1'" in result.data["note"]


async def test_import_pdf_commit_selects_non_error_items(api):
    api.post(f"{M}/imports/pdf").respond(200, json=PREVIEW)
    commit = api.post(f"{M}/imports/imp1/commit").respond(200, json={"imported": 2, "created": ["t1", "t2"]})
    result = await imports.import_pdf(
        [PDF], "main", cash_account="c1", investment_account="p1", accounts_by_currency={"USD": "c2"},
        secondary_cash_account="c3", convert_buy_sell_to_delivery=False, import_notes=True, dry_run=False,
    )
    assert sent_json(commit) == {
        "select": [0, 1],
        "targets": {
            "cashAccount": "c1",
            "cashAccountsByCurrency": {"USD": "c2"},
            "investmentAccount": "p1",
            "secondaryCashAccount": "c3",
        },
        "options": {"convertBuySellToDelivery": False, "importNotes": True},
    }
    assert sent_query(commit) == {}
    assert result["commit"]["imported"] == 2
    assert result["preview"]["importId"] == "imp1"


async def test_import_pdf_commit_with_nothing_importable(api):
    api.post(f"{M}/imports/pdf").respond(200, json={"importId": "imp2", "items": [{"index": 0, "status": "error"}]})
    result = await imports.import_pdf([PDF], "main", dry_run=False)
    assert "nothing was imported" in result["note"]


async def test_import_pdf_needs_paths():
    with pytest.raises(ToolError, match="paths"):
        await imports.import_pdf([], "main")


async def test_import_csv_preview(api):
    extract = api.post(f"{M}/imports/csv").respond(200, json={"importId": "imp3", "items": []})
    config = {"extractor": "account-transaction", "delimiter": ";", "columns": [{"field": "date", "format": "dd.MM.yyyy"}]}
    await imports.import_csv("tx.csv", config, "main")
    assert sent_json(extract) == {"path": os.path.abspath("tx.csv"), "config": config}


async def test_import_csv_direct_commit_with_instrument(api):
    api.post(f"{M}/imports/csv").respond(200, json={"importId": "imp4", "items": [{"index": 0, "status": "ok"}]})
    commit = api.post(f"{M}/imports/imp4/commit").respond(200, json={"imported": 1})
    await imports.import_csv(
        "prices.csv", {"extractor": "investment-vehicle-price"}, "main", instrument="a1", dry_run=False
    )
    assert sent_json(commit) == {"select": [0], "targets": {"instrument": "a1"}, "options": {}}


async def test_import_csv_parse_error(api):
    api.post(f"{M}/imports/csv").mock(
        return_value=problem(422, "csv-parse-error", "CSV could not be parsed",
                             errors=[{"field": "line 7", "code": "invalid-value", "message": "bad date"}])
    )
    with pytest.raises(ToolError, match=r"^csv-parse-error: CSV could not be parsed\nline 7 \(invalid-value\): bad date"):
        await imports.import_csv("tx.csv", {}, "main")


async def test_import_csv_rejects_float_config():
    with pytest.raises(ToolError, match="config.skipLines"):
        await imports.import_csv("tx.csv", {"skipLines": 1.5}, "main")


async def test_commit_import_explicit_select_and_dry_run_through_mcp(api, mcp_client):
    commit = api.post(f"{M}/imports/imp1/commit").respond(200, json={"dryRun": True, "imported": 1})
    result = await mcp_client.call_tool(
        "commit_import",
        {"file": "main", "import_id": "imp1", "select": [1], "cash_account": "c1", "remove_dividends": True,
         "dry_run": True},
    )
    assert sent_json(commit) == {"select": [1], "targets": {"cashAccount": "c1"}, "options": {"removeDividends": True}}
    assert sent_query(commit) == {"dry_run": "true"}
    assert result.data["imported"] == 1


async def test_commit_import_without_select(api):
    commit = api.post(f"{M}/imports/imp1/commit").respond(200, json={"imported": 3})
    await imports.commit_import("imp1", "main")
    assert sent_json(commit) == {"targets": {}, "options": {}}


async def test_commit_import_error_item(api):
    api.post(f"{M}/imports/imp1/commit").mock(
        return_value=problem(422, "validation", "Validation failed",
                             errors=[{"field": "select[0]", "code": "item-not-importable", "message": "item 2 has errors"}])
    )
    with pytest.raises(ToolError, match=r"select\[0\] \(item-not-importable\)"):
        await imports.commit_import("imp1", "main", select=[2])
