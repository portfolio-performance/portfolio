"""Watchlists, addressed by name (PP watchlists have no UUID)."""

from typing import Annotated, Any

from pydantic import Field

from pp_mcp.app import DESTRUCTIVE, READ_ONLY, WRITE, mcp
from pp_mcp.tools._common import DryRun, FileParam, api, compact, deleted, dry, fpath, out

Name = Annotated[str, Field(description="Watchlist name (watchlists are identified by name)")]
Instrument = Annotated[str, Field(description="Instrument UUID")]


@mcp.tool(annotations=READ_ONLY)
async def list_watchlists(file: FileParam = None) -> dict[str, Any]:
    """List the watchlists of a file with their instruments."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.get(fpath(f, "watchlists")))


@mcp.tool(annotations=WRITE)
async def create_watchlist(
    name: Name,
    file: FileParam = None,
    instruments: Annotated[list[str] | None, Field(description="Instrument UUIDs to add")] = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Create a watchlist; names must be unique (`already-exists`).
    The change is in memory only until save_file."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        body = compact(name=name, instruments=instruments)
        return out(await pp.post(fpath(f, "watchlists"), params=dry(dry_run), body=body))


@mcp.tool(annotations=WRITE)
async def rename_watchlist(name: Name, new_name: str, file: FileParam = None, dry_run: DryRun = False) -> dict[str, Any]:
    """Rename a watchlist. The change is in memory only until save_file."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.patch(fpath(f, "watchlists", name), params=dry(dry_run), body={"name": new_name}))


@mcp.tool(annotations=DESTRUCTIVE)
async def delete_watchlist(name: Name, file: FileParam = None, dry_run: DryRun = False) -> dict[str, Any]:
    """Delete a watchlist (its instruments are not affected).
    The change is in memory only until save_file."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        return deleted(await pp.delete(fpath(f, "watchlists", name), params=dry(dry_run)), name=name)


@mcp.tool(annotations=WRITE)
async def add_to_watchlist(
    name: Name, instrument: Instrument, file: FileParam = None, dry_run: DryRun = False
) -> dict[str, Any]:
    """Add an instrument to a watchlist. The change is in memory only until save_file."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        result = await pp.put(fpath(f, "watchlists", name, "instruments", instrument), params=dry(dry_run))
        return out(result if result is not None else {"added": True, "name": name, "instrument": instrument})


@mcp.tool(annotations=WRITE)
async def remove_from_watchlist(
    name: Name, instrument: Instrument, file: FileParam = None, dry_run: DryRun = False
) -> dict[str, Any]:
    """Remove an instrument from a watchlist. The change is in memory only until save_file."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        result = await pp.delete(fpath(f, "watchlists", name, "instruments", instrument), params=dry(dry_run))
        return out(result if result is not None else {"removed": True, "name": name, "instrument": instrument})
