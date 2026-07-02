package name.abuchen.portfolio.model.ledger.compatibility;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.LedgerAccountTypeToggleConverter;
import name.abuchen.portfolio.model.LedgerBuySellDeliveryConverter;
import name.abuchen.portfolio.model.LedgerBuySellReversalConverter;
import name.abuchen.portfolio.model.LedgerDeliveryDirectionConverter;
import name.abuchen.portfolio.model.LedgerPortfolioCompositeTypeConverter;
import name.abuchen.portfolio.model.LedgerTransferDirectionConverter;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.model.ledger.LedgerProjectionRole;
import name.abuchen.portfolio.model.ledger.configuration.LedgerEntryType;
import name.abuchen.portfolio.model.ledger.projection.LedgerBackedTransaction;

/**
 * Applies the Ledger-V6 transaction-table inline-editing matrix.
 * SOURCE and TRANSACTION_SOURCE intentionally map to the same transaction source property.
 */
public final class LedgerInlineEditingPolicy
{
    private static final Map<LedgerProjectionRole, Map<LedgerEntryType, Set<LedgerInlineEditingField>>> MATRIX = matrix();

    private LedgerInlineEditingPolicy()
    {
    }

    public static boolean isEditable(Object element, LedgerInlineEditingField field)
    {
        var transaction = transaction(element);

        if (!(transaction instanceof LedgerBackedTransaction ledgerBackedTransaction))
            return true;

        return isEditable(ledgerBackedTransaction.getLedgerEntry().getType(),
                        ledgerBackedTransaction.getLedgerProjectionRole(), field);
    }

    public static boolean isEditable(LedgerEntryType type, LedgerProjectionRole role, LedgerInlineEditingField field)
    {
        if (type == null || role == null || field == null)
            return false;

        var byType = MATRIX.get(role);
        if (byType == null)
            return false;

        return byType.getOrDefault(type, Set.of()).contains(field);
    }

    public static boolean isNativeTargetedProjection(Object element)
    {
        var tx = transaction(element);
        return tx != null && LedgerNativeComponentInspectorModel.isLedgerNativeTargetedProjection(tx);
    }

    public static boolean canEditOwner(Client client, Transaction transaction)
    {
        if (transaction == null || transaction.getCrossEntry() == null)
            return false;

        if (!isLedgerBacked(client, transaction))
            return true;

        return canUpdateOwner(client, transaction);
    }

    public static boolean canUpdateOwner(Client client, Transaction transaction)
    {
        var crossEntry = transaction.getCrossEntry();

        if (crossEntry instanceof AccountTransferEntry entry)
            return new LedgerAccountTransferTransactionCreator(client).canUpdate(entry);

        if (crossEntry instanceof PortfolioTransferEntry entry)
            return new LedgerPortfolioTransferTransactionCreator(client).canUpdate(entry);

        if (crossEntry instanceof BuySellEntry entry)
            return new LedgerBuySellTransactionCreator(client).canUpdate(entry);

        return false;
    }

    public static boolean updateOwner(Client client, Transaction transaction, TransactionOwner<?> oldValue,
                    TransactionOwner<?> newValue)
    {
        var crossEntry = transaction.getCrossEntry();

        if (crossEntry instanceof BuySellEntry entry)
        {
            var helper = new LedgerOwnerPatchHelper(client);
            if (newValue instanceof Account newAccount)
                helper.moveBuySellAccountSide(entry, newAccount);
            else if (newValue instanceof Portfolio newPortfolio)
                helper.moveBuySellPortfolioSide(entry, newPortfolio);
            else
                return false;
            return true;
        }

        if (crossEntry instanceof AccountTransferEntry entry && newValue instanceof Account account)
        {
            var helper = new LedgerOwnerPatchHelper(client);

            if (oldValue.equals(entry.getSourceAccount()))
                helper.moveAccountTransferSource(entry, account);
            else if (oldValue.equals(entry.getTargetAccount()))
                helper.moveAccountTransferTarget(entry, account);
            else
                return false;
            return true;
        }

        if (crossEntry instanceof PortfolioTransferEntry entry && newValue instanceof Portfolio portfolio)
        {
            var helper = new LedgerOwnerPatchHelper(client);

            if (oldValue.equals(entry.getSourcePortfolio()))
                helper.movePortfolioTransferSource(entry, portfolio);
            else if (oldValue.equals(entry.getTargetPortfolio()))
                helper.movePortfolioTransferTarget(entry, portfolio);
            else
                return false;
            return true;
        }

        return false;
    }

    public static boolean supportsTypeTransition(Client client, TransactionPair<?> pair, Enum<?> fromValue,
                    Enum<?> toValue)
    {
        var transaction = pair.getTransaction();

        if (!isLedgerBacked(client, transaction))
            return true;

        if (isLedgerBackedAccountTransfer(client, transaction))
            return (fromValue == AccountTransaction.Type.TRANSFER_IN && toValue == AccountTransaction.Type.TRANSFER_OUT
                            || fromValue == AccountTransaction.Type.TRANSFER_OUT
                                            && toValue == AccountTransaction.Type.TRANSFER_IN)
                            && new LedgerTransferDirectionConverter(client)
                                            .canReverseSafely((AccountTransferEntry) transaction.getCrossEntry());

        if (isLedgerBackedPortfolioTransfer(client, transaction))
            return (fromValue == PortfolioTransaction.Type.TRANSFER_IN
                            && toValue == PortfolioTransaction.Type.TRANSFER_OUT
                            || fromValue == PortfolioTransaction.Type.TRANSFER_OUT
                                            && toValue == PortfolioTransaction.Type.TRANSFER_IN)
                            && new LedgerTransferDirectionConverter(client)
                                            .canReverseSafely((PortfolioTransferEntry) transaction.getCrossEntry());

        if (isLedgerBackedBuySell(client, transaction))
            return supportsBuySellTypeTransition(client, pair, fromValue, toValue);

        if (isLedgerBackedDelivery(client, transaction))
            return supportsDeliveryTypeTransition(client, pair, fromValue, toValue);

        if (isLedgerBackedAccountOnly(client, transaction))
            return supportsAccountOnlyTypeTransition(client, pair, fromValue, toValue);

        return false;
    }

    public static boolean canUpdateDividendExDate(Client client, AccountTransaction transaction)
    {
        return client != null && new LedgerDividendTransactionCreator(client).canUpdate(transaction);
    }

    public static boolean updateDividendExDate(Client client, Account owner, AccountTransaction transaction,
                    LocalDateTime exDate)
    {
        if (!canUpdateDividendExDate(client, transaction))
            return false;

        new LedgerDividendTransactionCreator(client).update(transaction, owner, transaction.getType(),
                        transaction.getDateTime(), transaction.getAmount(), transaction.getCurrencyCode(),
                        transaction.getSecurity(), transaction.getShares(), exDate, null,
                        LedgerUnitPostingPatch.none(), transaction.getNote(), transaction.getSource());
        return true;
    }

    public static boolean updateDividendShares(Client client, Account owner, AccountTransaction transaction,
                    long shares)
    {
        var creator = new LedgerDividendTransactionCreator(client);
        if (!creator.canUpdate(transaction))
            return false;

        creator.update(transaction, owner, transaction.getType(), transaction.getDateTime(), transaction.getAmount(),
                        transaction.getCurrencyCode(), transaction.getSecurity(), shares, transaction.getExDate(),
                        null, null, transaction.getUnits().toList(), transaction.getNote(), transaction.getSource());
        return true;
    }

    public static boolean updateShares(Client client, TransactionPair<?> pair, long shares)
    {
        if (!isEditable(pair, LedgerInlineEditingField.SHARES))
            return false;

        var transaction = pair.getTransaction();

        if (transaction.getCrossEntry() instanceof BuySellEntry buySellEntry)
        {
            var creator = new LedgerBuySellTransactionCreator(client);
            if (!creator.canUpdate(buySellEntry))
                return false;

            var portfolioTransaction = buySellEntry.getPortfolioTransaction();
            creator.update(buySellEntry, buySellEntry.getPortfolio(), buySellEntry.getAccount(),
                            portfolioTransaction.getType(), portfolioTransaction.getDateTime(),
                            portfolioTransaction.getAmount(), portfolioTransaction.getCurrencyCode(),
                            portfolioTransaction.getSecurity(), shares, portfolioTransaction.getUnits().toList(),
                            portfolioTransaction.getNote(), portfolioTransaction.getSource());
            return true;
        }

        if (transaction.getCrossEntry() instanceof PortfolioTransferEntry transferEntry)
        {
            var creator = new LedgerPortfolioTransferTransactionCreator(client);
            if (!creator.canUpdate(transferEntry))
                return false;

            var sourceTransaction = transferEntry.getSourceTransaction();
            creator.update(transferEntry, transferEntry.getSourcePortfolio(), transferEntry.getTargetPortfolio(),
                            sourceTransaction.getSecurity(), sourceTransaction.getDateTime(), shares,
                            sourceTransaction.getAmount(), sourceTransaction.getCurrencyCode(),
                            sourceTransaction.getNote(), sourceTransaction.getSource());
            return true;
        }

        if (transaction instanceof PortfolioTransaction portfolioTransaction)
        {
            var creator = new LedgerDeliveryTransactionCreator(client);
            if (!creator.canUpdate(portfolioTransaction))
                return false;

            creator.update(portfolioTransaction, (Portfolio) pair.getOwner(), portfolioTransaction.getType(),
                            portfolioTransaction.getDateTime(), portfolioTransaction.getAmount(),
                            portfolioTransaction.getCurrencyCode(), portfolioTransaction.getSecurity(), shares, null,
                            null, portfolioTransaction.getUnits().toList(), portfolioTransaction.getNote(),
                            portfolioTransaction.getSource());
            return true;
        }

        if (transaction instanceof AccountTransaction accountTransaction)
            return updateDividendShares(client, (Account) pair.getOwner(), accountTransaction, shares);

        return false;
    }

    public static boolean isLedgerBackedAccountTransaction(Client client, AccountTransaction transaction)
    {
        if (client == null)
            return false;

        if (new LedgerAccountOnlyTransactionCreator(client).canUpdate(transaction))
            return true;

        if (transaction.getCrossEntry() instanceof BuySellEntry entry
                        && new LedgerBuySellTransactionCreator(client).isLedgerBacked(entry))
            return true;

        return transaction.getCrossEntry() instanceof AccountTransferEntry entry
                        && new LedgerAccountTransferTransactionCreator(client).isLedgerBacked(entry);
    }

    public static boolean isLedgerBacked(Client client, Transaction transaction)
    {
        return isLedgerBackedAccountTransfer(client, transaction) || isLedgerBackedPortfolioTransfer(client, transaction)
                        || isLedgerBackedBuySell(client, transaction) || isLedgerBackedDelivery(client, transaction)
                        || isLedgerBackedAccountOnly(client, transaction) || isLedgerBackedDividend(client, transaction);
    }

    private static Transaction transaction(Object element)
    {
        if (element instanceof TransactionPair<?> pair)
            return pair.getTransaction();

        if (element instanceof Transaction transaction)
            return transaction;

        return null;
    }

    @SuppressWarnings("unchecked")
    private static TransactionPair<AccountTransaction> accountPair(TransactionPair<?> pair)
    {
        return (TransactionPair<AccountTransaction>) pair;
    }

    @SuppressWarnings("unchecked")
    private static TransactionPair<PortfolioTransaction> portfolioPair(TransactionPair<?> pair)
    {
        return (TransactionPair<PortfolioTransaction>) pair;
    }

    private static boolean supportsBuySellTypeTransition(Client client, TransactionPair<?> pair, Enum<?> fromValue,
                    Enum<?> toValue)
    {
        var transaction = pair.getTransaction();

        if (fromValue == AccountTransaction.Type.BUY)
            return toValue == AccountTransaction.Type.SELL && new LedgerBuySellReversalConverter(client)
                            .canReverseSafely((BuySellEntry) transaction.getCrossEntry());
        if (fromValue == AccountTransaction.Type.SELL)
            return toValue == AccountTransaction.Type.BUY && new LedgerBuySellReversalConverter(client)
                            .canReverseSafely((BuySellEntry) transaction.getCrossEntry());
        if (fromValue == PortfolioTransaction.Type.BUY)
            return toValue == PortfolioTransaction.Type.SELL && new LedgerBuySellReversalConverter(client)
                            .canReverseSafely((BuySellEntry) transaction.getCrossEntry())
                            || toValue == PortfolioTransaction.Type.DELIVERY_INBOUND
                                            && new LedgerBuySellDeliveryConverter(client)
                                                            .canConvertSafely(portfolioPair(pair))
                            || toValue == PortfolioTransaction.Type.DELIVERY_OUTBOUND
                                            && new LedgerPortfolioCompositeTypeConverter(client)
                                                            .canConvertSafely(portfolioPair(pair));
        if (fromValue == PortfolioTransaction.Type.SELL)
            return toValue == PortfolioTransaction.Type.BUY && new LedgerBuySellReversalConverter(client)
                            .canReverseSafely((BuySellEntry) transaction.getCrossEntry())
                            || toValue == PortfolioTransaction.Type.DELIVERY_OUTBOUND
                                            && new LedgerBuySellDeliveryConverter(client)
                                                            .canConvertSafely(portfolioPair(pair))
                            || toValue == PortfolioTransaction.Type.DELIVERY_INBOUND
                                            && new LedgerPortfolioCompositeTypeConverter(client)
                                                            .canConvertSafely(portfolioPair(pair));

        return false;
    }

    private static boolean supportsDeliveryTypeTransition(Client client, TransactionPair<?> pair, Enum<?> fromValue,
                    Enum<?> toValue)
    {
        if (fromValue == PortfolioTransaction.Type.DELIVERY_INBOUND)
            return toValue == PortfolioTransaction.Type.DELIVERY_OUTBOUND
                            && new LedgerDeliveryDirectionConverter(client).canReverseSafely(portfolioPair(pair))
                            || toValue == PortfolioTransaction.Type.BUY
                                            && new LedgerBuySellDeliveryConverter(client)
                                                            .canConvertDeliveryToBuySellSafely(portfolioPair(pair))
                            || toValue == PortfolioTransaction.Type.SELL
                                            && new LedgerPortfolioCompositeTypeConverter(client)
                                                            .canConvertSafely(portfolioPair(pair));
        if (fromValue == PortfolioTransaction.Type.DELIVERY_OUTBOUND)
            return toValue == PortfolioTransaction.Type.DELIVERY_INBOUND
                            && new LedgerDeliveryDirectionConverter(client).canReverseSafely(portfolioPair(pair))
                            || toValue == PortfolioTransaction.Type.SELL
                                            && new LedgerBuySellDeliveryConverter(client)
                                                            .canConvertDeliveryToBuySellSafely(portfolioPair(pair))
                            || toValue == PortfolioTransaction.Type.BUY
                                            && new LedgerPortfolioCompositeTypeConverter(client)
                                                            .canConvertSafely(portfolioPair(pair));

        return false;
    }

    private static boolean supportsAccountOnlyTypeTransition(Client client, TransactionPair<?> pair, Enum<?> fromValue,
                    Enum<?> toValue)
    {
        if (fromValue == AccountTransaction.Type.DEPOSIT)
            return toValue == AccountTransaction.Type.REMOVAL
                            && new LedgerAccountTypeToggleConverter(client).canToggleSafely(accountPair(pair));
        if (fromValue == AccountTransaction.Type.REMOVAL)
            return toValue == AccountTransaction.Type.DEPOSIT
                            && new LedgerAccountTypeToggleConverter(client).canToggleSafely(accountPair(pair));
        if (fromValue == AccountTransaction.Type.INTEREST)
            return toValue == AccountTransaction.Type.INTEREST_CHARGE
                            && new LedgerAccountTypeToggleConverter(client).canToggleSafely(accountPair(pair));
        if (fromValue == AccountTransaction.Type.INTEREST_CHARGE)
            return toValue == AccountTransaction.Type.INTEREST
                            && new LedgerAccountTypeToggleConverter(client).canToggleSafely(accountPair(pair));
        if (fromValue == AccountTransaction.Type.FEES)
            return toValue == AccountTransaction.Type.FEES_REFUND
                            && new LedgerAccountTypeToggleConverter(client).canToggleSafely(accountPair(pair));
        if (fromValue == AccountTransaction.Type.FEES_REFUND)
            return toValue == AccountTransaction.Type.FEES
                            && new LedgerAccountTypeToggleConverter(client).canToggleSafely(accountPair(pair));
        if (fromValue == AccountTransaction.Type.TAXES)
            return toValue == AccountTransaction.Type.TAX_REFUND
                            && new LedgerAccountTypeToggleConverter(client).canToggleSafely(accountPair(pair));
        if (fromValue == AccountTransaction.Type.TAX_REFUND)
            return toValue == AccountTransaction.Type.TAXES
                            && new LedgerAccountTypeToggleConverter(client).canToggleSafely(accountPair(pair));

        return false;
    }

    private static boolean isLedgerBackedAccountTransfer(Client client, Transaction transaction)
    {
        return transaction instanceof AccountTransaction
                        && transaction.getCrossEntry() instanceof AccountTransferEntry entry
                        && new LedgerAccountTransferTransactionCreator(client).isLedgerBacked(entry);
    }

    private static boolean isLedgerBackedPortfolioTransfer(Client client, Transaction transaction)
    {
        return transaction instanceof PortfolioTransaction
                        && transaction.getCrossEntry() instanceof PortfolioTransferEntry entry
                        && new LedgerPortfolioTransferTransactionCreator(client).isLedgerBacked(entry);
    }

    private static boolean isLedgerBackedBuySell(Client client, Transaction transaction)
    {
        return transaction.getCrossEntry() instanceof BuySellEntry entry
                        && new LedgerBuySellTransactionCreator(client).isLedgerBacked(entry);
    }

    private static boolean isLedgerBackedDelivery(Client client, Transaction transaction)
    {
        return transaction instanceof PortfolioTransaction portfolioTransaction
                        && new LedgerDeliveryTransactionCreator(client).canUpdate(portfolioTransaction);
    }

    private static boolean isLedgerBackedAccountOnly(Client client, Transaction transaction)
    {
        return transaction instanceof AccountTransaction accountTransaction
                        && new LedgerAccountOnlyTransactionCreator(client).canUpdate(accountTransaction);
    }

    private static boolean isLedgerBackedDividend(Client client, Transaction transaction)
    {
        return transaction instanceof AccountTransaction accountTransaction
                        && new LedgerDividendTransactionCreator(client).canUpdate(accountTransaction);
    }

    private static Map<LedgerProjectionRole, Map<LedgerEntryType, Set<LedgerInlineEditingField>>> matrix()
    {
        var matrix = new EnumMap<LedgerProjectionRole, Map<LedgerEntryType, Set<LedgerInlineEditingField>>>(
                        LedgerProjectionRole.class);

        allow(matrix, LedgerProjectionRole.ACCOUNT, LedgerEntryType.DEPOSIT, metadataAndType());
        allow(matrix, LedgerProjectionRole.ACCOUNT, LedgerEntryType.REMOVAL, metadataAndType());
        allow(matrix, LedgerProjectionRole.ACCOUNT, LedgerEntryType.INTEREST, metadataAndType());
        allow(matrix, LedgerProjectionRole.ACCOUNT, LedgerEntryType.INTEREST_CHARGE, metadataAndType());
        allow(matrix, LedgerProjectionRole.ACCOUNT, LedgerEntryType.FEES, metadata());
        allow(matrix, LedgerProjectionRole.ACCOUNT, LedgerEntryType.FEES_REFUND, metadata());
        allow(matrix, LedgerProjectionRole.ACCOUNT, LedgerEntryType.TAXES, metadata());
        allow(matrix, LedgerProjectionRole.ACCOUNT, LedgerEntryType.TAX_REFUND, metadata());
        allow(matrix, LedgerProjectionRole.ACCOUNT, LedgerEntryType.DIVIDENDS,
                        EnumSet.of(LedgerInlineEditingField.DATE, LedgerInlineEditingField.NOTE,
                                        LedgerInlineEditingField.EX_DATE, LedgerInlineEditingField.SHARES));
        allow(matrix, LedgerProjectionRole.ACCOUNT, LedgerEntryType.BUY, metadataAndType());
        allow(matrix, LedgerProjectionRole.ACCOUNT, LedgerEntryType.SELL, metadataAndType());

        allow(matrix, LedgerProjectionRole.PORTFOLIO, LedgerEntryType.BUY, metadataAndType());
        allow(matrix, LedgerProjectionRole.PORTFOLIO, LedgerEntryType.SELL, metadataAndType());

        allow(matrix, LedgerProjectionRole.SOURCE_ACCOUNT, LedgerEntryType.CASH_TRANSFER, metadataAndType());
        allow(matrix, LedgerProjectionRole.TARGET_ACCOUNT, LedgerEntryType.CASH_TRANSFER, metadataAndType());

        allow(matrix, LedgerProjectionRole.SOURCE_PORTFOLIO, LedgerEntryType.SECURITY_TRANSFER, metadata());
        allow(matrix, LedgerProjectionRole.TARGET_PORTFOLIO, LedgerEntryType.SECURITY_TRANSFER, metadata());

        allow(matrix, LedgerProjectionRole.DELIVERY_INBOUND, LedgerEntryType.DELIVERY_INBOUND, metadataAndType());
        allow(matrix, LedgerProjectionRole.DELIVERY_OUTBOUND, LedgerEntryType.DELIVERY_OUTBOUND, metadataAndType());

        return matrix;
    }

    private static EnumSet<LedgerInlineEditingField> metadata()
    {
        return EnumSet.of(LedgerInlineEditingField.DATE, LedgerInlineEditingField.NOTE);
    }

    private static EnumSet<LedgerInlineEditingField> metadataAndType()
    {
        var fields = metadata();
        fields.add(LedgerInlineEditingField.TYPE);
        return fields;
    }

    private static void allow(Map<LedgerProjectionRole, Map<LedgerEntryType, Set<LedgerInlineEditingField>>> matrix,
                    LedgerProjectionRole role, LedgerEntryType type, Set<LedgerInlineEditingField> fields)
    {
        matrix.computeIfAbsent(role, ignored -> new EnumMap<>(LedgerEntryType.class)).put(type, Set.copyOf(fields));
    }
}
