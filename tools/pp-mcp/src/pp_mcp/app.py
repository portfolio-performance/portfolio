"""The FastMCP server instance that every tool module registers with."""

from fastmcp import FastMCP

INSTRUCTIONS = """\
These tools read and edit the portfolio files open in a running Portfolio Performance (PP)
desktop application, through PP's local REST API. PP validates and books every change
exactly as its own dialogs do, and computes every report with its own engine.

Files
- Start with list_files. `file` is a file id or alias from there; omit it when exactly one
  file is listed. A file that is enabled for API access but not open can be opened with
  open_file(path). Files the user did not enable are invisible.

Saving
- Writes change only PP's in-memory copy and mark the file dirty, like an edit in the UI.
  Nothing reaches the disk until save_file. Save once at the end of a coherent batch of
  changes, not after every write, and only when the user wants the changes kept.
- save_file waits for running quote updates. If it answers backgroundUpdatesPending=true,
  save again later.

Before writing
- Look things up, never guess identifiers: find_instrument (by ISIN, WKN, ticker or name)
  and list_accounts (cash accounts have one currency; investment accounts hold shares and
  have a reference cash account).
- Use dry_run=True first when a write is derived from a document or an instruction you
  interpreted. Show the user the resolved result (date, instrument, accounts, shares,
  gross value, fees, taxes, exchange rate, total) and ask before the real write when the
  user has not already approved it.
- Pass client_ref on every transaction create with a stable key taken from the source
  (for example the broker's order or document number). Repeating the create with the same
  key returns the existing transaction instead of booking it twice.
- create_instrument and the account creates return an existing entity with the same ISIN
  (or name) instead of creating a duplicate; pass allow_duplicate=True only on purpose.
- Deleting is permanent once saved. Confirm deletes with the user.

Imports
- import_pdf and import_csv preview by default: they return the extracted items with a
  status (ok, warning, error) and an import id, and change nothing. commit_import books
  the items you select. Warnings such as probable duplicates need the user's decision.
- `errors` lists the documents PP could not read; PP has no extractor for them. Offer
  to book them with the create_* tools from what the document says, after confirmation.

Numbers and units
- Every number is a decimal string, never a float: money 2 decimals, shares 8, quotes 8,
  exchange rates up to 10, taxonomy weights in percent with 2 decimals.
- Amounts are in the transaction currency, which is the cash account's currency.
  quote/gross_value of buys, sells and deliveries are in the instrument currency.
- exchange_rate = units of transaction currency per 1 unit of instrument currency. Omit it
  when PP has a rate for the date; PP then looks it up.
- A buy or sell books both sides (investment account and cash account). Give shares plus
  quote, gross_value or total; PP derives the rest and checks it adds up.

Transaction types
- create_buy / create_sell: shares change hands for cash.
- create_delivery: shares come in or go out without cash (inbound/outbound).
- create_dividend: cash account + instrument; amounts in the cash account currency.
- create_cash_transaction: deposit, removal, interest, interest_charge, fees, fees_refund,
  taxes, tax_refund.
- create_transfer: cash between two cash accounts (target_amount when currencies differ).
- create_security_transfer: shares between two investment accounts.

Errors
- Errors name the offending field and a code. Fix the input rather than retrying as is.
- user-interaction means the user has a dialog open or edits a cell in PP. The tools
  already retried; tell the user and wait.
- Watchlists and investment plans are addressed by name, everything else by UUID.
"""

mcp = FastMCP("portfolio-performance", instructions=INSTRUCTIONS)

READ_ONLY = {"readOnlyHint": True, "openWorldHint": False}
WRITE = {"readOnlyHint": False, "destructiveHint": False, "openWorldHint": False}
DESTRUCTIVE = {"readOnlyHint": False, "destructiveHint": True, "openWorldHint": False}
