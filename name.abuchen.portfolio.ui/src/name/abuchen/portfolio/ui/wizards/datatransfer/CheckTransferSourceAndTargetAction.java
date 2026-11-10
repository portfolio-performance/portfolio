package name.abuchen.portfolio.ui.wizards.datatransfer;

import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.datatransfer.ImportAction.Status.Code;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.ui.Messages;

/**
 * Marks transfers whose source and target are the same account or
 * portfolio, e.g. because the same account is selected as account and as
 * offset account. Such a transfer is not imported. The accounts and
 * portfolios are resolved exactly as for the import.
 */
final class CheckTransferSourceAndTargetAction implements ImportAction
{
    @Override
    public Status process(AccountTransferEntry entry, Account source, Account target)
    {
        if (source != null && source.equals(target))
            return new Status(Code.ERROR, Messages.MsgAccountMustBeDifferent);

        return Status.OK_STATUS;
    }

    @Override
    public Status process(PortfolioTransferEntry entry, Portfolio source, Portfolio target)
    {
        if (source != null && source.equals(target))
            return new Status(Code.ERROR, Messages.MsgPortfolioMustBeDifferent);

        return Status.OK_STATUS;
    }
}
