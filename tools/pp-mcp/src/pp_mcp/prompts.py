"""Prompt templates for recurring tasks."""

from typing import Annotated

from pydantic import Field

from pp_mcp.app import mcp


@mcp.prompt
def record_documents(
    paths: Annotated[str, Field(description="Absolute paths of the broker PDFs, separated by newlines or commas")],
    file: Annotated[str, Field(description="PP file id or alias; empty if only one file is open")] = "",
) -> str:
    """Book broker documents (trade confirmations, dividend and interest notices) into a PP file."""
    target = f"the PP file `{file}`" if file else "the open PP file"
    return f"""\
Book the following broker documents into {target}:

{paths}

1. Call list_files and list_accounts. Note which cash account and investment account belong
   to the broker of these documents; ask me if that is not obvious from the account names.
2. Call import_pdf with all paths, the chosen accounts, and dry_run=True.
3. Show me one line per extracted item: document, type, date, instrument, shares, amount,
   and its status. Explain every warning (for example a probable duplicate) and every
   error. Documents that PP could not read are listed separately; say so.
4. Wait for my confirmation. Then call commit_import with the import id and the item
   indexes I approved, using the same accounts.
5. Show the booked transactions (get_transaction for a few if useful) and ask whether to
   save. Call save_file only after I agree.
"""


@mcp.prompt
def review_portfolio(
    file: Annotated[str, Field(description="PP file id or alias; empty if only one file is open")] = "",
    since: Annotated[str, Field(description="Start of the performance period, YYYY-MM-DD; empty = one year")] = "",
) -> str:
    """Read-only overview of holdings, performance and income."""
    target = f"the PP file `{file}`" if file else "the open PP file"
    period = f"since {since}" if since else "over the last 12 months"
    return f"""\
Give me an overview of {target}. Do not change anything.

1. get_holdings: total assets, the ten largest positions with value and share of total,
   and cash per currency.
2. get_performance {period}: TTWROR, IRR, absolute change, fees and taxes.
3. get_security_performance for the same period: best and worst three positions.
4. get_earnings for the same period: dividends and interest per currency.
5. list_taxonomies, then get_taxonomy_allocation for the asset allocation taxonomy if
   there is one: allocation versus target.

Present the numbers as PP returns them, with currencies. Point out anything unusual,
such as instruments without a current price.
"""
