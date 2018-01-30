package name.abuchen.portfolio.snapshot.filter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CrossEntry;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.util.SecurityUtil;

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
        FilterContext filterContext = new FilterContext(client);

        for (Portfolio portfolio : portfolios)
        {
            adaptPortfolioTransactions(portfolio, filterContext.get(portfolio));

            // collect security relevant transactions for all connected accounts
            filterContext.portfolio2Accounts.get(portfolio).forEach(acc -> {
                if (!accounts.contains(acc.getSource()))
                    collectSecurityRelevantTx(filterContext, portfolio, acc);
            });
        }

        for (Account account : accounts)
        {
            adaptAccountTransactions(filterContext, account);
        }

        return filterContext.getClient();
    }

    private void adaptPortfolioTransactions(Portfolio portfolio, ReadOnlyPortfolio pseudoPortfolio)
    {
        for (PortfolioTransaction t : portfolio.getTransactions())
        {
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

    private double calculatePercentageOfDividendsOfSecurities(FilterContext filterContext,
                    ReadOnlyAccount pseudoAccount, Transaction t)
    {
        Security security = t.getSecurity();
        Optional<SecurityEvent> lastExDay = SecurityUtil.findLastDividendEvent(security, t.getDateTime().toLocalDate());
        if (!lastExDay.isPresent())
        {
            // no ex day available, therefor it is not possible to know how many
            // shares belong to this portfolio
            // -> fallback: divide transaction to all portfolios with the given
            // account as reference account
            return calculatePercentage(filterContext, pseudoAccount);
        }
        else if (lastExDay.get().getDate().isBefore(t.getDateTime().toLocalDate().minusMonths(3)))
        {
            // if the ex day is more than three months before the transaction
            // they cannot belong together
            // -> divide transaction to all portfolios with the given account as
            // reference account
            return calculatePercentage(filterContext, pseudoAccount);
        }
        LocalDate exDay = lastExDay.get().getDate();
        // The stocks must be hold on the day BEFORE the ex day to be entitled
        // to the dividend
        return calculatePercentageOfSecurities(filterContext, pseudoAccount, t, exDay.atStartOfDay());
    }

    private double calculatePercentage(FilterContext filterContext, ReadOnlyAccount pseudoAccount)
    {
        long selectedPortfoliosWithAccount = filterContext.portfolio2Accounts.entrySet().stream()
                        .filter(e -> e.getValue().contains(pseudoAccount)).count();
        return 1d / selectedPortfoliosWithAccount;
    }

    private double calculatePercentageOfSecurities(FilterContext filterContext, ReadOnlyAccount pseudoAccount,
                    Transaction t, LocalDateTime referenceDate)
    {
        ReadOnlyClient pseudoClient = filterContext.getClient();
        Client client = pseudoClient.getSource();
        // get the security position at the time of the given date
        SecurityPosition securityPosition = calculateSecurityPosition(pseudoClient.getPortfolios().stream() //
                        .map(p -> ((ReadOnlyPortfolio) p).getSource()) //
                        .collect(Collectors.toList()) //
                        , pseudoAccount, t.getSecurity(), referenceDate);
        if (securityPosition.getShares() == 0)
        {
            // there is no security available for this transaction at this date
            return 0;
        }

        long referenceShareCount;
        if (t.getShares() == 0)
        {
            // there is no information available about the amount of shares the
            // transaction is affecting. Use the number of shares for this
            // security in all portfolios tracked in the given account instead.
            SecurityPosition clientSecurityPosition = calculateSecurityPosition(client.getPortfolios(), pseudoAccount, t.getSecurity(), referenceDate);
            referenceShareCount = clientSecurityPosition.getFilteredShares(pt -> pseudoAccount.getSource().equals(pt.getCrossEntry().getCrossOwner(pt)));
        }
        else
        {
            referenceShareCount = t.getShares();
        }

        // Count the number of shares of this security tracked in the given account
        long accountShares = securityPosition.getFilteredShares(
                        pt -> pseudoAccount.getSource().equals(pt.getCrossEntry().getCrossOwner(pt)));

        return Math.min(1d, accountShares * 1d / referenceShareCount);
    }

    private SecurityPosition calculateSecurityPosition(Collection<Portfolio> portfolios, ReadOnlyAccount pseudoAccount, Security security, LocalDateTime exDay)
    {
        // we need to add the real portfolios here, because the transactions are
        // probably changed
        List<Portfolio> sourcePortfolios = portfolios.stream().map(p -> {
            if (p instanceof ReadOnlyPortfolio)
            {
                return ((ReadOnlyPortfolio) p).getSource();
            }
            else
            {
                return p;
            }
        }).collect(Collectors.toList());
        // only the account of the evaluated transaction is interesting here
        Account referenceAccount = pseudoAccount.getSource();

        // for dividends we must use the ex-day to evaluate the stock count
        // relevant for the calculation
        return getSecurityPosition(sourcePortfolios, referenceAccount, security, exDay);
    }
    
    private static SecurityPosition getSecurityPosition(Collection<Portfolio> portfolios, Account referenceAccount, Security security, LocalDateTime date)
    {
        List<PortfolioTransaction> transactions = new ArrayList<>();
        portfolios.forEach(portfolio -> {
            transactions.addAll(portfolio.getTransactions() //
                            .stream() //
                            .filter(t -> t.getSecurity().equals(security)) //
                            .filter(t -> t.getDateTime().isBefore(date)) //
                            .filter(t -> {
                                CrossEntry crossEntry = t.getCrossEntry();
                                if (crossEntry instanceof BuySellEntry)
                                    return referenceAccount.equals(((BuySellEntry) crossEntry).getAccount());
                                return true;
                            }) //
                            .collect(Collectors.toList()));
        });
        return new SecurityPosition(security, new CurrencyConverterImpl(null, referenceAccount.getCurrencyCode()), security.getSecurityPrice(date.toLocalDate()), transactions);
    }

    private void collectSecurityRelevantTx(FilterContext filterContext, Portfolio portfolio,
                    ReadOnlyAccount pseudoAccount)
    {
        if (portfolio.getReferenceAccount() == null)
            return;

        // Iterate through the transactions of the "real" account
        for (AccountTransaction t : pseudoAccount.getSource().getTransactions()) // NOSONAR
        {
            if (t.getSecurity() == null)
                continue;

            if (!filterContext.usedSecurities.contains(t.getSecurity()))
                continue;

            switch (t.getType())
            {
                case TAX_REFUND:
                case FEES_REFUND:
                    // security must be non-null -> tax refund is relevant for
                    // performance of security
                    // ATTENTION: the transaction cannot be matched to a portfolio transaction.
                    // Therefore it is not possible to know if this transaction is really relevant for this portfolio.
                    // As a compromise the transaction is spread between all portfolios, where the current account is also the reference account
                    // Otherwise those transactions would be fully considered for each portfolio. 
                    double taxRefundPart = calculatePercentage(filterContext, pseudoAccount);
                    addAdjustedSecurityRelevantTransaction(pseudoAccount, t, taxRefundPart,
                                    AccountTransaction.Type.REMOVAL);
                    break;
                case DIVIDENDS:
                    double dividendPart = calculatePercentageOfDividendsOfSecurities(filterContext, pseudoAccount, t);
                    addAdjustedSecurityRelevantTransaction(pseudoAccount, t, dividendPart,
                                    AccountTransaction.Type.REMOVAL);
                    break;
                case TAXES:
                case FEES:
                    double taxFeePart = calculatePercentage(filterContext, pseudoAccount);
                    addAdjustedSecurityRelevantTransaction(pseudoAccount, t, taxFeePart,
                                    AccountTransaction.Type.DEPOSIT);
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

    private void addAdjustedSecurityRelevantTransaction(ReadOnlyAccount account, AccountTransaction t,
                    double percentage, AccountTransaction.Type type)
    {
        // the percentage must be > 0 to create transactions
        if (percentage > 0)
        {
            if (percentage == 1)
            {
                // if the full amount can be used, add the transaction directly
                account.internalAddTransaction(t);
                account.internalAddTransaction(convertTo(t, type));
            }
            else
            {
                // calculate the amount to use of this transaction
                long amount = Math.round(t.getAmount() * percentage);
                AccountTransaction adjustedTransaction = new AccountTransaction(t.getDateTime(), t.getCurrencyCode(),
                                amount, t.getSecurity(), t.getType());
                // adjust all units accordingly
                adjustedTransaction.addUnits(t.getUnits().map(u -> {
                    Money unitMoney = u.getAmount();
                    Money adjustedUnitMoney = Money.of(unitMoney.getCurrencyCode(),
                                    Math.round(unitMoney.getAmount() * percentage));
                    if (u.getForex() == null)
                    {
                        return new Unit(u.getType(), adjustedUnitMoney);
                    }
                    else
                    {
                        Money unitForex = u.getForex();
                        Money adjustedUnitForex = Money.of(unitForex.getCurrencyCode(),
                                        Math.round(unitForex.getAmount() * percentage));
                        return new Unit(u.getType(), adjustedUnitMoney, adjustedUnitForex, u.getExchangeRate());
                    }
                }));

                account.internalAddTransaction(adjustedTransaction);
                account.internalAddTransaction(
                                new AccountTransaction(t.getDateTime(), t.getCurrencyCode(), amount, null, type));
            }
        }
    }

    private void adaptAccountTransactions(FilterContext filterContext, Account account)
    {
        ReadOnlyAccount pseudoAccount = filterContext.get(account);
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
                    if (t.getSecurity() == null || filterContext.usedSecurities.contains(t.getSecurity()))
                        pseudoAccount.internalAddTransaction(t);
                    else
                        pseudoAccount.internalAddTransaction(convertTo(t, AccountTransaction.Type.DEPOSIT));
                    break;
                case TAXES:
                case FEES:
                    if (t.getSecurity() == null || filterContext.usedSecurities.contains(t.getSecurity()))
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

    private class FilterContext
    {
        private ReadOnlyClient pseudoClient;
        private Map<Account, ReadOnlyAccount> account2Pseudo = new HashMap<>();
        private Map<Portfolio, ReadOnlyPortfolio> portfolio2Pseudo = new HashMap<>();
        private Map<Portfolio, Set<ReadOnlyAccount>> portfolio2Accounts = new HashMap<>();
        private Set<Security> usedSecurities = new HashSet<>();

        public FilterContext(final Client client)
        {
            pseudoClient = new ReadOnlyClient(client);

            for (Portfolio portfolio : portfolios)
            {
                for (PortfolioTransaction t : portfolio.getTransactions())
                {
                    usedSecurities.add(t.getSecurity());
                }
                portfolio2Accounts.put(portfolio, collectAccountsWithTransactions(portfolio));
            }

            usedSecurities.forEach(sec -> pseudoClient.internalAddSecurity(sec));
        }

        public ReadOnlyClient getClient()
        {
            return pseudoClient;
        }

        private ReadOnlyAccount get(final Account account)
        {
            // if an account was found, return a pseudo account
            return account2Pseudo.computeIfAbsent(account, a -> {
                ReadOnlyAccount pa = new ReadOnlyAccount(a);
                pseudoClient.internalAddAccount(pa);
                return pa;
            });
        }

        private ReadOnlyPortfolio get(final Portfolio portfolio)
        {
            return portfolio2Pseudo.computeIfAbsent(portfolio, p -> {
                ReadOnlyPortfolio pp = new ReadOnlyPortfolio(p);
                pp.setReferenceAccount(get(portfolio.getReferenceAccount()));
                pseudoClient.internalAddPortfolio(pp);
                return pp;
            });
        }

        private Set<ReadOnlyAccount> collectAccountsWithTransactions(Portfolio portfolio)
        {
            return portfolio.getTransactions().stream().map(t -> {
                CrossEntry crossEntry = t.getCrossEntry();
                Account account = null;
                if (crossEntry instanceof BuySellEntry)
                {
                    // if it is a buy-sell entry use the affected account
                    account = ((BuySellEntry) crossEntry).getAccount();
                }
                else if (crossEntry instanceof PortfolioTransferEntry)
                {
                    // if it is a portfolio transfer entry use the owning
                    // account or
                    // the reference account of the owning portfolio
                }
                if (account != null)
                {
                    // if an account was found, return a pseudo account
                    return get(account);
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toSet());
        }
    }
}
