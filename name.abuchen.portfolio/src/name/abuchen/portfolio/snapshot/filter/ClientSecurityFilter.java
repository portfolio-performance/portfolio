package name.abuchen.portfolio.snapshot.filter;

import java.util.Arrays;
import java.util.List;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionPair;

public class ClientSecurityFilter implements ClientFilter
{
    private final List<Security> securities;

    public ClientSecurityFilter(Security... securities)
    {
        this.securities = Arrays.asList(securities);
    }

    @Override
    public Client filter(Client client)
    {
        ReadOnlyClient pseudoClient = new ReadOnlyClient(client);

        ReadOnlyAccount pseudoAccount = new ReadOnlyAccount(client.getAccounts().get(0));
        pseudoClient.internalAddAccount(pseudoAccount);

        ReadOnlyPortfolio pseudoPortfolio = new ReadOnlyPortfolio(client.getPortfolios().get(0));
        pseudoPortfolio.setReferenceAccount(pseudoAccount);
        pseudoClient.internalAddPortfolio(pseudoPortfolio);

        for (Security security : securities)
        {
            pseudoClient.internalAddSecurity(security);
            addSecurity(client, pseudoPortfolio, pseudoAccount, security);
        }

        return pseudoClient;
    }

    private void addSecurity(Client client, ReadOnlyPortfolio portfolio, ReadOnlyAccount account, Security security)
    {
        List<TransactionPair<?>> transactions = security.getTransactions(client);

        for (TransactionPair<?> pair : transactions)
        {
            if (pair.getTransaction() instanceof PortfolioTransaction)
                addPortfolioTransaction(portfolio, (PortfolioTransaction) pair.getTransaction());
            else if (pair.getTransaction() instanceof AccountTransaction)
                addAccountTransaction(account, (AccountTransaction) pair.getTransaction());
        }

    }

    private void addPortfolioTransaction(ReadOnlyPortfolio portfolio, PortfolioTransaction t)
    {
        switch (t.getType())
        {
            case BUY:
            case DELIVERY_INBOUND:
                portfolio.internalAddTransaction(convertToDelivery(t, PortfolioTransaction.Type.DELIVERY_INBOUND));
                break;
            case SELL:
            case DELIVERY_OUTBOUND:
                portfolio.internalAddTransaction(convertToDelivery(t, PortfolioTransaction.Type.DELIVERY_OUTBOUND));
                break;
            case TRANSFER_IN:
            case TRANSFER_OUT:
                // ignore: transfers are internal to the client file
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    private void addAccountTransaction(ReadOnlyAccount account, AccountTransaction t)
    {
        switch (t.getType())
        {
            case DIVIDENDS:
                long taxes = t.getUnitSum(Unit.Type.TAX).getAmount();
                long amount = t.getAmount();

                account.internalAddTransaction(new AccountTransaction(t.getDate(), t.getCurrencyCode(), amount + taxes,
                                t.getSecurity(), t.getType()));
                account.internalAddTransaction(new AccountTransaction(t.getDate(), t.getCurrencyCode(), amount + taxes,
                                t.getSecurity(), AccountTransaction.Type.REMOVAL));
                break;
            case TAX_REFUND:
                // ignore taxes
                break;
            case BUY:
            case SELL:
            case DEPOSIT:
            case REMOVAL:
            case TRANSFER_IN:
            case TRANSFER_OUT:
            case FEES:
            case FEES_REFUND:
            case TAXES:
            case INTEREST:
            case INTEREST_CHARGE:
                throw new IllegalArgumentException();
        }
    }

    private PortfolioTransaction convertToDelivery(PortfolioTransaction t, PortfolioTransaction.Type targetType)
    {
        PortfolioTransaction pseudo = new PortfolioTransaction();
        pseudo.setDate(t.getDate());
        pseudo.setCurrencyCode(t.getCurrencyCode());
        pseudo.setSecurity(t.getSecurity());
        pseudo.setShares(t.getShares());
        pseudo.setType(targetType);

        // calculation is without taxes -> remove any taxes & adapt
        // total accordingly

        long taxes = t.getUnitSum(Unit.Type.TAX).getAmount();
        long amount = t.getAmount();

        pseudo.setAmount(pseudo.getType() == PortfolioTransaction.Type.DELIVERY_INBOUND ? amount - taxes
                        : amount + taxes);

        // copy all units (except for taxes) over to the pseudo
        // transaction
        t.getUnits().filter(u -> u.getType() != Unit.Type.TAX).forEach(pseudo::addUnit);

        return pseudo;
    }
}
