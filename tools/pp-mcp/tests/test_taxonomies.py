import pytest
from fastmcp.exceptions import ToolError

from pp_mcp.tools import taxonomies as tx

from conftest import F, problem, raw_json, sent_json, sent_query

T = "/v1/files/main/taxonomies"


async def test_list_taxonomies_through_mcp(api, one_file, mcp_client):
    api.get(f"{F}/taxonomies").respond(200, json={"items": [{"id": "t1", "name": "Asset Allocation", "categories": []}]})
    result = await mcp_client.call_tool("list_taxonomies", {})
    assert result.data["items"][0]["id"] == "t1"


async def test_allocation(api):
    route = api.get(f"{T}/t1/allocation").mock(return_value=raw_json('{"items":[{"id":"c1","value":{"value":10.10,"currency":"EUR"}}]}'))
    result = await tx.get_taxonomy_allocation("t1", "main", date="2026-01-31", currency="EUR")
    assert sent_query(route) == {"date": "2026-01-31", "currency": "EUR"}
    assert result["items"][0]["value"]["value"] == "10.10"


async def test_taxonomy_crud(api):
    create = api.post(T).respond(201, json={"id": "t2"})
    await tx.create_taxonomy("Regions", "main", dry_run=True)
    assert sent_json(create) == {"name": "Regions"} and sent_query(create) == {"dry_run": "true"}
    rename = api.patch(f"{T}/t2").respond(200, json={"id": "t2"})
    await tx.rename_taxonomy("t2", "Regionen", "main")
    assert sent_json(rename) == {"name": "Regionen"}
    api.delete(f"{T}/t2").respond(204)
    assert await tx.delete_taxonomy("t2", "main") == {"deleted": True, "taxonomy_id": "t2"}


async def test_classification_crud(api):
    create = api.post(f"{T}/t1/classifications").respond(201, json={"id": "c9"})
    await tx.create_classification("t1", "Europe", "main", parent_id="c1", color="#89afee", weight="12.50", note="n")
    assert sent_json(create) == {"parent": "c1", "name": "Europe", "color": "#89afee", "weight": "12.50", "note": "n"}
    patch = api.patch(f"{T}/t1/classifications/c9").respond(200, json={"id": "c9"})
    await tx.update_classification("t1", "c9", "main", name="EU", clear=["note"])
    assert sent_json(patch) == {"name": "EU", "note": None}
    delete = api.delete(f"{T}/t1/classifications/c9").respond(204)
    await tx.delete_classification("t1", "c9", "main", cascade=True, dry_run=True)
    assert sent_query(delete) == {"cascade": "true", "dry_run": "true"}
    await tx.delete_classification("t1", "c9", "main")
    assert sent_query(delete) == {}


async def test_weight_precision():
    with pytest.raises(ToolError, match="weight: .* more than 2"):
        await tx.create_classification("t1", "x", "main", weight="33.333")


async def test_assign_and_unassign(api):
    put = api.put(f"{T}/t1/classifications/c1/assignments/a1").respond(200, json={"weight": "100"})
    await tx.assign_classification("t1", "c1", "a1", "main")
    assert sent_json(put) == {"weight": "100"}
    await tx.assign_classification("t1", "c1", "a1", "main", weight="40.5", dry_run=True)
    assert sent_json(put) == {"weight": "40.5"} and sent_query(put) == {"dry_run": "true"}
    api.delete(f"{T}/t1/classifications/c1/assignments/a1").respond(204)
    assert await tx.unassign_classification("t1", "c1", "a1", "main") == {"deleted": True, "vehicle_uuid": "a1"}


async def test_weight_exceeds_100(api, mcp_client):
    api.put(f"{T}/t1/classifications/c1/assignments/a1").mock(
        return_value=problem(
            422, "validation", "Validation failed",
            errors=[{"field": "weight", "code": "weight-exceeds-100", "message": "sum would be 140 %"}],
        )
    )
    with pytest.raises(ToolError, match=r"weight \(weight-exceeds-100\): sum would be 140 %"):
        await mcp_client.call_tool(
            "assign_classification",
            {"file": "main", "taxonomy_id": "t1", "classification_id": "c1", "vehicle_uuid": "a1", "weight": "60"},
        )
