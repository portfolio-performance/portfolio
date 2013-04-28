package name.abuchen.portfolio.snapshot;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

public class PortfolioIndex extends PerformanceIndex
{
    public static PortfolioIndex forPeriod(Client client, Portfolio portfolio, ReportingPeriod reportInterval,
                    List<Exception> warnings)
    {
        PortfolioIndex index = new PortfolioIndex(client, portfolio, reportInterval);
        index.calculate(warnings);
        return index;
    }

    private Portfolio portfolio;

    private PortfolioIndex(Client client, Portfolio portfolio, ReportingPeriod reportInterval)
    {
        super(client, reportInterval);
        this.portfolio = portfolio;
    }

    private void calculate(List<Exception> warnings)
    {
        Client pseudoClient = new Client();

        Account pseudoAccount = new Account();
        pseudoAccount.setName(""); //$NON-NLS-1$
        pseudoClient.addAccount(pseudoAccount);

        Portfolio pseudoPortfolio = new Portfolio();
        pseudoPortfolio.setReferenceAccount(pseudoAccount);
        pseudoClient.addPortfolio(pseudoPortfolio);

        Set<Security> securities = new HashSet<Security>();

        for (PortfolioTransaction t : portfolio.getTransactions())
        {
            securities.add(t.getSecurity());

            switch (t.getType())
            {
                case BUY:
                case TRANSFER_IN:
                {
                    pseudoPortfolio.addTransaction(new PortfolioTransaction(t.getDate(), t.getSecurity(),
                                    PortfolioTransaction.Type.DELIVERY_INBOUND, t.getShares(), t.getAmount(), t
                                                    .getFees()));
                    break;
                }
                case SELL:
                case TRANSFER_OUT:
                    pseudoPortfolio.addTransaction(new PortfolioTransaction(t.getDate(), t.getSecurity(),
                                    PortfolioTransaction.Type.DELIVERY_OUTBOUND, t.getShares(), t.getAmount(), t
                                                    .getFees()));
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

        ClientIndex clientIndex = ClientIndex.forPeriod(pseudoClient, getReportInterval(), warnings);

        dates = clientIndex.getDates();
        totals = clientIndex.getTotals();
        accumulated = clientIndex.getAccumulatedPercentage();
        delta = clientIndex.getDeltaPercentage();
        transferals = clientIndex.getTransferals();
    }

}
