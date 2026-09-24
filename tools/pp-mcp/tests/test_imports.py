import os

import pytest
from fastmcp.exceptions import ToolError

from pp_mcp.tools import imports
from pp_mcp.tools.imports import importable

from conftest import F, problem, sent_json, sent_query

M = "/v1/files/main"
PDF = os.path.abspath("statement.pdf")
CASH = {"uuid": "c1", "name": "Broker Cash"}
DEPOT = {"uuid": "p1", "name": "Broker"}
# the shape of POST /imports/pdf (ImportPreview in openapi.yaml)
PREVIEW = {
    "importId": "imp1",
    "kind": "pdf",
    "expiresAt": "2026-09-24T10:15:00Z",
    "items": [
        {
            "index": 0,
            "kind": "buy-sell",
            "status": "ok",
            "checks": [{"code": "duplicates", "status": "ok"}],
            "date": "2026-03-02",
            "transaction": {"type": "buy", "shares": 12.5},
            "investmentAccount": DEPOT,
            "cashAccount": CASH,
        },
        {
            "index": 1,
            "kind": "transaction",
            "status": "warning",
            "checks": [{"code": "duplicates", "status": "warning", "message": "probable duplicate"}],
            "transaction": {"type": "dividends"},
            "cashAccount": CASH,
        },
        {
            "index": 2,
            "kind": "skipped",
            "status": "error",
            "checks": [{"code": "skipped", "status": "error", "message": "not supported"}],
        },
        {"index": 3, "kind": "instrument", "status": "ok", "checks": [], "instrument": {"name": "Apple", "new": True}},
    ],
    "counts": {"ok": 2, "warning": 1, "error": 1},
    "errors": [{"path": "/tmp/other.pdf", "message": "no importer recognized the document"}],
}
COMMITTED = {
    "importId": "imp1",
    "count": 2,
    "items": [{"index": 3, "kind": "instrument"}, {"index": 0, "kind": "buy-sell"}],
    "transactions": [{"uuid": "t1", "type": "buy"}],
    "instruments": [{"uuid": "a1", "name": "Apple"}],
}


def test_importable():
    assert [importable(i) for i in PREVIEW["items"]] == [True, False, False, True]
    assert [importable(i, include_warnings=True) for i in PREVIEW["items"]] == [True, True, False, True]
    assert not importable({})


async def test_import_pdf_preview_is_default_through_mcp(api, one_file, mcp_client):
    extract = api.post(f"{F}/imports/pdf").respond(200, json=PREVIEW)
    result = await mcp_client.call_tool("import_pdf", {"paths": ["statement.pdf"], "cash_account": "c1"})
    # the preview runs its checks with the targets, too
    assert sent_json(extract) == {"paths": [PDF], "targets": {"cashAccount": "c1"}}
    assert sent_query(extract) == {}
    assert result.data["importId"] == "imp1"
    assert result.data["counts"] == {"ok": 2, "warning": 1, "error": 1}
    assert "commit_import(import_id='imp1'" in result.data["note"]


async def test_import_pdf_preview_without_targets(api):
    extract = api.post(f"{M}/imports/pdf").respond(200, json=PREVIEW)
    await imports.import_pdf([PDF], "main")
    assert sent_json(extract) == {"paths": [PDF]}


async def test_import_pdf_commit_selects_ok_items_only(api):
    extract = api.post(f"{M}/imports/pdf").respond(200, json=PREVIEW)
    commit = api.post(f"{M}/imports/imp1/commit").respond(200, json=COMMITTED)
    result = await imports.import_pdf(
        [PDF], "main", cash_account="c1", investment_account="p1", accounts_by_currency={"USD": "c2"},
        secondary_cash_account="c3", convert_buy_sell_to_delivery=False, import_notes=True, dry_run=False,
    )
    targets = {
        "cashAccount": "c1",
        "cashAccountsByCurrency": {"USD": "c2"},
        "investmentAccount": "p1",
        "secondaryCashAccount": "c3",
    }
    assert sent_json(extract) == {"paths": [PDF], "targets": targets}
    assert sent_json(commit) == {
        "select": [0, 3],
        "targets": targets,
        "options": {"convertBuySellToDelivery": False, "importNotes": True},
    }
    assert sent_query(commit) == {}
    assert result["commit"]["count"] == 2
    assert result["commit"]["instruments"] == [{"uuid": "a1", "name": "Apple"}]
    assert result["preview"]["importId"] == "imp1"


async def test_import_pdf_commit_include_warnings(api):
    api.post(f"{M}/imports/pdf").respond(200, json=PREVIEW)
    commit = api.post(f"{M}/imports/imp1/commit").respond(200, json=COMMITTED)
    await imports.import_pdf([PDF], "main", include_warnings=True, dry_run=False)
    assert sent_json(commit) == {"select": [0, 1, 3]}


async def test_import_pdf_commit_with_nothing_importable(api):
    api.post(f"{M}/imports/pdf").respond(
        200, json={"importId": "imp2", "items": [{"index": 0, "status": "warning"}, {"index": 1, "status": "error"}]}
    )
    result = await imports.import_pdf([PDF], "main", dry_run=False)
    assert "nothing was imported" in result["note"]


async def test_import_pdf_needs_paths():
    with pytest.raises(ToolError, match="paths"):
        await imports.import_pdf([], "main")


async def test_import_csv_preview(api):
    extract = api.post(f"{M}/imports/csv").respond(200, json={**PREVIEW, "kind": "csv", "errors": []})
    config = {"target": "account-transaction", "delimiter": ";", "columns": [{"field": "date", "format": "dd.MM.yyyy"}]}
    await imports.import_csv("tx.csv", config, "main")
    assert sent_json(extract) == {"path": os.path.abspath("tx.csv"), "config": config}


async def test_import_csv_direct_commit_with_instrument(api):
    extract = api.post(f"{M}/imports/csv").respond(
        200, json={"importId": "imp4", "kind": "csv", "items": [{"index": 0, "kind": "price", "status": "ok"}]}
    )
    commit = api.post(f"{M}/imports/imp4/commit").respond(200, json={"importId": "imp4", "count": 1})
    await imports.import_csv(
        "prices.csv", {"target": "investment-vehicle-price"}, "main", instrument="a1", dry_run=False
    )
    assert sent_json(extract)["targets"] == {"instrument": "a1"}
    assert sent_json(commit) == {"select": [0], "targets": {"instrument": "a1"}}


async def test_import_csv_parse_error(api):
    api.post(f"{M}/imports/csv").mock(
        return_value=problem(422, "validation", "Validation failed",
                             errors=[{"field": "line[7]", "code": "csv-parse-error", "message": "bad date"}])
    )
    with pytest.raises(ToolError, match=r"^validation: Validation failed\nline\[7\] \(csv-parse-error\): bad date"):
        await imports.import_csv("tx.csv", {}, "main")


async def test_import_csv_rejects_float_config():
    with pytest.raises(ToolError, match="config.skipLines"):
        await imports.import_csv("tx.csv", {"skipLines": 1.5}, "main")


async def test_commit_import_explicit_select_and_dry_run_through_mcp(api, mcp_client):
    commit = api.post(f"{M}/imports/imp1/commit").respond(
        200, json={"dryRun": True, "importId": "imp1", "count": 1, "items": [PREVIEW["items"][1]]}
    )
    result = await mcp_client.call_tool(
        "commit_import",
        {"file": "main", "import_id": "imp1", "select": [1], "cash_account": "c1", "remove_dividends": True,
         "dry_run": True},
    )
    assert sent_json(commit) == {"select": [1], "targets": {"cashAccount": "c1"}, "options": {"removeDividends": True}}
    assert sent_query(commit) == {"dry_run": "true"}
    assert result.data["count"] == 1


async def test_commit_import_without_select(api):
    commit = api.post(f"{M}/imports/imp1/commit").respond(200, json=COMMITTED)
    await imports.commit_import("imp1", "main")
    assert commit.calls.last.request.content == b"{}"


async def test_commit_import_error_item(api):
    api.post(f"{M}/imports/imp1/commit").mock(
        return_value=problem(422, "validation", "Validation failed",
                             errors=[{"field": "select[0]", "code": "item-not-importable",
                                      "message": "item 2 cannot be imported (skipped: not supported)"}])
    )
    with pytest.raises(ToolError, match=r"select\[0\] \(item-not-importable\)"):
        await imports.commit_import("imp1", "main", select=[2])
