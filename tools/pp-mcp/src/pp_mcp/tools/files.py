"""Open files: list, open, dirty state and save."""

from typing import Annotated, Any

from pydantic import Field

from pp_mcp.app import READ_ONLY, WRITE, mcp
from pp_mcp.tools._common import FileParam, api, fpath, out


@mcp.tool(annotations=READ_ONLY)
async def list_files() -> dict[str, Any]:
    """List the portfolio files that are open in PP and enabled for API access.

    Each item has `id`, `alias` (optional), `label`, `path` and `dirty` (true when
    the file has unsaved changes). Use `id` or `alias` as the `file` parameter of
    the other tools.
    """
    async with api() as pp:
        return out(await pp.get("/v1/files"))


@mcp.tool(annotations=READ_ONLY)
async def get_file(file: FileParam = None) -> dict[str, Any]:
    """Read one file: `id`, `alias`, `label`, `path` and `dirty` (unsaved changes)."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.get(fpath(f)))


@mcp.tool(annotations=WRITE)
async def open_file(
    path: Annotated[str, Field(description="Absolute path of a .portfolio/.xml file already enabled for API access")],
) -> dict[str, Any]:
    """Open a portfolio file in PP.

    Only paths the user has already enabled in Preferences → REST API can be opened;
    anything else is answered `not-found`. Returns the file object (`id`, `alias`,
    `label`, `path`, `dirty`).
    """
    async with api() as pp:
        return out(await pp.post("/v1/files/open", body={"path": path}))


@mcp.tool(annotations=WRITE)
async def save_file(
    file: FileParam = None,
    wait_for_updates: Annotated[
        int | None,
        Field(ge=0, le=60, description="Seconds PP waits for running background price updates before saving "
                                       "(default 30, 0 saves immediately)"),
    ] = None,
) -> dict[str, Any]:
    """Save a file to disk in its current format at its current path.

    All other write tools change only PP's in-memory copy (and mark it dirty);
    this is the only tool that persists changes. PP first waits for background
    updates that change the file (the online quote update after opening, dividend
    updates, automatic investment plan transactions), then saves. Returns the file
    object with `dirty`, `savedAt` and `backgroundUpdatesPending`. If
    `backgroundUpdatesPending` is true, an update was still running after the wait
    and `dirty` may stay true; call save_file again later.
    """
    params = {"waitForUpdates": str(wait_for_updates)} if wait_for_updates is not None else None
    wait = 30 if wait_for_updates is None else wait_for_updates
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.post(fpath(f, "save"), params=params, timeout=wait + 30.0))
