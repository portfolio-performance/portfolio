package name.abuchen.portfolio.datatransfer.actions;

import name.abuchen.portfolio.datatransfer.Extractor.NonImportableBuySellEntryItem;
import name.abuchen.portfolio.datatransfer.Extractor.NonImportableTransactionItem;
import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.datatransfer.ImportAction.Status.Code;

public class MarkNonImportableAction implements ImportAction
{
    @Override
    public Status process(NonImportableTransactionItem item)
    {
        return new Status(Code.ERROR, item.getNote());
    }

    @Override
    public Status process(NonImportableBuySellEntryItem item)
    {
        return new Status(Code.ERROR, item.getNote());
    }
}
