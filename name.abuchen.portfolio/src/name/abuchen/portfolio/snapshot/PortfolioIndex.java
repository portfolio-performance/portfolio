package name.abuchen.portfolio.snapshot;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;

/* package */final class PortfolioIndex
{
    private PortfolioIndex()
    {}

    /* package */static PerformanceIndex calculate(Client client, CurrencyConverter converter, Portfolio portfolio,
                    ReportingPeriod reportInterval, List<Exception> warnings)
    {
        Client pseudoClient = new Client();

        Account pseudoAccount = new Account();
        pseudoAccount.setName(""); //$NON-NLS-1$
        pseudoClient.addAccount(pseudoAccount);

        Portfolio pseudoPortfolio = new Portfolio();
        pseudoPortfolio.setReferenceAccount(pseudoAccount);
        pseudoClient.addPortfolio(pseudoPortfolio);

        adaptPortfolioTransactions(portfolio, pseudoClient, pseudoPortfolio);
        collectDividends(portfolio, pseudoClient, pseudoAccount);

        return PerformanceIndex.forClient(pseudoClient, converter, reportInterval, warnings);
    }

    private static void adaptPortfolioTransactions(Portfolio portfolio, Client pseudoClient, Portfolio pseudoPortfolio)
    {
        Set<Security> securities = new HashSet<Security>();

        for (PortfolioTransaction t : portfolio.getTransactions())
        {
            securities.add(t.getSecurity());

            PortfolioTransaction clone = new PortfolioTransaction();
            switch (t.getType())
            {
                case BUY:
                case TRANSFER_IN:
                    clone.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                    clone.setDate(t.getDate());
                    clone.setCurrencyCode(t.getCurrencyCode());
                    clone.setSecurity(t.getSecurity());
                    clone.setAmount(t.getAmount());
                    clone.setShares(t.getShares());
                    clone.addUnits(t.getUnits());
                    pseudoPortfolio.addTransaction(clone);
                    break;
                case SELL:
                case TRANSFER_OUT:
                    clone.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                    clone.setDate(t.getDate());
                    clone.setCurrencyCode(t.getCurrencyCode());
                    clone.setSecurity(t.getSecurity());
                    clone.setAmount(t.getAmount());
                    clone.setShares(t.getShares());
                    clone.addUnits(t.getUnits());
                    pseudoPortfolio.addTransaction(clone);
                    break;
                case DELIVERY_INBOUND:
                case DELIVERY_OUTBOUND:
                    pseudoPortfolio.addTransaction(t);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        for (Security security : securities)
            pseudoClient.addSecurity(security);
    }

    private static void collectDividends(Portfolio portfolio, Client pseudoClient, Account pseudoAccount)
    {
        if (portfolio.getReferenceAccount() == null)
            return;

        for (AccountTransaction t : portfolio.getReferenceAccount().getTransactions())
        {
            if (t.getSecurity() == null)
                continue;

            if (!pseudoClient.getSecurities().contains(t.getSecurity()))
                continue;

            switch (t.getType())
            {
                case TAX_REFUND:
                    // security must be non-null -> tax refund is relevant for
                    // performance of security
                case DIVIDENDS:
                    pseudoAccount.addTransaction(t);
                    pseudoAccount.addTransaction(new AccountTransaction(t.getDate(), t.getCurrencyCode(),
                                    t.getAmount(), null, AccountTransaction.Type.REMOVAL));
                    break;
                case BUY:
                case TRANSFER_IN:
                case SELL:
                case TRANSFER_OUT:
                case DEPOSIT:
                case REMOVAL:
                case INTEREST:
                case TAXES:
                case FEES:
                    // do nothing
                    break;
                default:
                    throw new UnsupportedOperationException();

            }
        }
    }
}
