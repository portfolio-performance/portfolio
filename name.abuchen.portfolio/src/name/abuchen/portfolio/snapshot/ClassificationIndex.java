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
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;

/* package */final class ClassificationIndex
{
    private ClassificationIndex()
    {}

    /* package */static PerformanceIndex calculate(final Client client, CurrencyConverter converter,
                    Classification classification, ReportingPeriod reportInterval, List<Exception> warnings)
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

        return PerformanceIndex.forClient(pseudoClient, converter, reportInterval, warnings);
    }

    private static void addSecurity(Client pseudoClient, Client client, Security security, int weight)
    {
        // if a security has no currency code, it must be an index and must not
        // have transactions after all
        if (security.getCurrencyCode() == null)
            return;

        Account pseudoAccount = new Account();
        pseudoAccount.setName(""); //$NON-NLS-1$
        pseudoAccount.setCurrencyCode(security.getCurrencyCode());
        pseudoClient.addAccount(pseudoAccount);

        Portfolio pseudoPortfolio = new Portfolio();
        pseudoPortfolio.setReferenceAccount(pseudoAccount);
        pseudoClient.addPortfolio(pseudoPortfolio);

        pseudoClient.addSecurity(security);

        for (Portfolio p : client.getPortfolios())
        {
            for (PortfolioTransaction t : p.getTransactions())
            {
                if (!security.equals(t.getSecurity()))
                    continue;

                PortfolioTransaction pseudo = new PortfolioTransaction();
                pseudo.setDate(t.getDate());
                pseudo.setCurrencyCode(t.getCurrencyCode());
                pseudo.setSecurity(security);
                pseudo.setShares(value(t.getShares(), weight));

                // convert type to the appropriate delivery type (either inbound
                // or outbound delivery)

                pseudo.setType(convertTypeToDelivery(t.getType()));

                // calculation is without taxes -> remove any taxes & adapt
                // total accordingly

                long taxes = value(t.getUnitSum(Unit.Type.TAX).getAmount(), weight);
                long amount = value(t.getAmount(), weight);

                pseudo.setAmount(pseudo.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND ? amount - taxes
                                : amount + taxes);

                // copy all units (except for taxes) over to the pseudo
                // transaction
                t.getUnits().filter(u -> u.getType() != Unit.Type.TAX).forEach(u -> pseudo.addUnit(value(u, weight)));

                pseudoPortfolio.addTransaction(pseudo);
            }
        }

        for (Account a : client.getAccounts())
        {
            for (AccountTransaction t : a.getTransactions())
            {
                if (!security.equals(t.getSecurity()))
                    continue;

                switch (t.getType())
                {
                    case DIVIDENDS:
                        long taxes = value(t.getUnitSum(Unit.Type.TAX).getAmount(), weight);
                        long amount = value(t.getAmount(), weight);

                        pseudoAccount.addTransaction(new AccountTransaction(t.getDate(), t.getCurrencyCode(),
                                        amount + taxes, t.getSecurity(), t.getType()));
                        pseudoAccount.addTransaction(new AccountTransaction(t.getDate(), t.getCurrencyCode(),
                                        amount + taxes, t.getSecurity(), AccountTransaction.Type.REMOVAL));
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
                    case INTEREST_CHARGE:
                    case TAXES:
                    case FEES:
                    case FEES_REFUND:
                        // do nothing
                        break;
                    default:
                        throw new UnsupportedOperationException();

                }
            }
        }
    }

    private static PortfolioTransaction.Type convertTypeToDelivery(PortfolioTransaction.Type type)
    {
        switch (type)
        {
            case BUY:
            case TRANSFER_IN:
            case DELIVERY_INBOUND:
                return PortfolioTransaction.Type.DELIVERY_INBOUND;
            case SELL:
            case TRANSFER_OUT:
            case DELIVERY_OUTBOUND:
                return PortfolioTransaction.Type.DELIVERY_OUTBOUND;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private static void addAccount(Client pseudoClient, Account account, int weight)
    {
        Account pseudoAccount = new Account();
        pseudoAccount.setCurrencyCode(account.getCurrencyCode());
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
                    pseudoAccount.addTransaction(new AccountTransaction(t.getDate(), t.getCurrencyCode(), amount, null,
                                    AccountTransaction.Type.DEPOSIT));
                    break;
                case BUY:
                case TRANSFER_OUT:
                    pseudoAccount.addTransaction(new AccountTransaction(t.getDate(), t.getCurrencyCode(), amount, null,
                                    AccountTransaction.Type.REMOVAL));
                    break;
                case TAX_REFUND:
                    if (t.getSecurity() != null)
                    {
                        pseudoAccount.addTransaction(new AccountTransaction(t.getDate(), t.getCurrencyCode(), amount,
                                        null, AccountTransaction.Type.DEPOSIT));
                        break;
                    }
                    // fall through if tax refund applies to account
                case DEPOSIT:
                case REMOVAL:
                case INTEREST:
                case INTEREST_CHARGE:
                case TAXES:
                case FEES:
                case FEES_REFUND:
                    if (weight != Classification.ONE_HUNDRED_PERCENT)
                        pseudoAccount.addTransaction(new AccountTransaction(t.getDate(), t.getCurrencyCode(), amount,
                                        null, t.getType()));
                    else
                        pseudoAccount.addTransaction(t);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    private static Unit value(Unit unit, int weight)
    {
        if (weight == Classification.ONE_HUNDRED_PERCENT)
            return unit;
        else
            return new Unit(unit.getType(),
                            Money.of(unit.getAmount().getCurrencyCode(), value(unit.getAmount().getAmount(), weight)));
    }

    private static long value(long value, int weight)
    {
        if (weight == Classification.ONE_HUNDRED_PERCENT)
            return value;
        else
            return Math.round(value * weight / (double) Classification.ONE_HUNDRED_PERCENT);
    }
}
