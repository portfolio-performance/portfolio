package name.abuchen.portfolio.snapshot.filter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;

/**
 * Filters the Client to include only transactions related to the given
 * portfolios and accounts.
 */
public class PortfolioClientFilter implements ClientFilter
{
    private final List<Portfolio> portfolios;
    private final List<Account> accounts;

    public PortfolioClientFilter(List<Portfolio> portfolios, List<Account> accounts)
    {
        this.portfolios = portfolios;
        this.accounts = accounts;
    }

    public PortfolioClientFilter(Portfolio portfolio)
    {
        this(Arrays.asList(portfolio), Collections.emptyList());
    }

    public PortfolioClientFilter(Portfolio portfolio, Account account)
    {
        this(Arrays.asList(portfolio), Arrays.asList(account));
    }

    @Override
    public Client filter(Client client)
    {
        ReadOnlyClient pseudoClient = new ReadOnlyClient(client);

        Map<Account, ReadOnlyAccount> account2pseudo = new HashMap<>();
        Set<Security> usedSecurities = new HashSet<>();

        for (Portfolio portfolio : portfolios)
        {
            ReadOnlyAccount pseudoAccount = account2pseudo.computeIfAbsent(portfolio.getReferenceAccount(), a -> {
                ReadOnlyAccount pa = new ReadOnlyAccount(a);
                pseudoClient.internalAddAccount(pa);
                return pa;
            });

            ReadOnlyPortfolio pseudoPortfolio = new ReadOnlyPortfolio(portfolio);
            pseudoPortfolio.setReferenceAccount(pseudoAccount);
            pseudoClient.internalAddPortfolio(pseudoPortfolio);

            adaptPortfolioTransactions(portfolio, pseudoPortfolio, usedSecurities);

            if (!accounts.contains(portfolio.getReferenceAccount()))
                collectSecurityRelevantTx(portfolio, pseudoAccount, usedSecurities);
        }

        for (Account account : accounts)
        {
            ReadOnlyAccount pseudoAccount = account2pseudo.computeIfAbsent(account, a -> {
                ReadOnlyAccount pa = new ReadOnlyAccount(a);
                pseudoClient.internalAddAccount(pa);
                return pa;
            });

            adaptAccountTransactions(account, pseudoAccount, usedSecurities);
        }

        for (Security security : usedSecurities)
            pseudoClient.internalAddSecurity(security);

        return pseudoClient;
    }

    private void adaptPortfolioTransactions(Portfolio portfolio, ReadOnlyPortfolio pseudoPortfolio,
                    Set<Security> usedSecurities)
    {
        for (PortfolioTransaction t : portfolio.getTransactions())
        {
            usedSecurities.add(t.getSecurity());

            switch (t.getType())
            {
                case BUY:
                    if (accounts.contains(t.getCrossEntry().getCrossOwner(t)))
                        pseudoPortfolio.internalAddTransaction(t);
                    else
                        pseudoPortfolio.internalAddTransaction(
                                        convertTo(t, PortfolioTransaction.Type.DELIVERY_INBOUND));
                    break;
                case TRANSFER_IN:
                    if (portfolios.contains(t.getCrossEntry().getCrossOwner(t)))
                        pseudoPortfolio.internalAddTransaction(t);
                    else
                        pseudoPortfolio.internalAddTransaction(
                                        convertTo(t, PortfolioTransaction.Type.DELIVERY_INBOUND));
                    break;
                case SELL:
                    if (accounts.contains(t.getCrossEntry().getCrossOwner(t)))
                        pseudoPortfolio.internalAddTransaction(t);
                    else
                        pseudoPortfolio.internalAddTransaction(
                                        convertTo(t, PortfolioTransaction.Type.DELIVERY_OUTBOUND));
                    break;
                case TRANSFER_OUT:
                    if (portfolios.contains(t.getCrossEntry().getCrossOwner(t)))
                        pseudoPortfolio.internalAddTransaction(t);
                    else
                        pseudoPortfolio.internalAddTransaction(
                                        convertTo(t, PortfolioTransaction.Type.DELIVERY_OUTBOUND));
                    break;
                case DELIVERY_INBOUND:
                case DELIVERY_OUTBOUND:
                    pseudoPortfolio.internalAddTransaction(t);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    private void collectSecurityRelevantTx(Portfolio portfolio, ReadOnlyAccount pseudoAccount, Set<Security> usedSecurities)
    {
        if (portfolio.getReferenceAccount() == null)
            return;

        for (AccountTransaction t : portfolio.getReferenceAccount().getTransactions()) // NOSONAR
        {
            if (t.getSecurity() == null)
                continue;

            if (!usedSecurities.contains(t.getSecurity()))
                continue;

            switch (t.getType())
            {
                case TAX_REFUND:
                case FEES_REFUND:
                    // security must be non-null -> tax refund is relevant for
                    // performance of security
                case DIVIDENDS:
                    pseudoAccount.internalAddTransaction(t);
                    pseudoAccount.internalAddTransaction(new AccountTransaction(t.getDate(), t.getCurrencyCode(),
                                    t.getAmount(), null, AccountTransaction.Type.REMOVAL));
                    break;
                case TAXES:
                case FEES:
                    pseudoAccount.internalAddTransaction(t);
                    pseudoAccount.internalAddTransaction(new AccountTransaction(t.getDate(), t.getCurrencyCode(),
                                    t.getAmount(), null, AccountTransaction.Type.DEPOSIT));
                    break;
                case BUY:
                case TRANSFER_IN:
                case SELL:
                case TRANSFER_OUT:
                case DEPOSIT:
                case REMOVAL:
                case INTEREST:
                case INTEREST_CHARGE:
                    // do nothing
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    private void adaptAccountTransactions(Account account, ReadOnlyAccount pseudoAccount, Set<Security> usedSecurities)
    {
        for (AccountTransaction t : account.getTransactions())
        {
            switch (t.getType())
            {
                case BUY:
                    if (portfolios.contains(t.getCrossEntry().getCrossOwner(t)))
                        pseudoAccount.internalAddTransaction(t);
                    else
                        pseudoAccount.internalAddTransaction(convertTo(t, AccountTransaction.Type.REMOVAL));
                    break;
                case SELL:
                    if (portfolios.contains(t.getCrossEntry().getCrossOwner(t)))
                        pseudoAccount.internalAddTransaction(t);
                    else
                        pseudoAccount.internalAddTransaction(convertTo(t, AccountTransaction.Type.DEPOSIT));
                    break;
                case TRANSFER_IN:
                    if (accounts.contains(t.getCrossEntry().getCrossOwner(t)))
                        pseudoAccount.internalAddTransaction(t);
                    else
                        pseudoAccount.internalAddTransaction(convertTo(t, AccountTransaction.Type.DEPOSIT));
                    break;
                case TRANSFER_OUT:
                    if (accounts.contains(t.getCrossEntry().getCrossOwner(t)))
                        pseudoAccount.internalAddTransaction(t);
                    else
                        pseudoAccount.internalAddTransaction(convertTo(t, AccountTransaction.Type.REMOVAL));
                    break;
                case DIVIDENDS:
                case TAX_REFUND:
                case FEES_REFUND:
                    if (t.getSecurity() == null || usedSecurities.contains(t.getSecurity()))
                        pseudoAccount.internalAddTransaction(t);
                    else
                        pseudoAccount.internalAddTransaction(convertTo(t, AccountTransaction.Type.DEPOSIT));
                    break;
                case TAXES:
                case FEES:
                    if (t.getSecurity() == null || usedSecurities.contains(t.getSecurity()))
                        pseudoAccount.internalAddTransaction(t);
                    else
                        pseudoAccount.internalAddTransaction(convertTo(t, AccountTransaction.Type.REMOVAL));
                    break;
                case DEPOSIT:
                case REMOVAL:
                case INTEREST:
                case INTEREST_CHARGE:
                    pseudoAccount.internalAddTransaction(t);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    private PortfolioTransaction convertTo(PortfolioTransaction t, PortfolioTransaction.Type type)
    {
        PortfolioTransaction clone = new PortfolioTransaction();
        clone.setType(type);
        clone.setDate(t.getDate());
        clone.setCurrencyCode(t.getCurrencyCode());
        clone.setSecurity(t.getSecurity());
        clone.setAmount(t.getAmount());
        clone.setShares(t.getShares());
        clone.addUnits(t.getUnits());
        return clone;
    }

    private AccountTransaction convertTo(AccountTransaction t, AccountTransaction.Type type)
    {
        AccountTransaction clone = new AccountTransaction();
        clone.setType(type);
        clone.setDate(t.getDate());
        clone.setCurrencyCode(t.getCurrencyCode());
        clone.setSecurity(null); // no security for REMOVAL or DEPOSIT
        clone.setAmount(t.getAmount());
        clone.setShares(t.getShares());

        // do *not* copy units as REMOVAL and DEPOSIT have never units
        return clone;
    }
}
