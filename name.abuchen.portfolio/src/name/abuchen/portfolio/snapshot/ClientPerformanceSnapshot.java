package name.abuchen.portfolio.snapshot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Adaptable;
import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MoneyCollectors;
import name.abuchen.portfolio.money.MutableMoney;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.security.CapitalGainsRecord;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceIndicator;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceRecord;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.trail.Trail;
import name.abuchen.portfolio.snapshot.trail.TrailProvider;
import name.abuchen.portfolio.snapshot.trail.TrailRecord;
import name.abuchen.portfolio.util.Interval;

public class ClientPerformanceSnapshot
{
    public static class Position implements TrailProvider, Adaptable
    {
        public static final String TRAIL_VALUE = "value"; //$NON-NLS-1$
        public static final String TRAIL_FOREX_GAIN = "forexGain"; //$NON-NLS-1$

        private final String label;
        private final Security security;
        private final Money value;
        private final TrailRecord valueTrail;
        private final Money forexGain;
        private final TrailRecord forexGainTrail;

        private Position(Security security, Money value, TrailRecord trail)
        {
            this(security.getName(), security, value, trail, null, null);
        }

        private Position(Security security, Money value, TrailRecord trail, Money forexGain, TrailRecord forexGainTrail)
        {
            this(security.getName(), security, value, trail, forexGain, forexGainTrail);
        }

        private Position(String label, Money value, TrailRecord trail)
        {
            this(label, null, value, trail, null, null);
        }

        private Position(String label, Security security, Money value, TrailRecord valueTrail, Money forexGain,
                        TrailRecord forexGainTrail)
        {
            this.label = label;
            this.security = security;

            this.value = value;
            this.valueTrail = valueTrail;

            this.forexGain = forexGain;
            this.forexGainTrail = forexGainTrail;
        }

        public Money getValue()
        {
            return value;
        }

        public String getLabel()
        {
            return label;
        }

        public Security getSecurity()
        {
            return security;
        }

        public Money getForexGain()
        {
            return forexGain;
        }

        @Override
        public Optional<Trail> explain(String key)
        {
            switch (key)
            {
                case TRAIL_VALUE:
                    return Trail.of(label, valueTrail);
                case TRAIL_FOREX_GAIN:
                    return Trail.of(label, forexGainTrail);
                default:
                    return Optional.empty();
            }
        }

        @Override
        public <T> T adapt(Class<T> type)
        {
            return Adaptor.adapt(type, security);
        }
    }

    public static class Category
    {
        private List<Position> positions = new ArrayList<>();

        private String label;
        private String sign;
        private Money valuation;

        public Category(String label, String sign, Money valuation)
        {
            this.label = label;
            this.sign = sign;
            this.valuation = valuation;
        }

        public Money getValuation()
        {
            return valuation;
        }

        public String getLabel()
        {
            return label;
        }

        public String getSign()
        {
            return sign;
        }

        public List<Position> getPositions()
        {
            return positions;
        }
    }

    public enum CategoryType
    {
        INITIAL_VALUE, CAPITAL_GAINS, REALIZED_CAPITAL_GAINS, EARNINGS, FEES, TAXES, CURRENCY_GAINS, TRANSFERS, FINAL_VALUE
    }

    private final Client client;
    private final CurrencyConverter converter;
    private final Interval period;
    private ClientSnapshot snapshotStart;
    private ClientSnapshot snapshotEnd;

    private final EnumMap<CategoryType, Category> categories = new EnumMap<>(CategoryType.class);
    private final List<TransactionPair<?>> earnings = new ArrayList<>();
    private final List<TransactionPair<?>> fees = new ArrayList<>();
    private final List<TransactionPair<?>> taxes = new ArrayList<>();
    private double irr;

    public ClientPerformanceSnapshot(Client client, CurrencyConverter converter, LocalDate startDate, LocalDate endDate)
    {
        this(client, converter, Interval.of(startDate, endDate));
    }

    public ClientPerformanceSnapshot(Client client, CurrencyConverter converter, Interval period)
    {
        this.client = client;
        this.converter = converter;
        this.period = period;
        this.snapshotStart = ClientSnapshot.create(client, converter, period.getStart());
        this.snapshotEnd = ClientSnapshot.create(client, converter, period.getEnd());

        calculate();
    }

    public Client getClient()
    {
        return client;
    }

    public ClientSnapshot getStartClientSnapshot()
    {
        return snapshotStart;
    }

    public ClientSnapshot getEndClientSnapshot()
    {
        return snapshotEnd;
    }

    public List<Category> getCategories()
    {
        return new ArrayList<>(categories.values());
    }

    public Category getCategoryByType(CategoryType type)
    {
        return categories.get(type);
    }

    public Money getValue(CategoryType categoryType)
    {
        return categories.get(categoryType).getValuation();
    }

    public List<TransactionPair<?>> getEarnings()
    {
        return earnings;
    }

    public List<TransactionPair<?>> getFees()
    {
        return fees;
    }

    public List<TransactionPair<?>> getTaxes()
    {
        return taxes;
    }

    public double getPerformanceIRR()
    {
        return irr;
    }

    public Money getAbsoluteDelta()
    {
        MutableMoney delta = MutableMoney.of(converter.getTermCurrency());

        for (Map.Entry<CategoryType, Category> entry : categories.entrySet())
        {
            switch (entry.getKey())
            {
                case CAPITAL_GAINS:
                case REALIZED_CAPITAL_GAINS:
                case EARNINGS:
                case CURRENCY_GAINS:
                    delta.add(entry.getValue().getValuation());
                    break;
                case FEES:
                case TAXES:
                    delta.subtract(entry.getValue().getValuation());
                    break;
                default:
                    break;
            }
        }

        return delta.toMoney();
    }

    private void calculate()
    {
        categories.put(CategoryType.INITIAL_VALUE,
                        new Category(String.format(Messages.ColumnInitialValue,
                                        Values.Date.format(snapshotStart.getTime())), "", //$NON-NLS-1$
                                        snapshotStart.getMonetaryAssets()));

        Money zero = Money.of(converter.getTermCurrency(), 0);

        categories.put(CategoryType.CAPITAL_GAINS, new Category(Messages.ColumnCapitalGains, "+", zero)); //$NON-NLS-1$
        categories.put(CategoryType.REALIZED_CAPITAL_GAINS,
                        new Category(Messages.LabelRealizedCapitalGains, "+", zero)); //$NON-NLS-1$
        categories.put(CategoryType.EARNINGS, new Category(Messages.ColumnEarnings, "+", zero)); //$NON-NLS-1$
        categories.put(CategoryType.FEES, new Category(Messages.ColumnPaidFees, "-", zero)); //$NON-NLS-1$
        categories.put(CategoryType.TAXES, new Category(Messages.ColumnPaidTaxes, "-", zero)); //$NON-NLS-1$
        categories.put(CategoryType.CURRENCY_GAINS, new Category(Messages.ColumnCurrencyGains, "+", zero)); //$NON-NLS-1$
        categories.put(CategoryType.TRANSFERS, new Category(Messages.ColumnTransfers, "+", zero)); //$NON-NLS-1$

        categories.put(CategoryType.FINAL_VALUE,
                        new Category(String.format(Messages.ColumnFinalValue,
                                        Values.Date.format(snapshotEnd.getTime())), "=", //$NON-NLS-1$
                                        snapshotEnd.getMonetaryAssets()));

        irr = ClientIRRYield.create(client, snapshotStart, snapshotEnd).getIrr();

        addCapitalGains();
        addEarnings();
        addCurrencyGains();
    }

    /**
     * Calculates realized and unrealized capital gains using the FIFO method.
     * If the security is traded in forex then additionally the currency gains
     * are calculated, i.e. the change in value if the investment would have
     * been in cash in the foreign currency.
     */
    private void addCapitalGains()
    {
        SecurityPerformanceSnapshot securityPerformance = SecurityPerformanceSnapshot.create(client, converter, period,
                        snapshotStart, snapshotEnd, SecurityPerformanceIndicator.CapitalGains.class);

        Category realizedCapitalGains = categories.get(CategoryType.REALIZED_CAPITAL_GAINS);
        addCapitalGains(realizedCapitalGains, securityPerformance, record -> record.getRealizedCapitalGains());

        // create position for unrealized capital gains

        Category capitalGains = categories.get(CategoryType.CAPITAL_GAINS);
        addCapitalGains(capitalGains, securityPerformance, record -> record.getUnrealizedCapitalGains());
    }

    private void addCapitalGains(Category category, SecurityPerformanceSnapshot securityPerformance,
                    Function<SecurityPerformanceRecord, CapitalGainsRecord> mapper)
    {
        category.positions = securityPerformance.getRecords().stream()
                        .sorted((p1, p2) -> p1.getSecurityName().compareToIgnoreCase(p2.getSecurityName())) //
                        .map(mapper)
                        .filter(gains -> !gains.getCapitalGains().isZero() || !gains.getForexCaptialGains().isZero())
                        .map(gains -> new Position(gains.getSecurity(), gains.getCapitalGains(),
                                        gains.getCapitalGainsTrail(), gains.getForexCaptialGains(),
                                        gains.getForexCapitalGainsTrail())) //
                        .collect(Collectors.toList());

        category.valuation = category.positions.stream() //
                        .map(Position::getValue) //
                        .collect(MoneyCollectors.sum(converter.getTermCurrency()));
    }

    private void addEarnings()
    {
        String termCurrency = converter.getTermCurrency();

        MutableMoney mEarnings = MutableMoney.of(termCurrency);
        MutableMoney mFees = MutableMoney.of(termCurrency);
        MutableMoney mTaxes = MutableMoney.of(termCurrency);
        MutableMoney mDeposits = MutableMoney.of(termCurrency);
        MutableMoney mRemovals = MutableMoney.of(termCurrency);

        Map<Security, MutableMoney> earningsBySecurity = new HashMap<>();
        Map<Security, MutableMoney> feesBySecurity = new HashMap<>();
        Map<Security, MutableMoney> taxesBySecurity = new HashMap<>();

        for (Account account : client.getAccounts())
        {
            for (AccountTransaction t : account.getTransactions())
            {
                if (!period.contains(t.getDateTime()))
                    continue;

                Money value = t.getMonetaryAmount().with(converter.at(t.getDateTime()));

                switch (t.getType())
                {
                    case DIVIDENDS:
                    case INTEREST:
                        addEarningTransaction(account, t, mEarnings, earningsBySecurity, mFees, mTaxes, feesBySecurity,
                                        taxesBySecurity);
                        break;
                    case INTEREST_CHARGE:
                        mEarnings.subtract(value);
                        earnings.add(new TransactionPair<AccountTransaction>(account, t));
                        earningsBySecurity.computeIfAbsent(null, s -> MutableMoney.of(termCurrency)).subtract(value);
                        break;
                    case DEPOSIT:
                        mDeposits.add(value);
                        break;
                    case REMOVAL:
                        mRemovals.add(value);
                        break;
                    case FEES:
                        mFees.add(value);
                        fees.add(new TransactionPair<AccountTransaction>(account, t));
                        feesBySecurity.computeIfAbsent(t.getSecurity(), s -> MutableMoney.of(termCurrency)).add(value);
                        break;
                    case FEES_REFUND:
                        mFees.subtract(value);
                        fees.add(new TransactionPair<AccountTransaction>(account, t));
                        feesBySecurity.computeIfAbsent(t.getSecurity(), s -> MutableMoney.of(termCurrency))
                                        .subtract(value);
                        break;
                    case TAXES:
                        mTaxes.add(value);
                        taxes.add(new TransactionPair<AccountTransaction>(account, t));
                        taxesBySecurity.computeIfAbsent(t.getSecurity(), s -> MutableMoney.of(termCurrency)).add(value);
                        break;
                    case TAX_REFUND:
                        mTaxes.subtract(value);
                        taxes.add(new TransactionPair<AccountTransaction>(account, t));
                        taxesBySecurity.computeIfAbsent(t.getSecurity(), s -> MutableMoney.of(termCurrency))
                                        .subtract(value);
                        break;
                    case BUY:
                    case SELL:
                    case TRANSFER_IN:
                    case TRANSFER_OUT:
                        // no operation
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            }
        }

        for (Portfolio portfolio : client.getPortfolios())
        {
            for (PortfolioTransaction t : portfolio.getTransactions())
            {
                if (!period.contains(t.getDateTime()))
                    continue;

                Money unit = t.getUnitSum(Unit.Type.FEE, converter);
                if (!unit.isZero())
                {
                    mFees.add(unit);
                    fees.add(new TransactionPair<PortfolioTransaction>(portfolio, t));
                    feesBySecurity.computeIfAbsent(t.getSecurity(), s -> MutableMoney.of(termCurrency)).add(unit);
                }

                unit = t.getUnitSum(Unit.Type.TAX, converter);
                if (!unit.isZero())
                {
                    mTaxes.add(unit);
                    taxes.add(new TransactionPair<PortfolioTransaction>(portfolio, t));
                    taxesBySecurity.computeIfAbsent(t.getSecurity(), s -> MutableMoney.of(termCurrency)).add(unit);
                }

                switch (t.getType())
                {
                    case DELIVERY_INBOUND:
                        mDeposits.add(t.getMonetaryAmount().with(converter.at(t.getDateTime())));
                        break;
                    case DELIVERY_OUTBOUND:
                        mRemovals.add(t.getMonetaryAmount().with(converter.at(t.getDateTime())));
                        break;
                    case BUY:
                    case SELL:
                    case TRANSFER_IN:
                    case TRANSFER_OUT:
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            }
        }

        BiFunction<Map<Security, MutableMoney>, String, List<Position>> asPositions = (map, otherLabel) -> map
                        .entrySet().stream() //
                        .filter(entry -> !entry.getValue().isZero()) //
                        .map(entry -> entry.getKey() == null
                                        ? new Position(otherLabel, entry.getValue().toMoney(), null)
                                        : new Position(entry.getKey(), entry.getValue().toMoney(), null))
                        .sorted((p1, p2) -> {
                            if (p1.getSecurity() == null)
                                return p2.getSecurity() == null ? 0 : 1;
                            if (p2.getSecurity() == null)
                                return -1;
                            return p1.getLabel().compareToIgnoreCase(p2.getLabel());
                        }) //
                        .collect(Collectors.toList());

        Category earningsCategory = categories.get(CategoryType.EARNINGS);
        earningsCategory.valuation = mEarnings.toMoney();
        earningsCategory.positions = asPositions.apply(earningsBySecurity, Messages.LabelInterest);

        categories.get(CategoryType.FEES).valuation = mFees.toMoney();
        categories.get(CategoryType.FEES).positions = asPositions.apply(feesBySecurity, Messages.LabelOtherCategory);

        categories.get(CategoryType.TAXES).valuation = mTaxes.toMoney();
        categories.get(CategoryType.TAXES).positions = asPositions.apply(taxesBySecurity, Messages.LabelOtherCategory);

        categories.get(CategoryType.TRANSFERS).valuation = mDeposits.toMoney().subtract(mRemovals.toMoney());
        categories.get(CategoryType.TRANSFERS).positions
                        .add(new Position(Messages.LabelDeposits, mDeposits.toMoney(), null));
        categories.get(CategoryType.TRANSFERS).positions
                        .add(new Position(Messages.LabelRemovals, mRemovals.toMoney(), null));
    }

    private void addEarningTransaction(Account account, AccountTransaction transaction, MutableMoney mEarnings,
                    Map<Security, MutableMoney> earningsBySecurity, MutableMoney mFees, MutableMoney mTaxes,
                    Map<Security, MutableMoney> feesBySecurity, Map<Security, MutableMoney> taxesBySecurity)
    {
        Money earned = transaction.getGrossValue().with(converter.at(transaction.getDateTime()));
        mEarnings.add(earned);
        this.earnings.add(new TransactionPair<AccountTransaction>(account, transaction));
        earningsBySecurity.computeIfAbsent(transaction.getSecurity(), k -> MutableMoney.of(converter.getTermCurrency()))
                        .add(earned);

        Money fee = transaction.getUnitSum(Unit.Type.FEE, converter).with(converter.at(transaction.getDateTime()));
        if (!fee.isZero())
        {
            mFees.add(fee);
            this.fees.add(new TransactionPair<AccountTransaction>(account, transaction));
            feesBySecurity.computeIfAbsent(transaction.getSecurity(), s -> MutableMoney.of(converter.getTermCurrency()))
                            .add(fee);
        }

        Money tax = transaction.getUnitSum(Unit.Type.TAX, converter).with(converter.at(transaction.getDateTime()));
        if (!tax.isZero())
        {
            mTaxes.add(tax);
            this.taxes.add(new TransactionPair<AccountTransaction>(account, transaction));
            taxesBySecurity.computeIfAbsent(transaction.getSecurity(),
                            s -> MutableMoney.of(converter.getTermCurrency())).add(tax);
        }
    }

    private void addCurrencyGains()
    {
        Map<String, MutableMoney> currency2money = new HashMap<>();

        for (AccountSnapshot snapshot : snapshotStart.getAccounts())
        {
            if (converter.getTermCurrency().equals(snapshot.getAccount().getCurrencyCode()))
                continue;

            MutableMoney value = currency2money.computeIfAbsent(snapshot.getAccount().getCurrencyCode(),
                            c -> MutableMoney.of(converter.getTermCurrency()));

            // subtract initial values
            value.subtract(snapshot.getFunds());

            // add and subtract transactions
            for (AccountTransaction t : snapshot.getAccount().getTransactions())
            {
                if (!period.contains(t.getDateTime()))
                    continue;

                switch (t.getType())
                {
                    case DIVIDENDS:
                    case INTEREST:
                    case DEPOSIT:
                    case TAX_REFUND:
                    case SELL:
                    case FEES_REFUND:
                        value.subtract(t.getMonetaryAmount().with(converter.at(t.getDateTime())));
                        break;
                    case REMOVAL:
                    case FEES:
                    case INTEREST_CHARGE:
                    case TAXES:
                    case BUY:
                        value.add(t.getMonetaryAmount().with(converter.at(t.getDateTime())));
                        break;
                    case TRANSFER_IN:
                        value.subtract(determineTransferAmount(t));
                        break;
                    case TRANSFER_OUT:
                        value.add(determineTransferAmount(t));
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            }
        }

        // add final values (if in foreign currency)
        for (AccountSnapshot snapshot : snapshotEnd.getAccounts())
        {
            if (converter.getTermCurrency().equals(snapshot.getAccount().getCurrencyCode()))
                continue;

            currency2money.computeIfAbsent(snapshot.getAccount().getCurrencyCode(),
                            c -> MutableMoney.of(converter.getTermCurrency())) //
                            .add(snapshot.getFunds());
        }

        Category currencyGains = categories.get(CategoryType.CURRENCY_GAINS);
        currency2money.forEach((currency, money) -> {
            currencyGains.valuation = currencyGains.valuation.add(money.toMoney());
            currencyGains.positions.add(new Position(currency, money.toMoney(), null));
        });
        Collections.sort(currencyGains.positions, (p1, p2) -> p1.getLabel().compareTo(p2.getLabel()));
    }

    /**
     * Determine the monetary amount when transferring cash between accounts.
     * Because the actual exchange rate of the transferal might differ from the
     * historical rate given by the exchange rate provider (e.g. ECB), we would
     * get rounding differences if we do not take the original amount. If the
     * transferal does not involve the term currency at all, we calculate the
     * average value out of both converted amounts.
     */
    private Money determineTransferAmount(AccountTransaction t)
    {
        if (converter.getTermCurrency().equals(t.getCurrencyCode()))
            return t.getMonetaryAmount();

        Transaction other = t.getCrossEntry().getCrossTransaction(t);
        if (converter.getTermCurrency().equals(other.getCurrencyCode()))
            return other.getMonetaryAmount();

        MutableMoney m = MutableMoney.of(converter.getTermCurrency());
        m.add(t.getMonetaryAmount().with(converter.at(t.getDateTime())));
        m.add(other.getMonetaryAmount().with(converter.at(t.getDateTime())));
        return m.divide(2).toMoney();
    }
}
