"""Application actions: online quote update (async job) and stock split."""

import math
import time
from typing import Annotated, Any, Literal

from pydantic import Field

from pp_mcp import client as client_module
from pp_mcp.app import READ_ONLY, WRITE, mcp
from pp_mcp.money import SHARES, date_str, decimal_str
from pp_mcp.tools._common import DryRun, FileParam, api, dry, fpath, out

MAX_SERVER_WAIT_SECONDS = 60
POLL_INTERVAL_SECONDS = 1.0
TERMINAL = {"done", "failed"}


def _terminal(job: Any) -> bool:
    return isinstance(job, dict) and str(job.get("status", "")).lower() in TERMINAL


@mcp.tool(annotations={"readOnlyHint": False, "destructiveHint": False, "openWorldHint": True})
async def update_quotes(
    file: FileParam = None,
    instruments: Annotated[list[str] | None, Field(description="Instrument UUIDs; omit for all instruments")] = None,
    targets: Annotated[
        list[Literal["latest", "historic"]], Field(description="Which quotes to fetch")
    ] = ["latest", "historic"],  # noqa: B006 - immutable default for the schema
    wait_seconds: Annotated[int, Field(description="Wait up to this long for completion (0 = return at once)", ge=0)] = 30,
) -> dict[str, Any]:
    """Fetch online quotes (latest and/or historic) through the instruments' quote feeds.

    Starts a PP background job and waits up to `wait_seconds` for it, polling the
    job status. Returns the job: `{jobId, status: running|done|failed, summary?}`;
    when it is still running, call get_job later. Updated prices change the
    in-memory file only until save_file.
    """
    body: dict[str, Any] = {"targets": list(dict.fromkeys(targets))}
    if instruments is not None:
        body["instruments"] = instruments
    server_wait = min(wait_seconds, MAX_SERVER_WAIT_SECONDS)
    params = {"wait": server_wait} if server_wait > 0 else None
    started = time.monotonic()
    async with api() as pp:
        f = await pp.resolve_file(file)
        job = await pp.post(
            fpath(f, "actions", "update-quotes"), params=params, body=body, timeout=server_wait + 30.0
        )
        if wait_seconds > 0 and not _terminal(job):
            remaining = wait_seconds - (time.monotonic() - started)
            polls = max(1, math.ceil(remaining / POLL_INTERVAL_SECONDS))
            for _ in range(polls):
                await client_module._sleep(POLL_INTERVAL_SECONDS)
                job = await pp.get(fpath(f, "jobs", str(job["jobId"])))
                if _terminal(job):
                    break
    result = out(job)
    if not _terminal(job):
        result["note"] = f"The quote update is still running; check it with get_job(job_id={job['jobId']!r})."
    return result


@mcp.tool(annotations=READ_ONLY)
async def get_job(job_id: Annotated[str, Field(description="Job id from update_quotes")], file: FileParam = None) -> dict[str, Any]:
    """Status of a background job: `{jobId, status: pending|running|done|failed, startedAt,
    finishedAt?, summary?}`. Jobs are kept for one hour after completion."""
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.get(fpath(f, "jobs", job_id)))


@mcp.tool(annotations=WRITE)
async def apply_stock_split(
    uuid: Annotated[str, Field(description="Instrument UUID")],
    ex_date: Annotated[str, Field(description="Ex-date YYYY-MM-DD; data before it is adjusted")],
    new_shares: Annotated[str, Field(description="New shares in the ratio, e.g. '4' for a 4:1 split (decimal string)")],
    old_shares: Annotated[str, Field(description="Old shares in the ratio, e.g. '1' for a 4:1 split (decimal string)")],
    file: FileParam = None,
    adjust_transactions: Annotated[bool, Field(description="Multiply shares of transactions before ex_date")] = True,
    adjust_prices: Annotated[bool, Field(description="Divide historical prices before ex_date")] = True,
    dry_run: DryRun = False,
) -> dict[str, Any]:
    """Apply a stock split like PP's split wizard: records a split event and adjusts the
    shares of all transactions and the historical prices dated before `ex_date` by
    new_shares:old_shares. A dry run returns the counts of affected transactions and
    prices. The change is in memory only until save_file."""
    body = {
        "exDate": date_str(ex_date, "ex_date"),
        "newShares": decimal_str(new_shares, SHARES, "new_shares"),
        "oldShares": decimal_str(old_shares, SHARES, "old_shares"),
        "adjustTransactions": adjust_transactions,
        "adjustPrices": adjust_prices,
    }
    async with api() as pp:
        f = await pp.resolve_file(file)
        return out(await pp.post(fpath(f, "instruments", uuid, "actions", "split"), params=dry(dry_run), body=body))
