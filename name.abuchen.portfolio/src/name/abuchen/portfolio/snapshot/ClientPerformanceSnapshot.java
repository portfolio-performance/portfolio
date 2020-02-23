package name.abuchen.portfolio.snapshot;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
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
import name.abuchen.portfolio.snapshot.trail.Trail;
import name.abuchen.portfolio.snapshot.trail.TrailProvider;
import name.abuchen.portfolio.snapshot.trail.TrailRecord;
import name.abuchen.portfolio.util.Interval;

public class ClientPerformanceSnapshot
{
    public static class Position implements TrailProvider
    {
        public static final String TRAIL_VALUE = "value"; //$NON-NLS-1$

        private final String label;
        private final Security security;
        private final Money value;
        private final Money forexGain;

        private final TrailRecord trail;

        private Position(Security security, Money value, TrailRecord trail)
        {
            this(security.getName(), security, value, null, trail);
        }

        private Position(Security security, Money value, Money forexGain, TrailRecord trail)
        {
            this(security.getName(), security, value, forexGain, trail);
        }

        private Position(String label, Money value, TrailRecord trail)
        {
            this(label, null, value, null, trail);
        }

        private Position(String label, Security security, Money value, Money forexGain, TrailRecord trail)
        {
            this.label = label;
            this.security = security;
            this.value = value;
            this.forexGain = forexGain;

            this.trail = trail;
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
            return TRAIL_VALUE.equals(key) && trail != null ? Optional.of(new Trail(label, trail)) : Optional.empty();
        }

        public Position combine(Position other)
        {
            if (!Objects.equals(security, other.security))
                throw new IllegalArgumentException();

            return new Position(security, //
                            value.add(other.value), //
                            forexGain.add(other.forexGain), //
                            trail.add(other.trail));
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

    private static class LineItem
    {
        private long shares;
        private LocalDate date;
        private long value;

        private final TrailRecord trail;

        /**
         * Holds the original number of shares (of the transaction). The
         * original shares are needed to calculate fractions if the transaction
         * is split up multiple times
         */
        private final long originalShares;

        public LineItem(long shares, LocalDate date, long value, TrailRecord trail)
        {
            this.shares = shares;
            this.date = Objects.requireNonNull(date);
            this.value = value;
            this.trail = trail;
            this.originalShares = shares;
        }
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
        String termCurrency = converter.getTermCurrency();
        Money zero = Money.of(termCurrency, 0);

        Map<Security, List<LineItem>> security2fifo = new HashMap<>();
        Map<Security, Position> security2realizedGain = new HashMap<>();

        for (Security s : client.getSecurities())
        {
            security2fifo.put(s, new ArrayList<>());
            security2realizedGain.put(s, new Position(s, zero, zero, TrailRecord.empty()));
        }

        snapshotStart.getJointPortfolio().getPositions().stream().forEach(p -> {

            Money value = p.calculateValue();
            Money converted = value.with(converter.at(snapshotStart.getTime()));

            TrailRecord trail = TrailRecord.ofSnapshot(snapshotStart, p);
            if (!value.getCurrencyCode().equals(converter.getTermCurrency()))
                trail = trail.convert(converted, converter.getRate(snapshotStart.getTime(), value.getCurrencyCode()));

            security2fifo.get(p.getInvestmentVehicle())
                            .add(new LineItem(p.getShares(), snapshotStart.getTime(), converted.getAmount(), trail));
        });

        // sort transactions to prepare for FIFO calculation
        List<PortfolioTransaction> tx = snapshotStart.getJointPortfolio().getSource().getTransactions();
        tx.sort(new Transaction.ByDate());

        for (PortfolioTransaction t : tx)
        {
            if (!period.contains(t.getDateTime()))
                continue;

            if (t.getType() == PortfolioTransaction.Type.TRANSFER_IN
                            || t.getType() == PortfolioTransaction.Type.TRANSFER_OUT)
                continue;

            Money grossValue = t.getGrossValue();
            Money convertedGrossValue = grossValue.with(converter.at(t.getDateTime()));

            TrailRecord trail = TrailRecord.ofTransaction(t).asGrossValue(grossValue);
            if (!grossValue.getCurrencyCode().equals(converter.getTermCurrency()))
                trail = trail.convert(convertedGrossValue,
                                converter.getRate(snapshotStart.getTime(), grossValue.getCurrencyCode()));

            switch (t.getType())
            {
                case BUY:
                case DELIVERY_INBOUND:
                    security2fifo.get(t.getSecurity()).add(new LineItem(t.getShares(), t.getDateTime().toLocalDate(),
                                    convertedGrossValue.getAmount(), trail));
                    break;

                case SELL:
                case DELIVERY_OUTBOUND:

                    long value = convertedGrossValue.getAmount();
                    List<LineItem> fifo = security2fifo.get(t.getSecurity());

                    long sold = t.getShares();

                    for (LineItem item : fifo) // NOSONAR
                    {
                        if (item.shares == 0)
                            continue;

                        if (sold <= 0)
                            break;

                        long soldShares = Math.min(sold, item.shares);
                        long start = Math.round((double) soldShares / item.shares * item.value);
                        long end = Math.round((double) soldShares / t.getShares() * value);

                        long forexGain = 0L;

                        if (!termCurrency.equals(t.getSecurity().getCurrencyCode()))
                        {
                            // calculate currency gains (if the security is
                            // traded in forex) by converting the start value to
                            // forex and converting it back with the exchange
                            // rate at the end of the period (equivalent to
                            // holding the money as cash in forex currency)

                            forexGain = converter.with(t.getSecurity().getCurrencyCode())
                                            .convert(item.date, Money.of(termCurrency, start))
                                            .with(converter.at(t.getDateTime())).getAmount() - start;
                        }

                        Position p = new Position(t.getSecurity(), //
                                        Money.of(termCurrency, end - start), //
                                        Money.of(termCurrency, forexGain), //
                                        trail.fraction(Money.of(termCurrency, end), soldShares, t.getShares())
                                                        .substract(item.trail.fraction(Money.of(termCurrency, start),
                                                                        soldShares, item.originalShares)));

                        security2realizedGain.put(t.getSecurity(),
                                        security2realizedGain.get(t.getSecurity()).combine(p));

                        item.shares -= soldShares;
                        item.value -= start;

                        sold -= soldShares;
                    }

                    if (sold > 0)
                    {
                        // Report that more was sold than bought to log
                        PortfolioLog.warning(MessageFormat.format(Messages.MsgNegativeHoldingsDuringFIFOCostCalculation,
                                        Values.Share.format(sold), t.getSecurity().getName(),
                                        Values.DateTime.format(t.getDateTime())));
                    }

                    break;

                case TRANSFER_OUT:
                case TRANSFER_IN:
                default:
                    throw new UnsupportedOperationException();
            }
        }

        // create positions for realized capital gains

        Category realizedCapitalGains = categories.get(CategoryType.REALIZED_CAPITAL_GAINS);

        realizedCapitalGains.positions = security2realizedGain.values().stream() //
                        .filter(p -> !p.getValue().isZero()) //
                        .sorted((p1, p2) -> p1.getLabel().compareToIgnoreCase(p2.getLabel())) //
                        .collect(Collectors.toList());

        realizedCapitalGains.valuation = realizedCapitalGains.positions.stream() //
                        .map(Position::getValue) //
                        .collect(MoneyCollectors.sum(termCurrency));

        // create position for unrealized capital gains

        Category capitalGains = categories.get(CategoryType.CAPITAL_GAINS);

        Map<Security, SecurityPosition> security2end = snapshotEnd.getJointPortfolio().getPositionsBySecurity();

        capitalGains.positions = security2fifo.entrySet().stream() //
                        .filter(entry -> !entry.getValue().isEmpty()) //
                        .map(entry -> {
                            long start = entry.getValue().stream().mapToLong(item -> item.value).sum();

                            SecurityPosition positionAtEnd = security2end.get(entry.getKey());

                            if (start == 0L && positionAtEnd == null)
                                return new Position(entry.getKey(), zero, TrailRecord.empty());

                            if (start != 0L && positionAtEnd == null)
                            {
                                PortfolioLog.warning(MessageFormat.format(
                                                Messages.MsgNegativeHoldingsDuringFIFOCostCalculation,
                                                Values.Money.format(Money.of(termCurrency, start)),
                                                entry.getKey().getName(),
                                                entry.getValue().stream().map(item -> Values.Date.format(item.date))
                                                                .collect(Collectors.joining(",")))); //$NON-NLS-1$
                                return new Position(entry.getKey(), zero, TrailRecord.empty());
                            }

                            Money endValue = positionAtEnd.calculateValue();
                            Money convertedEndValue = endValue.with(converter.at(snapshotEnd.getTime()));

                            long end = convertedEndValue.getAmount();
                            long forexGain = 0L;

                            if (!termCurrency.equals(entry.getKey().getCurrencyCode()))
                            {
                                // calculate forex gains: use exchange rate of
                                // each date of investment

                                CurrencyConverter toForex = converter.with(entry.getKey().getCurrencyCode());

                                forexGain = entry.getValue().stream().filter(item -> item.value != 0)
                                                .map(item -> toForex.convert(item.date,
                                                                Money.of(termCurrency, item.value)))
                                                .collect(MoneyCollectors.sum(entry.getKey().getCurrencyCode()))
                                                .with(converter.at(snapshotEnd.getTime())).getAmount() - start;
                            }

                            // build trail

                            TrailRecord trail = TrailRecord.ofSnapshot(snapshotEnd, positionAtEnd);
                            if (!endValue.getCurrencyCode().equals(converter.getTermCurrency()))
                                trail = trail.convert(convertedEndValue,
                                                converter.getRate(snapshotEnd.getTime(), endValue.getCurrencyCode()));

                            trail = trail.substract(TrailRecord.of(entry.getValue().stream()
                                            .filter(item -> item.shares > 0)
                                            .map(item -> item.trail.fraction(Money.of(termCurrency, item.value),
                                                            item.shares, item.originalShares))
                                            .collect(Collectors.toList())));

                            return new Position(entry.getKey(), //
                                            Money.of(termCurrency, end - start), //
                                            Money.of(termCurrency, forexGain), //
                                            trail);
                        }) //
                        .filter(p -> !p.getValue().isZero())
                        .sorted((p1, p2) -> p1.getLabel().compareToIgnoreCase(p2.getLabel())) //
                        .collect(Collectors.toList());

        // total capital gains -> sum it up
        capitalGains.valuation = capitalGains.positions.stream() //
                        .map(Position::getValue) //
                        .collect(MoneyCollectors.sum(termCurrency));
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
                        addEarningTransaction(account, t, mEarnings, earningsBySecurity, mTaxes, taxesBySecurity);
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
                    Map<Security, MutableMoney> earningsBySecurity, MutableMoney mTaxes,
                    Map<Security, MutableMoney> taxesBySecurity)
    {
        Money earned = transaction.getGrossValue().with(converter.at(transaction.getDateTime()));
        mEarnings.add(earned);
        this.earnings.add(new TransactionPair<AccountTransaction>(account, transaction));
        earningsBySecurity.computeIfAbsent(transaction.getSecurity(), k -> MutableMoney.of(converter.getTermCurrency()))
                        .add(earned);

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
