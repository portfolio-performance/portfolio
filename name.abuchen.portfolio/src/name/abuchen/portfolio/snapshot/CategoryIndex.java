package name.abuchen.portfolio.snapshot;

import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Category;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

public class CategoryIndex extends PerformanceIndex
{
    public static CategoryIndex forPeriod(Client client, Category category, ReportingPeriod reportInterval,
                    List<Exception> warnings)
    {
        CategoryIndex index = new CategoryIndex(client, category, reportInterval);
        index.calculate(warnings);
        return index;
    }

    private Category category;

    private CategoryIndex(Client client, Category category, ReportingPeriod reportInterval)
    {
        super(client, reportInterval);
        this.category = category;
    }

    private void calculate(List<Exception> warnings)
    {
        Client pseudoClient = new Client();

        for (Object object : category.getTreeElements())
        {
            if (object instanceof Security)
            {
                addSecurity(pseudoClient, (Security) object);
            }
            else if (object instanceof Account)
            {
                addAccount(pseudoClient, (Account) object);
            }
        }

        ClientIndex clientIndex = ClientIndex.forPeriod(pseudoClient, getReportInterval(), warnings);

        dates = clientIndex.getDates();
        totals = clientIndex.getTotals();
        accumulated = clientIndex.getAccumulatedPercentage();
        delta = clientIndex.getDeltaPercentage();
        transferals = clientIndex.getTransferals();
    }

    private void addSecurity(Client pseudoClient, Security security)
    {
        Account pseudoAccount = new Account();
        pseudoClient.addAccount(pseudoAccount);

        Portfolio pseudoPortfolio = new Portfolio();
        pseudoPortfolio.setReferenceAccount(pseudoAccount);
        pseudoClient.addPortfolio(pseudoPortfolio);

        pseudoClient.addSecurity(security);

        for (Portfolio p : getClient().getPortfolios())
        {
            for (PortfolioTransaction t : p.getTransactions())
            {
                if (t.getSecurity().equals(security))
                {
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
                                            PortfolioTransaction.Type.DELIVERY_OUTBOUND, t.getShares(), t.getAmount(),
                                            t.getFees()));
                            break;
                        case DELIVERY_INBOUND:
                        case DELIVERY_OUTBOUND:
                            pseudoPortfolio.addTransaction(t);
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }

                }
            }
        }

        for (Account a : getClient().getAccounts())
        {
            for (AccountTransaction t : a.getTransactions())
            {
                if (security.equals(t.getSecurity()))
                {
                    switch (t.getType())
                    {
                        case DIVIDENDS:
                            pseudoAccount.addTransaction(t);
                            pseudoAccount.addTransaction(new AccountTransaction(t.getDate(), t.getSecurity(),
                                            AccountTransaction.Type.REMOVAL, t.getAmount()));
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
    }

    private void addAccount(Client pseudoClient, Account account)
    {
        Account pseudoAccount = new Account();
        pseudoClient.addAccount(pseudoAccount);

        for (AccountTransaction t : account.getTransactions())
        {
            switch (t.getType())
            {
                case BUY:
                case TRANSFER_IN:
                    pseudoAccount.addTransaction(new AccountTransaction(t.getDate(), t.getSecurity(),
                                    AccountTransaction.Type.DEPOSIT, t.getAmount()));
                    break;
                case SELL:
                case TRANSFER_OUT:
                    pseudoAccount.addTransaction(new AccountTransaction(t.getDate(), t.getSecurity(),
                                    AccountTransaction.Type.REMOVAL, t.getAmount()));
                    break;
                case DIVIDENDS:
                    pseudoAccount.addTransaction(new AccountTransaction(t.getDate(), t.getSecurity(),
                                    AccountTransaction.Type.DEPOSIT, t.getAmount()));
                    break;
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
