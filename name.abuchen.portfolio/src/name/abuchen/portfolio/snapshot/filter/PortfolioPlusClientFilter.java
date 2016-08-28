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
 * portfolio and the reference account of the portfolio.
 */
public class PortfolioPlusClientFilter implements ClientFilter
{
    private final Portfolio portfolio;

    public PortfolioPlusClientFilter(Portfolio portfolio)
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
        adaptAccountTransactions(portfolio, pseudoClient, pseudoAccount);

        return pseudoClient;
    }

    private void adaptPortfolioTransactions(Portfolio portfolio, ReadOnlyClient pseudoClient,
                    ReadOnlyPortfolio pseudoPortfolio)
    {
        Set<Security> securities = new HashSet<>();

        for (PortfolioTransaction t : portfolio.getTransactions())
        {
            securities.add(t.getSecurity());

            switch (t.getType())
            {
                case BUY:
                case TRANSFER_IN:
                    if (t.getCrossEntry().getCrossOwner(t).equals(portfolio.getReferenceAccount()))
                    {
                        pseudoPortfolio.internalAddTransaction(t);
                    }
                    else
                    {
                        PortfolioTransaction clone = new PortfolioTransaction();
                        clone.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                        clone.setDate(t.getDate());
                        clone.setCurrencyCode(t.getCurrencyCode());
                        clone.setSecurity(t.getSecurity());
                        clone.setAmount(t.getAmount());
                        clone.setShares(t.getShares());
                        clone.addUnits(t.getUnits());
                        pseudoPortfolio.internalAddTransaction(clone);
                    }
                    break;
                case SELL:
                case TRANSFER_OUT:
                    if (t.getCrossEntry().getCrossOwner(t).equals(portfolio.getReferenceAccount()))
                    {
                        pseudoPortfolio.internalAddTransaction(t);
                    }
                    else
                    {
                        PortfolioTransaction clone = new PortfolioTransaction();
                        clone.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                        clone.setDate(t.getDate());
                        clone.setCurrencyCode(t.getCurrencyCode());
                        clone.setSecurity(t.getSecurity());
                        clone.setAmount(t.getAmount());
                        clone.setShares(t.getShares());
                        clone.addUnits(t.getUnits());
                        pseudoPortfolio.internalAddTransaction(clone);
                    }
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

    private void adaptAccountTransactions(Portfolio portfolio, ReadOnlyClient pseudoClient,
                    ReadOnlyAccount pseudoAccount)
    {
        if (portfolio.getReferenceAccount() == null)
            return;

        for (AccountTransaction t : portfolio.getReferenceAccount().getTransactions())
        {
            switch (t.getType())
            {
                case BUY:
                    if (t.getCrossEntry().getCrossOwner(t).equals(portfolio))
                    {
                        pseudoAccount.internalAddTransaction(t);
                    }
                    else
                    {
                        pseudoAccount.internalAddTransaction(new AccountTransaction(t.getDate(), t.getCurrencyCode(),
                                        t.getAmount(), null, AccountTransaction.Type.REMOVAL));
                    }
                    break;
                case SELL:
                    if (t.getCrossEntry().getCrossOwner(t).equals(portfolio))
                    {
                        pseudoAccount.internalAddTransaction(t);
                    }
                    else
                    {
                        pseudoAccount.internalAddTransaction(new AccountTransaction(t.getDate(), t.getCurrencyCode(),
                                        t.getAmount(), null, AccountTransaction.Type.DEPOSIT));
                    }
                    break;
                case TRANSFER_IN:
                    pseudoAccount.internalAddTransaction(new AccountTransaction(t.getDate(), t.getCurrencyCode(),
                                    t.getAmount(), null, AccountTransaction.Type.DEPOSIT));
                    break;
                case TRANSFER_OUT:
                    pseudoAccount.internalAddTransaction(new AccountTransaction(t.getDate(), t.getCurrencyCode(),
                                    t.getAmount(), null, AccountTransaction.Type.REMOVAL));
                    break;
                case DIVIDENDS:
                case TAX_REFUND:
                    if (t.getSecurity() == null || pseudoClient.getSecurities().contains(t.getSecurity()))
                    {
                        pseudoAccount.internalAddTransaction(t);
                    }
                    else
                    {
                        pseudoAccount.internalAddTransaction(new AccountTransaction(t.getDate(), t.getCurrencyCode(),
                                        t.getAmount(), null, AccountTransaction.Type.DEPOSIT));
                    }
                    break;
                case DEPOSIT:
                case REMOVAL:
                case INTEREST:
                case INTEREST_CHARGE:
                case TAXES:
                case FEES:
                    pseudoAccount.internalAddTransaction(t);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }
}
