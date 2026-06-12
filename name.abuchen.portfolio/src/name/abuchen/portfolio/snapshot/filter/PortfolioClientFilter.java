package name.abuchen.portfolio.snapshot.filter;

import java.math.BigDecimal;
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
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;

/**
 * Filters the Client to include only transactions related to the given
 * portfolios and accounts. Each portfolio or account can additionally carry an
 * ownership <em>weight</em> (a percentage in the
 * {@link Classification#ONE_HUNDRED_PERCENT} scale); when an element is
 * weighted below 100% all of its transactions are scaled accordingly, so that a
 * shared account or securities account can be analyzed for one owner's share.
 * The default weight is 100%, in which case the result is identical to the
 * unweighted filter.
 */
public class PortfolioClientFilter implements ClientFilter
{
    private final List<Portfolio> portfolios;
    private final List<Account> accounts;

    /**
     * Ownership weight per element (Portfolio or Account) in the
     * {@link Classification#ONE_HUNDRED_PERCENT} scale. Canonical form: the
     * default 100% is <em>never</em> stored (absent ⇒ 100%), so that two
     * behaviorally identical filters compare equal.
     */
    private final Map<Object, Integer> element2weight;

    public PortfolioClientFilter(List<Portfolio> portfolios, List<Account> accounts)
    {
        this(portfolios, accounts, Collections.emptyMap());
    }

    public PortfolioClientFilter(List<Portfolio> portfolios, List<Account> accounts, Map<Object, Integer> weights)
    {
        this.portfolios = new ArrayList<>(Objects.requireNonNull(portfolios));
        this.accounts = new ArrayList<>(Objects.requireNonNull(accounts));
        this.element2weight = new HashMap<>();

        if (weights != null && !weights.isEmpty())
        {
            var members = new HashSet<Object>();
            members.addAll(this.portfolios);
            members.addAll(this.accounts);

            for (var entry : weights.entrySet()) // NOSONAR
            {
                var weight = entry.getValue();
                // canonical-form normalization: drop non-members, invalid, and
                // the default 100% (absence already means 100%)
                if (weight == null || !members.contains(entry.getKey()))
                    continue;
                if (weight.intValue() < 1 || weight.intValue() >= Classification.ONE_HUNDRED_PERCENT)
                    continue;
                element2weight.put(entry.getKey(), weight);
            }
        }
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
        element2weight.remove(element);
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

    /**
     * Returns the ownership weight (in the
     * {@link Classification#ONE_HUNDRED_PERCENT} scale) of the given element.
     * Returns 100% for non-members and for elements without an explicit weight.
     */
    public int getWeight(Object element)
    {
        var weight = element2weight.get(element);
        return weight == null ? Classification.ONE_HUNDRED_PERCENT : weight.intValue();
    }

    /**
     * Sets the ownership weight of an element. The element must be a current
     * member; the weight must be in {@code [1, ONE_HUNDRED_PERCENT]}. Setting
     * it to 100% removes the entry (canonical form).
     */
    public void setWeight(Object element, int weight)
    {
        if (!hasElement(element))
            throw new IllegalArgumentException("element is not part of the filter: " + element); //$NON-NLS-1$
        if (weight < 1 || weight > Classification.ONE_HUNDRED_PERCENT)
            throw new IllegalArgumentException("weight out of range: " + weight); //$NON-NLS-1$

        if (weight == Classification.ONE_HUNDRED_PERCENT)
            element2weight.remove(element);
        else
            element2weight.put(element, weight);
    }

    private BigDecimal getWeightBD(Object element)
    {
        return BigDecimal.valueOf(getWeight(element));
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

        // index of (reference account -> security -> highest ownership weight
        // among included portfolios that book that security there), built as a
        // side effect of the portfolio pass below so that resolving a security
        // weight for an account transaction is an O(1) lookup instead of a full
        // re-scan of every portfolio per account transaction.
        Map<Account, Map<Security, Integer>> securityWeightIndex = new HashMap<>();

        // keep track of transactions processed for a portfolio where the
        // reference account is not included in the filter (otherwise if
        // multiple portfolio share the same reference account, transactions are
        // included multiple times)
        Set<AccountTransaction> processedSecurityTx = new HashSet<>();

        for (Portfolio portfolio : portfolios)
        {
            adaptPortfolioTransactions(portfolio, portfolio2pseudo, account2pseudo, usedSecurities,
                            securityWeightIndex);

            if (!accounts.contains(portfolio.getReferenceAccount()))
                collectSecurityRelevantTx(portfolio, account2pseudo.get(portfolio.getReferenceAccount()),
                                usedSecurities, processedSecurityTx);
        }

        for (Account account : accounts)
            adaptAccountTransactions(account, account2pseudo, usedSecurities, securityWeightIndex);

        for (Security security : usedSecurities)
            pseudoClient.internalAddSecurity(security);

        return pseudoClient;
    }

    private void adaptPortfolioTransactions(Portfolio portfolio, Map<Portfolio, ReadOnlyPortfolio> portfolio2pseudo,
                    Map<Account, ReadOnlyAccount> account2pseudo, Set<Security> usedSecurities,
                    Map<Account, Map<Security, Integer>> securityWeightIndex)
    {
        ReadOnlyPortfolio pseudoPortfolio = portfolio2pseudo.get(portfolio);
        BigDecimal portfolioWeight = getWeightBD(portfolio);

        var referenceAccount = portfolio.getReferenceAccount();
        var perSecurity = referenceAccount == null ? null
                        : securityWeightIndex.computeIfAbsent(referenceAccount, k -> new HashMap<>());

        for (PortfolioTransaction t : portfolio.getTransactions())
        {
            var security = t.getSecurity();
            usedSecurities.add(security);

            // fold the (reference account, security) -> max weight index into
            // this pass instead of re-scanning all portfolios per account tx
            if (perSecurity != null && security != null)
                perSecurity.merge(security, getWeight(portfolio), Math::max);

            Object crossOwner = t.getCrossEntry() != null ? t.getCrossEntry().getCrossOwner(t) : null;

            switch (t.getType())
            {
                case BUY:
                    if (accounts.contains(crossOwner))
                        recreateBuySell((BuySellEntry) t.getCrossEntry(), pseudoPortfolio,
                                        account2pseudo.get(crossOwner), portfolio, (Account) crossOwner);
                    else
                        pseudoPortfolio.internalAddTransaction(
                                        convertTo(t, PortfolioTransaction.Type.DELIVERY_INBOUND, portfolioWeight));
                    break;
                case TRANSFER_IN:
                    if (portfolios.contains(crossOwner))
                        ClientFilterHelper.recreateTransfer((PortfolioTransferEntry) t.getCrossEntry(),
                                        portfolio2pseudo.get(crossOwner), pseudoPortfolio, getWeight(crossOwner),
                                        getWeight(portfolio));
                    else
                        pseudoPortfolio.internalAddTransaction(
                                        convertTo(t, PortfolioTransaction.Type.DELIVERY_INBOUND, portfolioWeight));
                    break;
                case SELL:
                    if (accounts.contains(crossOwner))
                        recreateBuySell((BuySellEntry) t.getCrossEntry(), pseudoPortfolio,
                                        account2pseudo.get(crossOwner), portfolio, (Account) crossOwner);
                    else
                        pseudoPortfolio.internalAddTransaction(
                                        convertTo(t, PortfolioTransaction.Type.DELIVERY_OUTBOUND, portfolioWeight));
                    break;
                case TRANSFER_OUT:
                    // regular transfer handled by TRANSFER_IN
                    if (!portfolios.contains(crossOwner))
                        pseudoPortfolio.internalAddTransaction(
                                        convertTo(t, PortfolioTransaction.Type.DELIVERY_OUTBOUND, portfolioWeight));
                    break;
                case DELIVERY_INBOUND, DELIVERY_OUTBOUND:
                    pseudoPortfolio.internalAddTransaction(scaled(t, portfolioWeight));
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    private void recreateBuySell(BuySellEntry buySell, ReadOnlyPortfolio readOnlyPortfolio,
                    ReadOnlyAccount readOnlyAccount, Portfolio portfolio, Account account)
    {
        PortfolioTransaction t = buySell.getPortfolioTransaction();

        var portfolioWeight = getWeightBD(portfolio);
        var accountWeight = getWeightBD(account);

        if (portfolioWeight.equals(accountWeight))
        {
            // both sides share the same weight -> a single scaled buy/sell
            var copy = new BuySellEntry(readOnlyPortfolio, readOnlyAccount);
            copy.setDate(t.getDateTime());
            copy.setCurrencyCode(t.getCurrencyCode());
            copy.setSecurity(t.getSecurity());
            copy.setType(t.getType());
            copy.setNote(t.getNote());
            copy.setShares(ClientFilterHelper.value(t.getShares(), portfolioWeight));
            copy.setAmount(ClientFilterHelper.value(t.getAmount(), portfolioWeight));

            t.getUnits().forEach(
                            u -> copy.getPortfolioTransaction().addUnit(ClientFilterHelper.value(u, portfolioWeight)));

            readOnlyPortfolio.internalAddTransaction(copy.getPortfolioTransaction());
            readOnlyAccount.internalAddTransaction(copy.getAccountTransaction());
            return;
        }

        // weights differ -> keep the common (lower-weighted) part as a linked
        // buy/sell and emit the excess: a deposit/removal in the account if the
        // account is the higher-weighted side, an additional delivery in the
        // portfolio if the security is. Taxes are preserved (unlike
        // ClientClassificationFilter, which is a pre-tax view).

        long securityAmount = ClientFilterHelper.value(t.getAmount(), portfolioWeight);
        long accountAmount = ClientFilterHelper.value(t.getAmount(), accountWeight);

        long commonAmount = Math.min(securityAmount, accountAmount);
        var commonWeight = securityAmount == 0L ? portfolioWeight
                        : BigDecimal.valueOf(commonAmount).divide(BigDecimal.valueOf(securityAmount), Values.MC)
                                        .multiply(portfolioWeight, Values.MC);

        var copy = new BuySellEntry(readOnlyPortfolio, readOnlyAccount);
        copy.setDate(t.getDateTime());
        copy.setCurrencyCode(t.getCurrencyCode());
        copy.setSecurity(t.getSecurity());
        copy.setType(t.getType());
        copy.setNote(t.getNote());
        copy.setShares(ClientFilterHelper.value(t.getShares(), commonWeight));
        copy.setAmount(commonAmount);

        t.getUnits().forEach(u -> copy.getPortfolioTransaction().addUnit(ClientFilterHelper.value(u, commonWeight)));

        readOnlyPortfolio.internalAddTransaction(copy.getPortfolioTransaction());
        readOnlyAccount.internalAddTransaction(copy.getAccountTransaction());

        if (accountAmount - commonAmount > 0)
        {
            var at = new AccountTransaction(t.getDateTime(), t.getCurrencyCode(), accountAmount - commonAmount, null,
                            t.getType() == PortfolioTransaction.Type.BUY ? AccountTransaction.Type.REMOVAL
                                            : AccountTransaction.Type.DEPOSIT);
            readOnlyAccount.internalAddTransaction(at);
        }

        if (securityAmount - commonAmount > 0)
        {
            var tp = new PortfolioTransaction();
            tp.setDateTime(t.getDateTime());
            tp.setCurrencyCode(t.getCurrencyCode());
            tp.setSecurity(t.getSecurity());
            tp.setShares(ClientFilterHelper.value(t.getShares(), portfolioWeight.subtract(commonWeight)));
            tp.setType(t.getType() == PortfolioTransaction.Type.BUY ? PortfolioTransaction.Type.DELIVERY_INBOUND
                            : PortfolioTransaction.Type.DELIVERY_OUTBOUND);
            tp.setAmount(securityAmount - commonAmount);

            t.getUnits().forEach(u -> tp.addUnit(ClientFilterHelper.value(u, portfolioWeight.subtract(commonWeight))));

            readOnlyPortfolio.internalAddTransaction(tp);
        }
    }

    private void collectSecurityRelevantTx(Portfolio portfolio, ReadOnlyAccount pseudoAccount,
                    Set<Security> usedSecurities, Set<AccountTransaction> processedDividendTx)
    {
        if (portfolio.getReferenceAccount() == null)
            return;

        var portfolioWeight = getWeightBD(portfolio);

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
                        pseudoAccount.internalAddTransaction(scaled(t, portfolioWeight));
                        pseudoAccount.internalAddTransaction(new AccountTransaction(t.getDateTime(),
                                        t.getCurrencyCode(), ClientFilterHelper.value(t.getAmount(), portfolioWeight),
                                        null, AccountTransaction.Type.REMOVAL));
                        processedDividendTx.add(t);
                    }
                    break;
                case TAXES:
                case FEES:
                    if (!processedDividendTx.contains(t))
                    {
                        pseudoAccount.internalAddTransaction(scaled(t, portfolioWeight));
                        pseudoAccount.internalAddTransaction(new AccountTransaction(t.getDateTime(),
                                        t.getCurrencyCode(), ClientFilterHelper.value(t.getAmount(), portfolioWeight),
                                        null, AccountTransaction.Type.DEPOSIT));
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
                    Set<Security> usedSecurities, Map<Account, Map<Security, Integer>> securityWeightIndex)
    {
        ReadOnlyAccount pseudoAccount = account2pseudo.get(account);
        BigDecimal accountWeight = getWeightBD(account);

        for (AccountTransaction t : account.getTransactions())
        {
            Object crossOwner = t.getCrossEntry() != null ? t.getCrossEntry().getCrossOwner(t) : null;

            switch (t.getType())
            {
                case BUY:
                    if (!portfolios.contains(crossOwner))
                        pseudoAccount.internalAddTransaction(
                                        convertTo(t, AccountTransaction.Type.REMOVAL, accountWeight));
                    // regular buy is handled via portfolio transactions
                    break;
                case SELL:
                    if (!portfolios.contains(crossOwner))
                        pseudoAccount.internalAddTransaction(
                                        convertTo(t, AccountTransaction.Type.DEPOSIT, accountWeight));
                    // regular sell is handled via portfolio transactions
                    break;
                case TRANSFER_IN:
                    if (accounts.contains(crossOwner))
                        ClientFilterHelper.recreateTransfer((AccountTransferEntry) t.getCrossEntry(),
                                        account2pseudo.get(crossOwner), pseudoAccount, getWeight(crossOwner),
                                        getWeight(account));
                    else
                        pseudoAccount.internalAddTransaction(
                                        convertTo(t, AccountTransaction.Type.DEPOSIT, accountWeight));
                    break;
                case TRANSFER_OUT:
                    // regular transfer handled by TRANSFER_IN
                    if (!accounts.contains(crossOwner))
                        pseudoAccount.internalAddTransaction(
                                        convertTo(t, AccountTransaction.Type.REMOVAL, accountWeight));
                    break;
                case DIVIDENDS:
                case TAX_REFUND:
                case FEES_REFUND:
                    addSecurityRelatedAccountTx(account, pseudoAccount, t, usedSecurities, accountWeight,
                                    AccountTransaction.Type.DEPOSIT, securityWeightIndex);
                    break;
                case TAXES:
                case FEES:
                    addSecurityRelatedAccountTx(account, pseudoAccount, t, usedSecurities, accountWeight,
                                    AccountTransaction.Type.REMOVAL, securityWeightIndex);
                    break;
                case DEPOSIT:
                case REMOVAL:
                case INTEREST:
                case INTEREST_CHARGE:
                    pseudoAccount.internalAddTransaction(scaled(t, accountWeight));
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    /**
     * Handles a security-related account transaction (dividend, tax, fee or
     * refund). If the security is not part of the filter, it is converted to a
     * plain cash flow scaled by the account weight. Otherwise the security part
     * is booked at the <em>portfolio</em> weight (resolved via the
     * reference-account relationship) and the difference up to the account
     * weight is booked as a balancing deposit/removal. Taxes are preserved.
     */
    private void addSecurityRelatedAccountTx(Account account, ReadOnlyAccount pseudoAccount, AccountTransaction t,
                    Set<Security> usedSecurities, BigDecimal accountWeight, AccountTransaction.Type fallbackType,
                    Map<Account, Map<Security, Integer>> securityWeightIndex)
    {
        var security = t.getSecurity();

        if (security == null)
        {
            // general fee/tax/refund without a security -> scale by account
            // weight
            pseudoAccount.internalAddTransaction(scaled(t, accountWeight));
            return;
        }

        if (!usedSecurities.contains(security))
        {
            pseudoAccount.internalAddTransaction(convertTo(t, fallbackType, accountWeight));
            return;
        }

        var securityWeight = BigDecimal
                        .valueOf(resolveSecurityWeight(securityWeightIndex, account, security, getWeight(account)));

        // book the security-related part at the portfolio weight (keeps the
        // type, security reference and units including taxes)
        pseudoAccount.internalAddTransaction(scaled(t, securityWeight));

        long securityAmount = ClientFilterHelper.value(t.getAmount(), securityWeight);
        long accountAmount = ClientFilterHelper.value(t.getAmount(), accountWeight);

        long delta = accountAmount - securityAmount;
        if (delta != 0)
        {
            AccountTransaction.Type deltaType = delta > 0 ^ t.getType().isDebit() ? AccountTransaction.Type.DEPOSIT
                            : AccountTransaction.Type.REMOVAL;
            pseudoAccount.internalAddTransaction(new AccountTransaction(t.getDateTime(), t.getCurrencyCode(),
                            Math.abs(delta), null, deltaType));
        }
    }

    /**
     * Resolves the ownership weight to apply to a security-related transaction
     * that sits in {@code account}. Candidates are included portfolios whose
     * reference account is {@code account} and whose transaction history
     * contains the security; their highest weight is used as a conservative
     * heuristic. The candidates are pre-indexed in {@code securityWeightIndex}
     * during the portfolio pass. If there is no candidate, the account's own
     * weight is used as a defensive fallback.
     */
    private int resolveSecurityWeight(Map<Account, Map<Security, Integer>> securityWeightIndex, Account account,
                    Security security, int accountWeight)
    {
        var perSecurity = securityWeightIndex.get(account);
        if (perSecurity == null)
            return accountWeight;

        var weight = perSecurity.get(security);
        return weight == null ? accountWeight : weight.intValue();
    }

    /**
     * Returns the transaction scaled by the given weight, keeping its type,
     * security and units (taxes included). Returns the original transaction
     * unchanged at 100% so that the unweighted filter is byte-identical.
     */
    private PortfolioTransaction scaled(PortfolioTransaction t, BigDecimal weight)
    {
        if (weight.equals(Classification.ONE_HUNDRED_PERCENT_BD))
            return t;
        return convertTo(t, t.getType(), weight);
    }

    private AccountTransaction scaled(AccountTransaction t, BigDecimal weight)
    {
        if (weight.equals(Classification.ONE_HUNDRED_PERCENT_BD))
            return t;

        AccountTransaction clone = new AccountTransaction();
        clone.setType(t.getType());
        clone.setDateTime(t.getDateTime());
        clone.setCurrencyCode(t.getCurrencyCode());
        clone.setSecurity(t.getSecurity());
        clone.setAmount(ClientFilterHelper.value(t.getAmount(), weight));
        clone.setShares(ClientFilterHelper.value(t.getShares(), weight));
        t.getUnits().forEach(u -> clone.addUnit(ClientFilterHelper.value(u, weight)));
        return clone;
    }

    private PortfolioTransaction convertTo(PortfolioTransaction t, PortfolioTransaction.Type type, BigDecimal weight)
    {
        PortfolioTransaction clone = new PortfolioTransaction();
        clone.setType(type);
        clone.setDateTime(t.getDateTime());
        clone.setCurrencyCode(t.getCurrencyCode());
        clone.setSecurity(t.getSecurity());
        clone.setAmount(ClientFilterHelper.value(t.getAmount(), weight));
        clone.setShares(ClientFilterHelper.value(t.getShares(), weight));
        t.getUnits().forEach(u -> clone.addUnit(ClientFilterHelper.value(u, weight)));
        return clone;
    }

    private AccountTransaction convertTo(AccountTransaction t, AccountTransaction.Type type, BigDecimal weight)
    {
        AccountTransaction clone = new AccountTransaction();
        clone.setType(type);
        clone.setDateTime(t.getDateTime());
        clone.setCurrencyCode(t.getCurrencyCode());
        clone.setSecurity(null); // no security for REMOVAL or DEPOSIT
        clone.setAmount(ClientFilterHelper.value(t.getAmount(), weight));
        clone.setShares(ClientFilterHelper.value(t.getShares(), weight));

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
        result = prime * result + element2weight.hashCode();
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

        return accounts.equals(other.accounts) && portfolios.equals(other.portfolios)
                        && element2weight.equals(other.element2weight);
    }
}
