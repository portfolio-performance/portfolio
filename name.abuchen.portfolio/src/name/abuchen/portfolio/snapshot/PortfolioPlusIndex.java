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

/* package */final class PortfolioPlusIndex
{
    private PortfolioPlusIndex()
    {}

    /* package */static PerformanceIndex calculate(Client client, Portfolio portfolio, ReportingPeriod reportInterval,
                    List<Exception> warnings)
    {
        Client pseudoClient = new Client();

        Account pseudoAccount = new Account();
        pseudoAccount.setName(""); //$NON-NLS-1$
        pseudoClient.addAccount(pseudoAccount);

        Portfolio pseudoPortfolio = new Portfolio();
        pseudoPortfolio.setReferenceAccount(pseudoAccount);
        pseudoClient.addPortfolio(pseudoPortfolio);

        adaptPortfolioTransactions(portfolio, pseudoClient, pseudoPortfolio);
        adaptAccountTransactions(portfolio, pseudoAccount);

        return PerformanceIndex.forClient(pseudoClient, reportInterval, warnings);
    }

    private static void adaptPortfolioTransactions(Portfolio portfolio, Client pseudoClient, Portfolio pseudoPortfolio)
    {
        Set<Security> securities = new HashSet<Security>();

        for (PortfolioTransaction t : portfolio.getTransactions())
        {
            securities.add(t.getSecurity());

            switch (t.getType())
            {
                case BUY:
                case TRANSFER_IN:
                    if (t.getCrossEntry().getCrossEntity(t).equals(portfolio.getReferenceAccount()))
                    {
                        pseudoPortfolio.addTransaction(t);
                    }
                    else
                    {
                        pseudoPortfolio.addTransaction(new PortfolioTransaction(t.getDate(), t.getSecurity(),
                                        PortfolioTransaction.Type.DELIVERY_INBOUND, t.getShares(), t.getAmount(), t
                                                        .getFees(), t.getTaxes()));
                    }
                    break;
                case SELL:
                case TRANSFER_OUT:
                    if (t.getCrossEntry().getCrossEntity(t).equals(portfolio.getReferenceAccount()))
                    {
                        pseudoPortfolio.addTransaction(t);
                    }
                    else
                    {
                        pseudoPortfolio.addTransaction(new PortfolioTransaction(t.getDate(), t.getSecurity(),
                                        PortfolioTransaction.Type.DELIVERY_OUTBOUND, t.getShares(), t.getAmount(), t
                                                        .getFees(), t.getTaxes()));
                    }
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

    private static void adaptAccountTransactions(Portfolio portfolio, Account pseudoAccount)
    {
        if (portfolio.getReferenceAccount() == null)
            return;

        for (AccountTransaction t : portfolio.getReferenceAccount().getTransactions())
        {
            switch (t.getType())
            {
                case BUY:
                    if (t.getCrossEntry().getCrossEntity(t).equals(portfolio))
                    {
                        pseudoAccount.addTransaction(t);
                    }
                    else
                    {
                        pseudoAccount.addTransaction(new AccountTransaction(t.getDate(), t.getSecurity(),
                                        AccountTransaction.Type.REMOVAL, t.getAmount()));
                    }
                    break;
                case SELL:
                    if (t.getCrossEntry().getCrossEntity(t).equals(portfolio))
                    {
                        pseudoAccount.addTransaction(t);
                    }
                    else
                    {
                        pseudoAccount.addTransaction(new AccountTransaction(t.getDate(), t.getSecurity(),
                                        AccountTransaction.Type.DEPOSIT, t.getAmount()));
                    }
                    break;
                case TRANSFER_IN:
                    pseudoAccount.addTransaction(new AccountTransaction(t.getDate(), t.getSecurity(),
                                    AccountTransaction.Type.DEPOSIT, t.getAmount()));
                    break;
                case TRANSFER_OUT:
                    pseudoAccount.addTransaction(new AccountTransaction(t.getDate(), t.getSecurity(),
                                    AccountTransaction.Type.REMOVAL, t.getAmount()));
                    break;
                case DIVIDENDS:
                case DEPOSIT:
                case REMOVAL:
                case INTEREST:
                case TAXES:
                case FEES:
                    pseudoAccount.addTransaction(t);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }
}
