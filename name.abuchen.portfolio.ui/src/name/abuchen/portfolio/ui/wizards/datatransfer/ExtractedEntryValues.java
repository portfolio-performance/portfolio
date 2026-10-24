package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.util.List;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Annotated;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

/**
 * Values of extracted entries shown in the import wizard: the taxes, fees and
 * note of an item, and which import options apply to a list of entries. The
 * class has no user interface.
 */
final class ExtractedEntryValues
{
    private ExtractedEntryValues()
    {
    }

    /** Returns the taxes of the transaction of the item, or null if there are none. */
    static Money getTaxes(Item item)
    {
        return getUnitSum(item, Unit.Type.TAX);
    }

    /** Returns the fees of the transaction of the item, or null if there are none. */
    static Money getFees(Item item)
    {
        return getUnitSum(item, Unit.Type.FEE);
    }

    /** Returns the note of the item, or null. */
    static String getNote(Item item)
    {
        return item.getSubject() instanceof Annotated annotated ? annotated.getNote() : null;
    }

    /** Returns true if the entries contain a purchase or sale. */
    static boolean hasBuySell(List<ExtractedEntry> entries)
    {
        return entries.stream().anyMatch(e -> e.getItem().getSubject() instanceof BuySellEntry);
    }

    /** Returns true if the entries contain a dividend. */
    static boolean hasDividends(List<ExtractedEntry> entries)
    {
        return entries.stream().anyMatch(e -> e.getItem().getSubject() instanceof AccountTransaction transaction
                        && transaction.getType() == AccountTransaction.Type.DIVIDENDS);
    }

    /**
     * Returns the taxes or fees of the transaction of the item, or null if
     * there are none.
     */
    private static Money getUnitSum(Item item, Unit.Type type)
    {
        var subject = item.getSubject();

        var transaction = switch (subject)
        {
            case BuySellEntry entry -> entry.getPortfolioTransaction();
            case AccountTransferEntry entry -> entry.getSourceTransaction();
            case PortfolioTransferEntry entry -> entry.getSourceTransaction();
            case Transaction t -> t;
            default -> null;
        };

        if (transaction == null)
            return null;

        var sum = transaction.getUnitSum(type);
        return sum.isZero() ? null : sum;
    }
}
