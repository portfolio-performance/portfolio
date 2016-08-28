package name.abuchen.portfolio.snapshot.filter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

/**
 * Filters the Client to include only transactions related to the given
 * portfolio. Dividend transactions are included together with a corresponding
 * REMOVAL transaction.
 */
public class PortfolioClientFilter implements ClientFilter
{
    private final Portfolio portfolio;

    public PortfolioClientFilter(Portfolio portfolio)
    {
        this.portfolio = Objects.requireNonNull(portfolio);
    }

    @Override
    public Client filter(Client client)
    {
        ReadOnlyClient pseudoClient = new ReadOnlyClient(client);

        ReadOnlyAccount pseudoAccount = new ReadOnlyAccount(portfolio.getReferenceAccount(), ""); //$NON-NLS-1$
        pseudoClient.internalAddAccount(pseudoAccount);

        ReadOnlyPortfolio pseudoPortfolio = new ReadOnlyPortfolio(portfolio);
        pseudoPortfolio.setReferenceAccount(pseudoAccount);
        pseudoClient.internalAddPortfolio(pseudoPortfolio);

        adaptPortfolioTransactions(portfolio, pseudoClient, pseudoPortfolio);
        collectDividends(portfolio, pseudoClient, pseudoAccount);

        return pseudoClient;
    }

    private void adaptPortfolioTransactions(Portfolio portfolio, ReadOnlyClient pseudoClient,
                    ReadOnlyPortfolio pseudoPortfolio)
    {
        Set<Security> securities = new HashSet<>();

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
                    pseudoPortfolio.internalAddTransaction(clone);
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
                    pseudoPortfolio.internalAddTransaction(clone);
                    break;
                case DELIVERY_INBOUND:
                case DELIVERY_OUTBOUND:
                    pseudoPortfolio.internalAddTransaction(t);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        for (Security security : securities)
            pseudoClient.internalAddSecurity(security);
    }

    private void collectDividends(Portfolio portfolio, ReadOnlyClient pseudoClient, ReadOnlyAccount pseudoAccount)
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
                    pseudoAccount.internalAddTransaction(t);
                    pseudoAccount.internalAddTransaction(new AccountTransaction(t.getDate(), t.getCurrencyCode(),
                                    t.getAmount(), null, AccountTransaction.Type.REMOVAL));
                    break;
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
                    // do nothing
                    break;
                default:
                    throw new UnsupportedOperationException();

            }
        }
    }
}
