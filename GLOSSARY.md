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
