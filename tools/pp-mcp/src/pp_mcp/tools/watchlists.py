"""Watchlists, addressed by name (PP watchlists have no UUID)."""

from typing import Annotated, Any

from fastmcp.exceptions import ToolError
from pydantic import Field

from pp_mcp.app import DESTRUCTIVE, READ_ONLY, WRITE, mcp
from pp_mcp.tools._common import ClientRef, DryRun, FileParam, api, compact, deleted, dry, fpath, out

Name = Annotated[str, Field(description="Watchlist name (watchlists are identified by name)")]
Instrument = Annotated[str, Field(description="Instrument UUID")]


@mcp.tool(annotations=READ_ONLY)
async def list_watchlists(file: FileParam = None) -> dict[str, Any]:
    """List the watchlists of a file: `{items: [{name, instruments: [{uuid, name}]}]}`."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.get(fpath(f, "watchlists")))


@mcp.tool(annotations=READ_ONLY)
async def get_watchlist(name: Name, file: FileParam = None) -> dict[str, Any]:
    """Read one watchlist: `{name, instruments: [{uuid, name}]}`."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.get(fpath(f, "watchlists", name)))


@mcp.tool(annotations=WRITE)
async def create_watchlist(
    name: Name,
    file: FileParam = None,
    instruments: Annotated[list[str] | None, Field(description="Instrument UUIDs to add")] = None,
    client_ref: ClientRef = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Create a watchlist; names must be unique (`already-exists`). Returns the watchlist.
    The change is in memory only until save_file."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        body = compact(name=name, instruments=instruments, clientRef=client_ref)
        return out(await pp.post(fpath(f, "watchlists"), params=dry(dry_run), body=body))


@mcp.tool(annotations=WRITE)
async def update_watchlist(
    name: Name,
    file: FileParam = None,
    new_name: Annotated[str | None, Field(description="Rename the watchlist")] = None,
    instruments: Annotated[
        list[str] | None,
        Field(description="Replace the members with these instrument UUIDs (each once; [] empties the list)"),
    ] = None,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Rename a watchlist and/or replace its instruments. Returns the watchlist.
    The change is in memory only until save_file."""
    patch = compact(name=new_name, instruments=instruments)
    if not patch:
        raise ToolError("nothing to update: pass new_name and/or instruments")
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.patch(fpath(f, "watchlists", name), params=dry(dry_run), body=patch))


@mcp.tool(annotations=DESTRUCTIVE)
async def delete_watchlist(name: Name, file: FileParam = None, dry_run: DryRun = False) -> dict[str, Any]:
    """Delete a watchlist (its instruments are not affected). A dry run answers
    `{dryRun: true, removed: [watchlist]}`. The change is in memory only until save_file."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        return deleted(await pp.delete(fpath(f, "watchlists", name), params=dry(dry_run)), name=name)


@mcp.tool(annotations=WRITE)
async def add_to_watchlist(
    name: Name, instrument: Instrument, file: FileParam = None, dry_run: DryRun = False
) -> dict[str, Any]:
    """Add an instrument to a watchlist (no-op if it is already a member). Returns the
    watchlist. The change is in memory only until save_file."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.put(fpath(f, "watchlists", name, "instruments", instrument), params=dry(dry_run)))


@mcp.tool(annotations=WRITE)
async def remove_from_watchlist(
    name: Name, instrument: Instrument, file: FileParam = None, dry_run: DryRun = False
) -> dict[str, Any]:
    """Remove an instrument from a watchlist (`not-found` if it is not a member).
    Returns the watchlist. The change is in memory only until save_file."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.delete(fpath(f, "watchlists", name, "instruments", instrument), params=dry(dry_run)))
