package name.abuchen.portfolio.datatransfer;

import java.util.List;

import name.abuchen.portfolio.datatransfer.Extractor.AccountTransferItem;
import name.abuchen.portfolio.datatransfer.Extractor.BuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.Extractor.TransactionItem;

public class ExtractorTestUtilities
{

    public static long countBuySell(List<Extractor.Item> items)
    {
        return items.stream().filter(BuySellEntryItem.class::isInstance).count();
    }

    public static long countAccountTransactions(List<Extractor.Item> items)
    {
        return items.stream().filter(TransactionItem.class::isInstance).count();
    }

    public static long countAccountTransfers(List<Extractor.Item> items)
    {
        return items.stream().filter(AccountTransferItem.class::isInstance).count();
    }

    public static long countSecurities(List<Extractor.Item> items)
    {
        return items.stream().filter(SecurityItem.class::isInstance).count();
    }

    public static long countItemsWithFailureMessage(List<Extractor.Item> items)
    {
        return items.stream().filter(Extractor.Item::isFailure).count();
    }

    public static long countSkippedItems(List<Extractor.Item> items)
    {
        return items.stream().filter(Extractor.Item::isSkipped).count();
    }
}