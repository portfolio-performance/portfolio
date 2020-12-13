package name.abuchen.portfolio.snapshot.filter;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;

/* protected */ class ClientFilterHelper
{
    private ClientFilterHelper()
    {
    }

    /* package */ static void recreateTransfer(PortfolioTransferEntry transferEntry, ReadOnlyPortfolio sourcePortfolio,
                    ReadOnlyPortfolio targetPortfolio)
    {
        recreateTransfer(transferEntry, sourcePortfolio, targetPortfolio, Classification.ONE_HUNDRED_PERCENT);
    }

    /* package */ static void recreateTransfer(PortfolioTransferEntry transferEntry, ReadOnlyPortfolio sourcePortfolio,
                    ReadOnlyPortfolio targetPortfolio, int weight)
    {
        PortfolioTransaction t = transferEntry.getSourceTransaction();

        PortfolioTransferEntry copy = new PortfolioTransferEntry(sourcePortfolio, targetPortfolio);
        copy.setDate(t.getDateTime());
        copy.setCurrencyCode(t.getCurrencyCode());
        copy.setSecurity(t.getSecurity());
        copy.setNote(t.getNote());
        copy.setShares(value(t.getShares(), weight));
        copy.setAmount(value(t.getAmount(), weight));

        sourcePortfolio.internalAddTransaction(copy.getSourceTransaction());
        targetPortfolio.internalAddTransaction(copy.getTargetTransaction());
    }

    /* package */ static void recreateTransfer(AccountTransferEntry transferEntry, ReadOnlyAccount sourceAccount,
                    ReadOnlyAccount targetAccount)
    {
        AccountTransaction t = transferEntry.getSourceTransaction();

        AccountTransferEntry copy = new AccountTransferEntry(sourceAccount, targetAccount);

        copy.setDate(t.getDateTime());
        copy.setNote(t.getNote());

        copy.getSourceTransaction().setCurrencyCode(t.getCurrencyCode());
        copy.getSourceTransaction().setAmount(t.getAmount());
        copy.getSourceTransaction().addUnits(t.getUnits());

        AccountTransaction tt = transferEntry.getTargetTransaction();
        copy.getTargetTransaction().setCurrencyCode(tt.getCurrencyCode());
        copy.getTargetTransaction().setAmount(tt.getAmount());

        sourceAccount.internalAddTransaction(copy.getSourceTransaction());
        targetAccount.internalAddTransaction(copy.getTargetTransaction());
    }

    private static long value(long value, int weight)
    {
        if (weight == Classification.ONE_HUNDRED_PERCENT)
            return value;
        else
            return Math.round(value * weight / (double) Classification.ONE_HUNDRED_PERCENT);
    }
}
