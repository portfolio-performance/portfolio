package name.abuchen.portfolio.checks.impl;

import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;

/**
 * Detects whether check issues involve ledger-backed transaction projections.
 * This is internal check support. Repairs should use Ledger-aware delete or mutation paths
 * instead of changing runtime projections directly.
 */
/* package */final class LedgerCheckSupport
{
    private LedgerCheckSupport()
    {
    }

    static boolean isLedgerBacked(Transaction transaction)
    {
        return transaction instanceof LedgerBackedTransaction;
    }

    static boolean isLedgerBacked(BuySellEntry entry)
    {
        return isLedgerBacked(entry.getAccountTransaction()) || isLedgerBacked(entry.getPortfolioTransaction());
    }

    static boolean isLedgerBacked(AccountTransferEntry entry)
    {
        return isLedgerBacked(entry.getSourceTransaction()) || isLedgerBacked(entry.getTargetTransaction());
    }

    static boolean isLedgerBacked(PortfolioTransferEntry entry)
    {
        return isLedgerBacked(entry.getSourceTransaction()) || isLedgerBacked(entry.getTargetTransaction());
    }
}
