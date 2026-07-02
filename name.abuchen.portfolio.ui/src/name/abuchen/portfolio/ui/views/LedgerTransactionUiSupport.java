package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.IStructuredSelection;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CrossEntry;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.model.ledger.compatibility.LedgerNativeComponentInspectorModel;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;

public final class LedgerTransactionUiSupport
{
    private LedgerTransactionUiSupport()
    {
    }

    public static List<TransactionPair<?>> transactionsForView(Client client)
    {
        List<TransactionPair<?>> transactions = new ArrayList<>();

        for (var portfolio : client.getPortfolios())
            portfolio.getTransactions().stream().map(t -> new TransactionPair<>(portfolio, t))
                            .forEach(transactions::add);

        for (var account : client.getAccounts())
            account.getTransactions().stream()
                            .filter(t -> t.getType() != AccountTransaction.Type.BUY
                                            && t.getType() != AccountTransaction.Type.SELL)
                            .map(t -> new TransactionPair<>(account, t)).forEach(transactions::add);

        return transactions;
    }

    public static boolean matchesClientFilter(PortfolioClientFilter clientFilter, TransactionPair<?> tx)
    {
        if (isTransfer(tx))
            return clientFilter.hasElement(tx.getOwner());

        return clientFilter.hasElement(tx.getOwner()) || (tx.getTransaction().getCrossEntry() != null
                        && clientFilter.hasElement(tx.getTransaction().getCrossEntry()
                                        .getCrossOwner(tx.getTransaction())));
    }

    public static void deleteTransactions(Client client, Object[] selectedTransactions)
    {
        Set<String> deletedLedgerEntryUUIDs = new HashSet<>();
        Set<String> deletedTransactionUUIDs = new HashSet<>();

        for (Object item : selectedTransactions)
        {
            TransactionPair<?> pair = (TransactionPair<?>) item;
            var transaction = pair.getTransaction();
            var ledgerEntryUUID = pair.getLedgerEntryUUID();

            if (ledgerEntryUUID.isPresent())
            {
                if (!deletedLedgerEntryUUIDs.add(ledgerEntryUUID.get()))
                    continue;

                pair.deleteTransaction(client);
                continue;
            }

            if (!deletedTransactionUUIDs.add(transaction.getUUID()))
                continue;

            CrossEntry crossEntry = transaction.getCrossEntry();
            if (crossEntry != null)
            {
                var crossTransaction = crossEntry.getCrossTransaction(transaction);

                if (crossTransaction != null)
                    deletedTransactionUUIDs.add(crossTransaction.getUUID());
            }

            pair.deleteTransaction(client);
        }
    }

    public static boolean supportsBuySellToDeliveryAction(
                    Collection<TransactionPair<PortfolioTransaction>> txCollection)
    {
        return !txCollection.isEmpty() && txCollection.stream().noneMatch(LedgerTransactionUiSupport::isNativeTargetedProjection)
                        && txCollection.stream().allMatch(tx -> {
            var type = tx.getTransaction().getType();
            return type == PortfolioTransaction.Type.BUY || type == PortfolioTransaction.Type.SELL;
        });
    }

    public static boolean supportsDeliveryToBuySellAction(
                    Collection<TransactionPair<PortfolioTransaction>> txCollection)
    {
        return !txCollection.isEmpty() && txCollection.stream().noneMatch(LedgerTransactionUiSupport::isNativeTargetedProjection)
                        && txCollection.stream().allMatch(tx -> {
            var type = tx.getTransaction().getType();
            return type == PortfolioTransaction.Type.DELIVERY_INBOUND
                            || type == PortfolioTransaction.Type.DELIVERY_OUTBOUND;
        });
    }

    public static boolean containsNativeTargetedProjection(IStructuredSelection selection)
    {
        return selection.stream() //
                        .filter(TransactionPair.class::isInstance) //
                        .map(TransactionPair.class::cast) //
                        .anyMatch(LedgerTransactionUiSupport::isNativeTargetedProjection);
    }

    public static boolean containsNativeTargetedAccountTransaction(IStructuredSelection selection)
    {
        return selection.stream() //
                        .filter(AccountTransaction.class::isInstance) //
                        .map(AccountTransaction.class::cast) //
                        .anyMatch(LedgerTransactionUiSupport::isNativeTargetedProjection);
    }

    public static boolean isNativeTargetedProjection(TransactionPair<?> tx)
    {
        return isNativeTargetedProjection(tx.getTransaction());
    }

    public static boolean isNativeTargetedProjection(Transaction transaction)
    {
        return LedgerNativeComponentInspectorModel.isLedgerNativeTargetedProjection(transaction);
    }

    private static boolean isTransfer(TransactionPair<?> tx)
    {
        var transaction = tx.getTransaction();

        return transaction instanceof AccountTransaction accountTransaction
                        && (accountTransaction.getType() == AccountTransaction.Type.TRANSFER_IN
                                        || accountTransaction.getType() == AccountTransaction.Type.TRANSFER_OUT)
                        || transaction instanceof PortfolioTransaction portfolioTransaction
                                        && (portfolioTransaction.getType() == PortfolioTransaction.Type.TRANSFER_IN
                                                        || portfolioTransaction
                                                                        .getType() == PortfolioTransaction.Type.TRANSFER_OUT);
    }
}
