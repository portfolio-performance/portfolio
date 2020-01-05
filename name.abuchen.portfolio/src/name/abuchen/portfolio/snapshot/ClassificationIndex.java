package name.abuchen.portfolio.snapshot;

import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy.Visitor;

/* package */final class ClassificationIndex
{
    private ClassificationIndex()
    {}

    /* package */static PerformanceIndex calculate(final Client client, Classification classification,
                    ReportingPeriod reportInterval, List<Exception> warnings)
    {
        final Client pseudoClient = new Client();

        classification.accept(new Visitor()
        {
            @Override
            public void visit(Classification classification, Assignment assignment)
            {
                InvestmentVehicle vehicle = assignment.getInvestmentVehicle();

                if (vehicle instanceof Security)
                    addSecurity(pseudoClient, client, (Security) vehicle, assignment.getWeight());
                else if (vehicle instanceof Account)
                    addAccount(pseudoClient, (Account) vehicle, assignment.getWeight());
            }
        });

        return PerformanceIndex.forClient(pseudoClient, reportInterval, warnings);
    }

    private static void addSecurity(Client pseudoClient, Client client, Security security, int weight)
    {
        Account pseudoAccount = new Account();
        pseudoAccount.setName(""); //$NON-NLS-1$
        pseudoClient.addAccount(pseudoAccount);

        Portfolio pseudoPortfolio = new Portfolio();
        pseudoPortfolio.setReferenceAccount(pseudoAccount);
        pseudoClient.addPortfolio(pseudoPortfolio);

        pseudoClient.addSecurity(security);

        for (Portfolio p : client.getPortfolios())
        {
            for (PortfolioTransaction t : p.getTransactions())
            {
                if (t.getSecurity().equals(security))
                {
                    long shares = value(t.getShares(), weight);
                    long amount = value(t.getAmount(), weight);
                    long fees = value(t.getFees(), weight);
                    long taxes = value(t.getTaxes(), weight);

                    switch (t.getType())
                    {
                        case BUY:
                        case TRANSFER_IN:
                        case DELIVERY_INBOUND:
                        {
                            pseudoPortfolio.addTransaction(new PortfolioTransaction(t.getDate(), t.getSecurity(),
                                            PortfolioTransaction.Type.DELIVERY_INBOUND, shares, amount - taxes, fees, 0));
                            break;
                        }
                        case SELL:
                        case TRANSFER_OUT:
                        case DELIVERY_OUTBOUND:
                            pseudoPortfolio.addTransaction(new PortfolioTransaction(t.getDate(), t.getSecurity(),
                                            PortfolioTransaction.Type.DELIVERY_OUTBOUND, shares, amount + taxes, fees,
                                            0));
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                }
            }
        }

        for (Account a : client.getAccounts())
        {
            for (AccountTransaction t : a.getTransactions())
            {
                if (security.equals(t.getSecurity()))
                {
                    switch (t.getType())
                    {
                        case DIVIDENDS:
                            long amount = value(t.getAmount(), weight);
                            pseudoAccount.addTransaction(new AccountTransaction(t.getDate(), t.getSecurity(), //
                                            t.getType(), amount));
                            pseudoAccount.addTransaction(new AccountTransaction(t.getDate(), t.getSecurity(),
                                            AccountTransaction.Type.REMOVAL, amount));
                            break;
                        case TAX_REFUND:
                            // ignore taxes when calculating performance of
                            // securities
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

    private static void addAccount(Client pseudoClient, Account account, int weight)
    {
        Account pseudoAccount = new Account();
        pseudoAccount.setName(account.getName());
        pseudoClient.addAccount(pseudoAccount);

        for (AccountTransaction t : account.getTransactions())
        {
            long amount = value(t.getAmount(), weight);
            switch (t.getType())
            {
                case SELL:
                case TRANSFER_IN:
                case DIVIDENDS:
                    pseudoAccount.addTransaction(new AccountTransaction(t.getDate(), null,
                                    AccountTransaction.Type.DEPOSIT, amount));
                    break;
                case BUY:
                case TRANSFER_OUT:
                    pseudoAccount.addTransaction(new AccountTransaction(t.getDate(), null,
                                    AccountTransaction.Type.REMOVAL, amount));
                    break;
                case TAX_REFUND:
                    if (t.getSecurity() != null)
                    {
                        pseudoAccount.addTransaction(new AccountTransaction(t.getDate(), null,
                                        AccountTransaction.Type.DEPOSIT, amount));
                        break;
                    }
                    // fall through if tax refund applies to account
                case DEPOSIT:
                case REMOVAL:
                case INTEREST:
                case TAXES:
                case FEES:
                    if (weight != Classification.ONE_HUNDRED_PERCENT)
                        pseudoAccount.addTransaction(new AccountTransaction(t.getDate(), null, t.getType(), amount));
                    else
                        pseudoAccount.addTransaction(t);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    private static long value(long value, int weight)
    {
        if (weight == Classification.ONE_HUNDRED_PERCENT)
            return value;
        else
            return Math.round(value * weight / (double) Classification.ONE_HUNDRED_PERCENT);
    }
}
