package name.abuchen.portfolio.snapshot.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
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
        this.portfolios = new ArrayList<>(Objects.requireNonNull(portfolios));
        this.accounts = new ArrayList<>(Objects.requireNonNull(accounts));
    }

    public PortfolioClientFilter(Portfolio portfolio)
    {
        this(Arrays.asList(portfolio), Collections.emptyList());
    }

    public PortfolioClientFilter(Portfolio portfolio, Account account)
    {
        this(Arrays.asList(portfolio), Arrays.asList(account));
    }

    public void removeElement(Object element)
    {
        portfolios.remove(element);
        accounts.remove(element);
    }

    public void addElement(Object element)
    {
        if (element instanceof Portfolio portfolio)
            portfolios.add(portfolio);
        else if (element instanceof Account account)
            accounts.add(account);
        else
            throw new IllegalArgumentException("element is null or of wrong type: " + element); //$NON-NLS-1$
    }
    
    public boolean hasElement(Object element)
    {
        if (element instanceof Portfolio portfolio)
            return portfolios.contains(portfolio);
        else if (element instanceof Account account)
            return accounts.contains(account);
        else
            throw new IllegalArgumentException("element is null or of wrong type: " + element); //$NON-NLS-1$
    }

    public Object[] getAllElements()
    {
        return Stream.concat(portfolios.stream(), accounts.stream()).toArray();
    }

    @Override
    public Client filter(Client client)
    {
        ReadOnlyClient pseudoClient = new ReadOnlyClient(client);

        // create all pseudo accounts
        Map<Account, ReadOnlyAccount> account2pseudo = new HashMap<>();
        Function<Account, ReadOnlyAccount> computeReadOnlyAccount = a -> {
            ReadOnlyAccount pa = new ReadOnlyAccount(a);
            pseudoClient.internalAddAccount(pa);
            return pa;
        };
        accounts.stream().forEach(a -> account2pseudo.put(a, computeReadOnlyAccount.apply(a)));

        // create all pseudo portfolios
        Map<Portfolio, ReadOnlyPortfolio> portfolio2pseudo = new HashMap<>();
        portfolios.stream().forEach(p -> {
            ReadOnlyAccount pseudoAccount = account2pseudo.computeIfAbsent(p.getReferenceAccount(),
                            computeReadOnlyAccount);

            ReadOnlyPortfolio pseudoPortfolio = new ReadOnlyPortfolio(p);
            pseudoPortfolio.setReferenceAccount(pseudoAccount);
            pseudoClient.internalAddPortfolio(pseudoPortfolio);
            portfolio2pseudo.put(p, pseudoPortfolio);
        });

        Set<Security> usedSecurities = new HashSet<>();

        // keep track of transactions processed for a portfolio where the
        // reference account is not included in the filter (otherwise if
        // multiple portfolio share the same reference account, transactions are
        // included multiple times)
        Set<AccountTransaction> processedSecurityTx = new HashSet<>();

        for (Portfolio portfolio : portfolios)
        {
            adaptPortfolioTransactions(portfolio, portfolio2pseudo, account2pseudo, usedSecurities);

            if (!accounts.contains(portfolio.getReferenceAccount()))
                collectSecurityRelevantTx(portfolio, account2pseudo.get(portfolio.getReferenceAccount()),
                                usedSecurities, processedSecurityTx);
        }

        for (Account account : accounts)
            adaptAccountTransactions(account, account2pseudo, usedSecurities);

        for (Security security : usedSecurities)
            pseudoClient.internalAddSecurity(security);

        return pseudoClient;
    }

    private void adaptPortfolioTransactions(Portfolio portfolio, Map<Portfolio, ReadOnlyPortfolio> portfolio2pseudo,
                    Map<Account, ReadOnlyAccount> account2pseudo, Set<Security> usedSecurities)
    {
        ReadOnlyPortfolio pseudoPortfolio = portfolio2pseudo.get(portfolio);

        for (PortfolioTransaction t : portfolio.getTransactions())
        {
            usedSecurities.add(t.getSecurity());

            switch (t.getType())
            {
                case BUY:
                    if (accounts.contains(t.getCrossEntry().getCrossOwner(t)))
                        recreateBuySell((BuySellEntry) t.getCrossEntry(), pseudoPortfolio,
                                        account2pseudo.get(t.getCrossEntry().getCrossOwner(t)));
                    else
                        pseudoPortfolio.internalAddTransaction(
                                        convertTo(t, PortfolioTransaction.Type.DELIVERY_INBOUND));
                    break;
                case TRANSFER_IN:
                    if (portfolios.contains(t.getCrossEntry().getCrossOwner(t)))
                        ClientFilterHelper.recreateTransfer((PortfolioTransferEntry) t.getCrossEntry(),
                                        portfolio2pseudo.get(t.getCrossEntry().getCrossOwner(t)), pseudoPortfolio);
                    else
                        pseudoPortfolio.internalAddTransaction(
                                        convertTo(t, PortfolioTransaction.Type.DELIVERY_INBOUND));
                    break;
                case SELL:
                    if (accounts.contains(t.getCrossEntry().getCrossOwner(t)))
                        recreateBuySell((BuySellEntry) t.getCrossEntry(), pseudoPortfolio,
                                        account2pseudo.get(t.getCrossEntry().getCrossOwner(t)));
                    else
                        pseudoPortfolio.internalAddTransaction(
                                        convertTo(t, PortfolioTransaction.Type.DELIVERY_OUTBOUND));
                    break;
                case TRANSFER_OUT:
                    // regular transfer handled by TRANSFER_IN
                    if (!portfolios.contains(t.getCrossEntry().getCrossOwner(t)))
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

    private void recreateBuySell(BuySellEntry buySell, ReadOnlyPortfolio readOnlyPortfolio,
                    ReadOnlyAccount readOnlyAccount)
    {
        PortfolioTransaction t = buySell.getPortfolioTransaction();

        BuySellEntry copy = new BuySellEntry(readOnlyPortfolio, readOnlyAccount);

        copy.setDate(t.getDateTime());
        copy.setCurrencyCode(t.getCurrencyCode());
        copy.setSecurity(t.getSecurity());
        copy.setType(t.getType());
        copy.setNote(t.getNote());
        copy.setShares(t.getShares());
        copy.setAmount(t.getAmount());

        t.getUnits().forEach(u -> copy.getPortfolioTransaction().addUnit(u));

        readOnlyPortfolio.internalAddTransaction(copy.getPortfolioTransaction());
        readOnlyAccount.internalAddTransaction(copy.getAccountTransaction());
    }

    private void collectSecurityRelevantTx(Portfolio portfolio, ReadOnlyAccount pseudoAccount,
                    Set<Security> usedSecurities, Set<AccountTransaction> processedDividendTx)
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
                    if (!processedDividendTx.contains(t))
                    {
                        pseudoAccount.internalAddTransaction(t);
                        pseudoAccount.internalAddTransaction(new AccountTransaction(t.getDateTime(),
                                        t.getCurrencyCode(), t.getAmount(), null, AccountTransaction.Type.REMOVAL));
                        processedDividendTx.add(t);
                    }
                    break;
                case TAXES:
                case FEES:
                    if (!processedDividendTx.contains(t))
                    {
                        pseudoAccount.internalAddTransaction(t);
                        pseudoAccount.internalAddTransaction(new AccountTransaction(t.getDateTime(),
                                        t.getCurrencyCode(), t.getAmount(), null, AccountTransaction.Type.DEPOSIT));
                        processedDividendTx.add(t);
                    }
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

    private void adaptAccountTransactions(Account account, Map<Account, ReadOnlyAccount> account2pseudo,
                    Set<Security> usedSecurities)
    {
        ReadOnlyAccount pseudoAccount = account2pseudo.get(account);

        for (AccountTransaction t : account.getTransactions())
        {
            switch (t.getType())
            {
                case BUY:
                    if (!portfolios.contains(t.getCrossEntry().getCrossOwner(t)))
                        pseudoAccount.internalAddTransaction(convertTo(t, AccountTransaction.Type.REMOVAL));
                    // regular buy is handled via portfolio transactions
                    break;
                case SELL:
                    if (!portfolios.contains(t.getCrossEntry().getCrossOwner(t)))
                        pseudoAccount.internalAddTransaction(convertTo(t, AccountTransaction.Type.DEPOSIT));
                    // regular sell is handled via portfolio transactions
                    break;
                case TRANSFER_IN:
                    if (accounts.contains(t.getCrossEntry().getCrossOwner(t)))
                        ClientFilterHelper.recreateTransfer((AccountTransferEntry) t.getCrossEntry(),
                                        account2pseudo.get(t.getCrossEntry().getCrossOwner(t)), pseudoAccount);
                    else
                        pseudoAccount.internalAddTransaction(convertTo(t, AccountTransaction.Type.DEPOSIT));
                    break;
                case TRANSFER_OUT:
                    // regular transfer handled by TRANSFER_IN
                    if (!accounts.contains(t.getCrossEntry().getCrossOwner(t)))
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
        clone.setDateTime(t.getDateTime());
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
        clone.setDateTime(t.getDateTime());
        clone.setCurrencyCode(t.getCurrencyCode());
        clone.setSecurity(null); // no security for REMOVAL or DEPOSIT
        clone.setAmount(t.getAmount());
        clone.setShares(t.getShares());

        // do *not* copy units as REMOVAL and DEPOSIT have never units
        return clone;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((accounts == null) ? 0 : accounts.hashCode());
        result = prime * result + ((portfolios == null) ? 0 : portfolios.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        PortfolioClientFilter other = (PortfolioClientFilter) obj;

        return accounts.equals(other.accounts) && portfolios.equals(other.portfolios);
    }
}
