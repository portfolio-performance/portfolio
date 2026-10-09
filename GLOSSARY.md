# Portfolio Performance

Desktop application for tracking and analysing a personal investment holding: securities, cash, transactions and their performance.

## Language

**Client file**:
The single file holding one user's complete data set (securities, accounts, portfolios, transactions, taxonomies, settings); persisted as XML, compressed XML, binary or encrypted.
_Avoid_: portfolio file, portfolio.xml, database

**Client**:
The in-memory root of everything stored in a client file.
_Avoid_: portfolio, workspace

**Portfolio**:
A securities account (depot) holding security positions and recording buy, sell and transfer transactions.
_Avoid_: depot, securities account; never use for the whole client file

**Account**:
A cash account in one currency, recording deposits, removals, dividends, interest, fees and taxes.
_Avoid_: cash account, bank account

**Security**:
A tradable instrument (stock, fund, bond, crypto, ...) that portfolios hold and transactions refer to.
_Avoid_: stock, asset

## AI/CLI vocabulary

Terms used on the external surface for AI agents, aligned with the upstream REST API proposal (PR #5870). They name the same things as the entries above.

**Instrument**:
The external name for a **Security**.

**Investment account**:
The external name for a **Portfolio**.

**Cash account**:
The external name for an **Account**.

**Holding**:
The quantity of one **Instrument** held in an **Investment account** (or across all of them) at a given date, with its market value.
_Avoid_: position (internal core term), asset
