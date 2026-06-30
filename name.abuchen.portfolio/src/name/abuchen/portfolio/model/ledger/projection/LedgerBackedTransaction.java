package name.abuchen.portfolio.model.ledger.projection;

import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;

/**
 * Marks a runtime legacy transaction view that is backed by a Ledger entry.
 * This projection interface lets compatibility code route writes back to the Ledger.
 * Contributors should still use creator, editor, converter, or deleter classes for writes.
 */
public interface LedgerBackedTransaction
{
    LedgerEntry getLedgerEntry();

    LedgerProjectionRef getLedgerProjectionRef();
}
