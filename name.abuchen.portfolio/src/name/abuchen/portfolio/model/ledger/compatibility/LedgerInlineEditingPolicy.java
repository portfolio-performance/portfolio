package name.abuchen.portfolio.model.ledger.compatibility;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import name.abuchen.portfolio.model.Transaction;
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
                        ledgerBackedTransaction.getLedgerProjectionRef().getRole(), field);
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

    private static Transaction transaction(Object element)
    {
        if (element instanceof TransactionPair<?> pair)
            return pair.getTransaction();

        if (element instanceof Transaction transaction)
            return transaction;

        return null;
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
