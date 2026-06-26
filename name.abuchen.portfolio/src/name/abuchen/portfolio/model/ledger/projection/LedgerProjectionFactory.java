package name.abuchen.portfolio.model.ledger.projection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import name.abuchen.portfolio.model.LedgerDiagnosticCode;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.ledger.LedgerEntry;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRef;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;

/**
 * Builds runtime legacy projection objects for a Ledger entry.
 * This is projection infrastructure. The created objects are compatibility views, not
 * persisted transaction facts.
 */
final class LedgerProjectionFactory
{
    Transaction createProjection(LedgerEntry entry, LedgerProjectionRef projectionRef)
    {
        Objects.requireNonNull(projectionRef);

        return createProjections(entry).stream() //
                        .filter(transaction -> transaction.getUUID().equals(projectionRef.getUUID())) //
                        .findFirst().orElseThrow(() -> new IllegalArgumentException(
                                        "Projection ref does not belong to entry: " + projectionRef.getUUID())); //$NON-NLS-1$
    }

    List<Transaction> createProjections(LedgerEntry entry)
    {
        Objects.requireNonNull(entry);

        var transactions = new ArrayList<Transaction>();

        for (var projectionRef : entry.getProjectionRefs())
            transactions.add(create(entry, projectionRef));

        attachCrossEntry(entry, transactions);

        return List.copyOf(transactions);
    }

    private Transaction create(LedgerEntry entry, LedgerProjectionRef projectionRef)
    {
        if (LedgerProjectionSupport.isAccountProjection(projectionRef))
            return new LedgerBackedAccountTransaction(entry, projectionRef);

        if (LedgerProjectionSupport.isPortfolioProjection(projectionRef))
            return new LedgerBackedPortfolioTransaction(entry, projectionRef);

        throw new UnsupportedOperationException(LedgerDiagnosticCode.LEDGER_PROJ_071
                        .message("Unsupported ledger projection role " + projectionRef.getRole())); //$NON-NLS-1$
    }

    private void attachCrossEntry(LedgerEntry entry, List<Transaction> transactions)
    {
        if (transactions.size() != 2)
            return;

        if (entry.getType() == LedgerEntryType.CASH_TRANSFER)
        {
            attachAccountTransferCrossEntry(entry, transactions);
            return;
        }

        if (entry.getType() == LedgerEntryType.SECURITY_TRANSFER)
        {
            attachPortfolioTransferCrossEntry(entry, transactions);
            return;
        }

        if (entry.getType() == LedgerEntryType.BUY || entry.getType() == LedgerEntryType.SELL)
        {
            attachBuySellCrossEntry(entry, transactions);
            return;
        }

        var crossEntry = new LedgerBackedCrossEntry(entry, transactions);

        for (var transaction : transactions)
        {
            if (transaction instanceof LedgerBackedAccountTransaction accountTransaction)
                accountTransaction.setLedgerCrossEntry(crossEntry);
            else if (transaction instanceof LedgerBackedPortfolioTransaction portfolioTransaction)
                portfolioTransaction.setLedgerCrossEntry(crossEntry);
        }
    }

    private void attachAccountTransferCrossEntry(LedgerEntry entry, List<Transaction> transactions)
    {
        var sourceTransaction = (LedgerBackedAccountTransaction) transaction(entry, transactions,
                        LedgerProjectionRole.SOURCE_ACCOUNT);
        var targetTransaction = (LedgerBackedAccountTransaction) transaction(entry, transactions,
                        LedgerProjectionRole.TARGET_ACCOUNT);
        var crossEntry = AccountTransferEntry.readOnly(sourceTransaction.getLedgerProjectionRef().getAccount(),
                        (AccountTransaction) sourceTransaction, targetTransaction.getLedgerProjectionRef().getAccount(),
                        (AccountTransaction) targetTransaction);

        sourceTransaction.setLedgerCrossEntry(crossEntry);
        targetTransaction.setLedgerCrossEntry(crossEntry);
    }

    private void attachPortfolioTransferCrossEntry(LedgerEntry entry, List<Transaction> transactions)
    {
        var sourceTransaction = (LedgerBackedPortfolioTransaction) transaction(entry, transactions,
                        LedgerProjectionRole.SOURCE_PORTFOLIO);
        var targetTransaction = (LedgerBackedPortfolioTransaction) transaction(entry, transactions,
                        LedgerProjectionRole.TARGET_PORTFOLIO);
        var crossEntry = PortfolioTransferEntry.readOnly(sourceTransaction.getLedgerProjectionRef().getPortfolio(),
                        (PortfolioTransaction) sourceTransaction,
                        targetTransaction.getLedgerProjectionRef().getPortfolio(),
                        (PortfolioTransaction) targetTransaction);

        sourceTransaction.setLedgerCrossEntry(crossEntry);
        targetTransaction.setLedgerCrossEntry(crossEntry);
    }

    private void attachBuySellCrossEntry(LedgerEntry entry, List<Transaction> transactions)
    {
        var accountTransaction = (LedgerBackedAccountTransaction) transaction(entry, transactions,
                        LedgerProjectionRole.ACCOUNT);
        var portfolioTransaction = (LedgerBackedPortfolioTransaction) transaction(entry, transactions,
                        LedgerProjectionRole.PORTFOLIO);
        var crossEntry = BuySellEntry.readOnly(portfolioTransaction.getLedgerProjectionRef().getPortfolio(),
                        (PortfolioTransaction) portfolioTransaction, accountTransaction.getLedgerProjectionRef()
                                        .getAccount(),
                        (AccountTransaction) accountTransaction);

        accountTransaction.setLedgerCrossEntry(crossEntry);
        portfolioTransaction.setLedgerCrossEntry(crossEntry);
    }

    private Transaction transaction(LedgerEntry entry, List<Transaction> transactions, LedgerProjectionRole role)
    {
        var projection = entry.getProjectionRefs().stream() //
                        .filter(ref -> ref.getRole() == role) //
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Projection not found: " + role)); //$NON-NLS-1$

        return transactions.stream() //
                        .filter(transaction -> transaction.getUUID().equals(projection.getUUID())) //
                        .findFirst().orElseThrow();
    }
}
