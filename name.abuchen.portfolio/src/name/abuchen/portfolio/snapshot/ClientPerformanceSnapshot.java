package name.abuchen.portfolio.snapshot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import name.abuchen.portfolio.Messages;
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

public class ClientPerformanceSnapshot
{
    public static class Position
    {
        private Money valuation;
        private String label;
        private Security security;

        public Position(Security security, Money valuation)
        {
            this.label = security.getName();
            this.valuation = valuation;
            this.security = security;
        }

        public Position(String label, Money valuation)
        {
            this.label = label;
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

        public Security getSecurity()
        {
            return security;
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
        INITIAL_VALUE, CAPITAL_GAINS, EARNINGS, FEES, TAXES, CURRENCY_GAINS, TRANSFERS, FINAL_VALUE
    }

    private final Client client;
    private final CurrencyConverter converter;
    private final ReportingPeriod period;
    private ClientSnapshot snapshotStart;
    private ClientSnapshot snapshotEnd;

    private final EnumMap<CategoryType, Category> categories = new EnumMap<>(CategoryType.class);
    private final List<TransactionPair<?>> earnings = new ArrayList<>();
    private final List<TransactionPair<?>> fees = new ArrayList<>();
    private final List<TransactionPair<?>> taxes = new ArrayList<>();
    private double irr;

    public ClientPerformanceSnapshot(Client client, CurrencyConverter converter, LocalDate startDate, LocalDate endDate)
    {
        this(client, converter, new ReportingPeriod.FromXtoY(startDate, endDate));
    }

    public ClientPerformanceSnapshot(Client client, CurrencyConverter converter, ReportingPeriod period)
    {
        this.client = client;
        this.converter = converter;
        this.period = period;
        this.snapshotStart = ClientSnapshot.create(client, converter, period.getStartDate());
        this.snapshotEnd = ClientSnapshot.create(client, converter, period.getEndDate());

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
                        new Category(String.format(Messages.ColumnInitialValue, snapshotStart.getTime()), "", //$NON-NLS-1$
                                        snapshotStart.getMonetaryAssets()));

        Money zero = Money.of(converter.getTermCurrency(), 0);

        categories.put(CategoryType.CAPITAL_GAINS, new Category(Messages.ColumnCapitalGains, "+", zero)); //$NON-NLS-1$
        categories.put(CategoryType.EARNINGS, new Category(Messages.ColumnEarnings, "+", zero)); //$NON-NLS-1$
        categories.put(CategoryType.FEES, new Category(Messages.ColumnPaidFees, "-", zero)); //$NON-NLS-1$
        categories.put(CategoryType.TAXES, new Category(Messages.ColumnPaidTaxes, "-", zero)); //$NON-NLS-1$
        categories.put(CategoryType.CURRENCY_GAINS, new Category(Messages.ColumnCurrencyGains, "+", zero)); //$NON-NLS-1$
        categories.put(CategoryType.TRANSFERS, new Category(Messages.ColumnTransfers, "+", zero)); //$NON-NLS-1$

        categories.put(CategoryType.FINAL_VALUE,
                        new Category(String.format(Messages.ColumnFinalValue, snapshotEnd.getTime()), "=", //$NON-NLS-1$
                                        snapshotEnd.getMonetaryAssets()));

        irr = ClientIRRYield.create(client, snapshotStart, snapshotEnd).getIrr();

        addCapitalGains();
        addEarnings();
        addCurrencyGains();
    }

    private void addCapitalGains()
    {
        Map<Security, MutableMoney> valuation = new HashMap<>();
        for (Security s : client.getSecurities())
            valuation.put(s, MutableMoney.of(converter.getTermCurrency()));

        snapshotStart.getJointPortfolio().getPositions().stream().forEach(p -> valuation.get(p.getInvestmentVehicle())
                        .subtract(p.calculateValue().with(converter.at(snapshotStart.getTime()))));

        for (PortfolioTransaction t : snapshotStart.getJointPortfolio().getSource().getTransactions())
        {
            if (!period.containsTransaction().test(t))
                continue;

            switch (t.getType())
            {
                case BUY:
                case DELIVERY_INBOUND:
                case TRANSFER_IN:
                    valuation.get(t.getSecurity()).subtract(t.getGrossValue().with(converter.at(t.getDate())));
                    break;
                case SELL:
                case DELIVERY_OUTBOUND:
                case TRANSFER_OUT:
                    valuation.get(t.getSecurity()).add(t.getGrossValue().with(converter.at(t.getDate())));
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        snapshotEnd.getJointPortfolio().getPositions().stream().forEach(p -> valuation.get(p.getInvestmentVehicle())
                        .add(p.calculateValue().with(converter.at(snapshotEnd.getTime()))));

        Category capitalGains = categories.get(CategoryType.CAPITAL_GAINS);

        // add securities w/ capital gains to the positions
        capitalGains.positions = valuation.entrySet().stream() //
                        .filter(entry -> !entry.getValue().isZero())
                        .map(entry -> new Position(entry.getKey(), entry.getValue().toMoney()))
                        .sorted((p1, p2) -> p1.getLabel().compareToIgnoreCase(p2.getLabel())) //
                        .collect(Collectors.toList());

        // total capital gains -> sum it up
        capitalGains.valuation = capitalGains.positions.stream() //
                        .map(p -> p.getValuation()) //
                        .collect(MoneyCollectors.sum(converter.getTermCurrency()));
    }

    private void addEarnings()
    {
        MutableMoney mEarnings = MutableMoney.of(converter.getTermCurrency());
        MutableMoney mOtherEarnings = MutableMoney.of(converter.getTermCurrency());
        MutableMoney mFees = MutableMoney.of(converter.getTermCurrency());
        MutableMoney mTaxes = MutableMoney.of(converter.getTermCurrency());
        MutableMoney mDeposits = MutableMoney.of(converter.getTermCurrency());
        MutableMoney mRemovals = MutableMoney.of(converter.getTermCurrency());

        Map<Security, MutableMoney> earningsBySecurity = new HashMap<>();

        for (Account account : client.getAccounts())
        {
            for (AccountTransaction t : account.getTransactions())
            {
                if (!period.containsTransaction().test(t))
                    continue;

                switch (t.getType())
                {
                    case DIVIDENDS:
                    case INTEREST:
                        addEarningTransaction(account, t, mEarnings, mOtherEarnings, mTaxes, earningsBySecurity);
                        break;
                    case INTEREST_CHARGE:
                        Money charged = t.getMonetaryAmount().with(converter.at(t.getDate()));
                        mEarnings.subtract(t.getMonetaryAmount().with(converter.at(t.getDate())));
                        earnings.add(new TransactionPair<AccountTransaction>(account, t));
                        mOtherEarnings.subtract(charged);
                        break;
                    case DEPOSIT:
                        mDeposits.add(t.getMonetaryAmount().with(converter.at(t.getDate())));
                        break;
                    case REMOVAL:
                        mRemovals.add(t.getMonetaryAmount().with(converter.at(t.getDate())));
                        break;
                    case FEES:
                        mFees.add(t.getMonetaryAmount().with(converter.at(t.getDate())));
                        fees.add(new TransactionPair<AccountTransaction>(account, t));
                        break;
                    case FEES_REFUND:
                        mFees.subtract(t.getMonetaryAmount().with(converter.at(t.getDate())));
                        fees.add(new TransactionPair<AccountTransaction>(account, t));
                        break;
                    case TAXES:
                        mTaxes.add(t.getMonetaryAmount().with(converter.at(t.getDate())));
                        taxes.add(new TransactionPair<AccountTransaction>(account, t));
                        break;
                    case TAX_REFUND:
                        mTaxes.subtract(t.getMonetaryAmount().with(converter.at(t.getDate())));
                        taxes.add(new TransactionPair<AccountTransaction>(account, t));
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
                if (!period.containsTransaction().test(t))
                    continue;

                Money unit = t.getUnitSum(Unit.Type.FEE, converter);
                if (!unit.isZero())
                {
                    mFees.add(unit);
                    fees.add(new TransactionPair<PortfolioTransaction>(portfolio, t));
                }

                unit = t.getUnitSum(Unit.Type.TAX, converter);
                if (!unit.isZero())
                {
                    mTaxes.add(unit);
                    taxes.add(new TransactionPair<PortfolioTransaction>(portfolio, t));
                }

                switch (t.getType())
                {
                    case DELIVERY_INBOUND:
                        mDeposits.add(t.getMonetaryAmount().with(converter.at(t.getDate())));
                        break;
                    case DELIVERY_OUTBOUND:
                        mRemovals.add(t.getMonetaryAmount().with(converter.at(t.getDate())));
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

        Category earningsCategory = categories.get(CategoryType.EARNINGS);
        earningsCategory.valuation = mEarnings.toMoney();
        earningsCategory.positions = earningsBySecurity.entrySet().stream()
                        //
                        .filter(entry -> !entry.getValue().isZero())
                        .map(entry -> new Position(entry.getKey(), entry.getValue().toMoney()))
                        .sorted((p1, p2) -> p1.getLabel().compareToIgnoreCase(p2.getLabel())) //
                        .collect(Collectors.toList());

        if (!mOtherEarnings.isZero())
            earningsCategory.positions.add(new Position(Messages.LabelInterest, mOtherEarnings.toMoney()));

        categories.get(CategoryType.FEES).valuation = mFees.toMoney();

        categories.get(CategoryType.TAXES).valuation = mTaxes.toMoney();

        categories.get(CategoryType.TRANSFERS).valuation = mDeposits.toMoney().subtract(mRemovals.toMoney());
        categories.get(CategoryType.TRANSFERS).positions.add(new Position(Messages.LabelDeposits, mDeposits.toMoney()));
        categories.get(CategoryType.TRANSFERS).positions.add(new Position(Messages.LabelRemovals, mRemovals.toMoney()));
    }

    private void addEarningTransaction(Account account, AccountTransaction transaction, MutableMoney mEarnings,
                    MutableMoney mOtherEarnings, MutableMoney mTaxes, Map<Security, MutableMoney> earningsBySecurity)
    {
        this.earnings.add(new TransactionPair<AccountTransaction>(account, transaction));

        Money tax = transaction.getUnitSum(Unit.Type.TAX, converter).with(converter.at(transaction.getDate()));
        Money earned = transaction.getGrossValue().with(converter.at(transaction.getDate()));

        mEarnings.add(earned);

        if (!tax.isZero())
        {
            mTaxes.add(tax);
            taxes.add(new TransactionPair<AccountTransaction>(account, transaction));
        }

        if (transaction.getSecurity() != null)
            earningsBySecurity.computeIfAbsent(transaction.getSecurity(),
                            k -> MutableMoney.of(converter.getTermCurrency())).add(earned);
        else
            mOtherEarnings.add(earned);
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
                if (!period.containsTransaction().test(t))
                    continue;

                switch (t.getType())
                {
                    case DIVIDENDS:
                    case INTEREST:
                    case DEPOSIT:
                    case TAX_REFUND:
                    case SELL:
                    case FEES_REFUND:
                        value.subtract(t.getMonetaryAmount().with(converter.at(t.getDate())));
                        break;
                    case REMOVAL:
                    case FEES:
                    case INTEREST_CHARGE:
                    case TAXES:
                    case BUY:
                        value.add(t.getMonetaryAmount().with(converter.at(t.getDate())));
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
            currencyGains.positions.add(new Position(currency, money.toMoney()));
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
        m.add(t.getMonetaryAmount().with(converter.at(t.getDate())));
        m.add(other.getMonetaryAmount().with(converter.at(t.getDate())));
        return m.divide(2).toMoney();
    }
}
