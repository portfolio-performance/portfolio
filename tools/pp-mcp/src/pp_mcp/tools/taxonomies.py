"""Taxonomies, classifications and assignments."""

from typing import Annotated, Any

from fastmcp.exceptions import ToolError
from pydantic import Field

from pp_mcp.app import DESTRUCTIVE, READ_ONLY, WRITE, mcp
from pp_mcp.money import WEIGHT, date_str, decimal_str
from pp_mcp.tools._common import Clear, DryRun, FileParam, api, apply_clear, compact, deleted, dry, fpath, out

TaxonomyId = Annotated[str, Field(description="Taxonomy id (see list_taxonomies)")]
ClassificationId = Annotated[str, Field(description="Classification id")]
Weight = Annotated[str | None, Field(description="Weight in percent (0-100), decimal string, max 2 decimals")]
Color = Annotated[str | None, Field(description="Display color as #rrggbb")]


@mcp.tool(annotations=READ_ONLY)
async def list_taxonomies(file: FileParam = None) -> dict[str, Any]:
    """List the taxonomies with their classification trees, ids, weights and
    assignments (instrument/account UUID and weight in percent)."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.get(fpath(f, "taxonomies")))


@mcp.tool(annotations=READ_ONLY)
async def get_taxonomy_allocation(
    taxonomy_id: TaxonomyId,
    file: FileParam = None,
    date: Annotated[str | None, Field(description="Valuation date YYYY-MM-DD (default today)")] = None,
    currency: Annotated[str | None, Field(description="Reporting currency (default: base currency)")] = None,
) -> dict[str, Any]:
    """Value per classification of a taxonomy at a date, in the reporting currency
    (decimal strings, 2 decimals)."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        params = {"date": date_str(date, "date"), "currency": currency}
        return out(await pp.get(fpath(f, "taxonomies", taxonomy_id, "allocation"), params=params))


@mcp.tool(annotations=WRITE)
async def create_taxonomy(name: str, file: FileParam = None, dry_run: DryRun = False) -> dict[str, Any]:
    """Create an empty taxonomy. The change is in memory only until save_file."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.post(fpath(f, "taxonomies"), params=dry(dry_run), body={"name": name}))


@mcp.tool(annotations=WRITE)
async def rename_taxonomy(
    taxonomy_id: TaxonomyId, name: str, file: FileParam = None, dry_run: DryRun = False
) -> dict[str, Any]:
    """Rename a taxonomy. The change is in memory only until save_file."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.patch(fpath(f, "taxonomies", taxonomy_id), params=dry(dry_run), body={"name": name}))


@mcp.tool(annotations=DESTRUCTIVE)
async def delete_taxonomy(taxonomy_id: TaxonomyId, file: FileParam = None, dry_run: DryRun = False) -> dict[str, Any]:
    """Delete a taxonomy with all classifications and assignments.
    The change is in memory only until save_file."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        result = await pp.delete(fpath(f, "taxonomies", taxonomy_id), params=dry(dry_run))
        return deleted(result, taxonomy_id=taxonomy_id)


@mcp.tool(annotations=WRITE)
async def create_classification(
    taxonomy_id: TaxonomyId,
    name: str,
    file: FileParam = None,
    parent_id: Annotated[str | None, Field(description="Parent classification id (default: the root)")] = None,
    color: Color = None,
    weight: Weight = None,
    note: str | None = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Create a classification (category) in a taxonomy.
    The change is in memory only until save_file."""
    body = compact(
        parent=parent_id, name=name, color=color, weight=decimal_str(weight, WEIGHT, "weight"), note=note
    )
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(
            await pp.post(fpath(f, "taxonomies", taxonomy_id, "classifications"), params=dry(dry_run), body=body)
        )


@mcp.tool(annotations=WRITE)
async def update_classification(
    taxonomy_id: TaxonomyId,
    classification_id: ClassificationId,
    file: FileParam = None,
    name: str | None = None,
    parent_id: Annotated[str | None, Field(description="Move under this parent classification")] = None,
    color: Color = None,
    weight: Weight = None,
    note: str | None = None,
    clear: Clear = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Update a classification (merge patch; `clear` accepts `note`).
    The change is in memory only until save_file."""
    patch = compact(
        name=name, parent=parent_id, color=color, weight=decimal_str(weight, WEIGHT, "weight"), note=note
    )
    apply_clear(patch, clear, {"note": "note"})
    if not patch:
        raise ToolError("nothing to update: pass at least one field")
    async with api() as pp:
        f = await pp.resolve_file(file)
        path = fpath(f, "taxonomies", taxonomy_id, "classifications", classification_id)
        return out(await pp.patch(path, params=dry(dry_run), body=patch))


@mcp.tool(annotations=DESTRUCTIVE)
async def delete_classification(
    taxonomy_id: TaxonomyId,
    classification_id: ClassificationId,
    file: FileParam = None,
    cascade: Annotated[bool, Field(description="Also delete child classifications and assignments")] = False,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Delete a classification. One with children or assignments needs `cascade=true`.
    The change is in memory only until save_file."""
    params = dry(dry_run, cascade="true" if cascade else None)
    async with api() as pp:
        f = await pp.resolve_file(file)
        path = fpath(f, "taxonomies", taxonomy_id, "classifications", classification_id)
        return deleted(await pp.delete(path, params=params), classification_id=classification_id)


@mcp.tool(annotations=WRITE)
async def assign_classification(
    taxonomy_id: TaxonomyId,
    classification_id: ClassificationId,
    vehicle_uuid: Annotated[str, Field(description="UUID of the instrument or cash account to assign")],
    file: FileParam = None,
    weight: Annotated[str, Field(description="Weight in percent, max 2 decimals")] = "100",
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Assign an instrument or cash account to a classification with a weight in percent
    (creates or replaces the assignment). A vehicle's weights within one taxonomy must
    not exceed 100 % (`weight-exceeds-100`). The change is in memory only until save_file."""
    body = {"weight": decimal_str(weight, WEIGHT, "weight")}
    async with api() as pp:
        f = await pp.resolve_file(file)
        path = fpath(f, "taxonomies", taxonomy_id, "classifications", classification_id, "assignments", vehicle_uuid)
        return out(await pp.put(path, params=dry(dry_run), body=body))


@mcp.tool(annotations=DESTRUCTIVE)
async def unassign_classification(
    taxonomy_id: TaxonomyId,
    classification_id: ClassificationId,
    vehicle_uuid: Annotated[str, Field(description="UUID of the assigned instrument or cash account")],
    file: FileParam = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Remove an assignment from a classification. The change is in memory only until save_file."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        path = fpath(f, "taxonomies", taxonomy_id, "classifications", classification_id, "assignments", vehicle_uuid)
        return deleted(await pp.delete(path, params=dry(dry_run)), vehicle_uuid=vehicle_uuid)
