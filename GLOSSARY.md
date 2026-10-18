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

**Exchange-rate instrument**:
A **Security** with a target currency, whose prices are the exchange rates between its currency and that target currency. Users create them to supply rates the built-in sources lack.
_Avoid_: currency pair, FX security

## AI/CLI vocabulary

Terms used on the external surface for AI agents, aligned with the upstream REST API proposal (PR #5870). They name the same things as the entries above.

**Instrument**:
The external name for a **Security**.

**Investment account**:
The external name for a **Portfolio**.

**Cash account**:
The external name for an **Account**.

**Reference cash account**:
The **Cash account** that an **Investment account** settles its buys, sells and dividends against by default.
_Avoid_: reference account, settlement account

**Master data**:
The **Instruments**, **Investment accounts** and **Cash accounts** in a client file, as opposed to the **Transaction records** and prices that refer to them.
_Avoid_: reference data, static data, entities

**Holding**:
The quantity of one **Instrument** held in an **Investment account** (or across all of them) at a given date, with its market value.
_Avoid_: position (internal core term), asset

**Retired**:
Marks an **Instrument**, **Investment account** or **Cash account** that the user no longer actively uses but keeps for its history.
_Avoid_: deactivated, inactive, archived

**Active in the period**:
An **Instrument** that was held at the start or end of a period, or had any transaction in it (a buy, sell, dividend, ...). An instrument that is not active in the period is _quiet_. Unrelated to **Retired**: a retired instrument can be active in a past period.
_Avoid_: active (alone), used, traded

**Attribute**:
A user-defined, typed field (text, number, percent, amount, date, ...) that the user attaches to **Instruments**, such as a fund's TER or vendor.
_Avoid_: custom field, property

**Reference**:
A string an agent uses to name one **Master data** entity: its UUID (canonical), or as a read-only convenience an ISIN, ticker or exact name. Unrelated to **Reference cash account**.
_Avoid_: identifier, selector, lookup key

**Price**:
The value of one unit of an **Instrument** on a date, in the instrument's currency.
_Avoid_: quote, rate

**Latest price**:
The **Price** that valuation uses as of a given date.
_Avoid_: current quote, last quote

**Stale price**:
A **Latest price** old enough that figures based on it need a caveat.
_Avoid_: outdated quote, old price

**Transaction record**:
One business event as shown to and edited by agents: a buy or sell with both its **Investment account** side and its **Cash account** side, or a transfer with its from and to accounts. It is created, changed and deleted as a whole, never one side at a time.
_Avoid_: leg, cross entry, half-transaction

**Side**:
One stored transaction within a **Transaction record**. A buy or sell has an **Investment account** side and a **Cash account** side, a transfer has an outgoing and an incoming side, and every other record has exactly one side. Any side's UUID identifies the whole record.
_Avoid_: leg, half, booking

**Transfer kind**:
Whether a transfer moves money between **Cash accounts** (cash) or **Instruments** between **Investment accounts** (security).
_Avoid_: transfer type (type is the record's type, which is TRANSFER for both kinds)
