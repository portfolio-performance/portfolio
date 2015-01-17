package name.abuchen.portfolio.snapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
        private List<Position> positions = new ArrayList<Position>();

        private String label;
        private Money valuation;

        public Category(String label, Money valuation)
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
    private EnumMap<CategoryType, Category> categories;
    private List<Transaction> earnings;
    private double irr;
    private PerformanceIndex performanceIndex;

    public ClientPerformanceSnapshot(Client client, CurrencyConverter converter, Date startDate, Date endDate)
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
        this.categories = new EnumMap<CategoryType, Category>(CategoryType.class);
        this.earnings = new ArrayList<Transaction>();

        calculate();
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
        return new ArrayList<Category>(categories.values());
    }

    public Category getCategoryByType(CategoryType type)
    {
        return categories.get(type);
    }

    public Money getValue(CategoryType categoryType)
    {
        return categories.get(categoryType).getValuation();
    }

    public List<Transaction> getEarnings()
    {
        return earnings;
    }

    public PerformanceIndex getPerformanceIndex()
    {
        return performanceIndex;
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
                    delta.substract(entry.getValue().getValuation());
                    break;
                default:
                    break;
            }
        }

        return delta.toMoney();
    }

    /* package */EnumMap<CategoryType, Category> getCategoryMap()
    {
        return categories;
    }

    private void calculate()
    {
        categories.put(CategoryType.INITIAL_VALUE,
                        new Category(String.format(Messages.ColumnInitialValue, snapshotStart.getTime()), //
                                        snapshotStart.getMonetaryAssets()));

        Money zero = Money.of(converter.getTermCurrency(), 0);

        categories.put(CategoryType.CAPITAL_GAINS, new Category(Messages.ColumnCapitalGains, zero));
        categories.put(CategoryType.EARNINGS, new Category(Messages.ColumnEarnings, zero));
        categories.put(CategoryType.FEES, new Category(Messages.ColumnPaidFees, zero));
        categories.put(CategoryType.TAXES, new Category(Messages.ColumnPaidTaxes, zero));
        categories.put(CategoryType.CURRENCY_GAINS, new Category(Messages.ColumnCurrencyGains, zero));
        categories.put(CategoryType.TRANSFERS, new Category(Messages.ColumnTransfers, zero));

        categories.put(CategoryType.FINAL_VALUE,
                        new Category(String.format(Messages.ColumnFinalValue, snapshotEnd.getTime()), //
                                        snapshotEnd.getMonetaryAssets()));

        irr = ClientIRRYield.create(client, snapshotStart, snapshotEnd).getIrr();

        performanceIndex = PerformanceIndex.forClient(client, converter,
                        new ReportingPeriod.FromXtoY(snapshotStart.getTime(), snapshotEnd.getTime()),
                        new ArrayList<Exception>());

        addCapitalGains();
        addEarnings();
        addCurrencyGains();
    }

    private void addCapitalGains()
    {
        Map<Security, MutableMoney> valuation = new HashMap<Security, MutableMoney>();
        for (Security s : client.getSecurities())
            valuation.put(s, MutableMoney.of(converter.getTermCurrency()));

        snapshotStart.getJointPortfolio()
                        .getPositions()
                        .stream()
                        .forEach(p -> valuation.get(p.getInvestmentVehicle()).substract(
                                        p.calculateValue().with(converter.at(snapshotStart.getTime()))));

        for (PortfolioTransaction t : snapshotStart.getJointPortfolio().getSource().getTransactions())
        {
            if (!period.containsTransaction().test(t))
                continue;

            switch (t.getType())
            {
                case BUY:
                case DELIVERY_INBOUND:
                case TRANSFER_IN:
                    valuation.get(t.getSecurity()).substract(t.getLumpSum().with(converter.at(t.getDate())));
                    break;
                case SELL:
                case DELIVERY_OUTBOUND:
                case TRANSFER_OUT:
                    valuation.get(t.getSecurity()).add(t.getLumpSum().with(converter.at(t.getDate())));
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        snapshotEnd.getJointPortfolio()
                        .getPositions()
                        .stream()
                        .forEach(p -> valuation.get(p.getInvestmentVehicle()).add(
                                        p.calculateValue().with(converter.at(snapshotEnd.getTime()))));

        Category capitalGains = categories.get(CategoryType.CAPITAL_GAINS);

        // add securities w/ capital gains to the positions
        capitalGains.positions = valuation.entrySet()
                        .stream()
                        //
                        .filter(entry -> !entry.getValue().isZero())
                        .map(entry -> new Position(entry.getKey(), entry.getValue().toMoney()))
                        .sorted((p1, p2) -> p1.getLabel().compareTo(p2.getLabel())) //
                        .collect(Collectors.toList());

        // total capital gains -> sum it up
        capitalGains.valuation = capitalGains.positions.stream() //
                        .map(p -> p.getValuation()) //
                        .collect(MoneyCollectors.sum(converter.getTermCurrency()));
    }

    private void addEarnings()
    {
        MutableMoney earnings = MutableMoney.of(converter.getTermCurrency());
        MutableMoney otherEarnings = MutableMoney.of(converter.getTermCurrency());
        MutableMoney fees = MutableMoney.of(converter.getTermCurrency());
        MutableMoney taxes = MutableMoney.of(converter.getTermCurrency());
        MutableMoney deposits = MutableMoney.of(converter.getTermCurrency());
        MutableMoney removals = MutableMoney.of(converter.getTermCurrency());

        Map<Security, MutableMoney> earningsBySecurity = new HashMap<Security, MutableMoney>();

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
                        this.earnings.add(t);
                        earnings.add(t.getMonetaryAmount().with(converter.at(t.getDate())));
                        if (t.getSecurity() != null)
                        {
                            earningsBySecurity.computeIfAbsent(t.getSecurity(),
                                            k -> MutableMoney.of(converter.getTermCurrency())) //
                                            .add(t.getMonetaryAmount().with(converter.at(t.getDate())));
                        }
                        else
                        {
                            otherEarnings.add(t.getMonetaryAmount().with(converter.at(t.getDate())));
                        }
                        break;
                    case DEPOSIT:
                        deposits.add(t.getMonetaryAmount().with(converter.at(t.getDate())));
                        break;
                    case REMOVAL:
                        removals.add(t.getMonetaryAmount().with(converter.at(t.getDate())));
                        break;
                    case FEES:
                        fees.add(t.getMonetaryAmount().with(converter.at(t.getDate())));
                        break;
                    case TAXES:
                        taxes.add(t.getMonetaryAmount().with(converter.at(t.getDate())));
                        break;
                    case TAX_REFUND:
                        taxes.substract(t.getMonetaryAmount().with(converter.at(t.getDate())));
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

                switch (t.getType())
                {
                    case DELIVERY_INBOUND:
                        deposits.add(t.getMonetaryAmount().with(converter.at(t.getDate())));
                        break;
                    case DELIVERY_OUTBOUND:
                        removals.add(t.getMonetaryAmount().with(converter.at(t.getDate())));
                        break;
                    case BUY:
                    case SELL:
                    case TRANSFER_IN:
                    case TRANSFER_OUT:
                        fees.add(t.getMonetaryFees().with(converter.at(t.getDate())));
                        taxes.add(t.getMonetaryTaxes().with(converter.at(t.getDate())));
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            }
        }

        Category earningsCategory = categories.get(CategoryType.EARNINGS);
        earningsCategory.valuation = earnings.toMoney();
        earningsCategory.positions = earningsBySecurity.entrySet()
                        .stream()
                        //
                        .filter(entry -> !entry.getValue().isZero())
                        .map(entry -> new Position(entry.getKey(), entry.getValue().toMoney()))
                        .sorted((p1, p2) -> p1.getLabel().compareTo(p2.getLabel())) //
                        .collect(Collectors.toList());

        if (!otherEarnings.isZero())
            earningsCategory.positions.add(new Position(Messages.LabelInterest, otherEarnings.toMoney()));

        categories.get(CategoryType.FEES).valuation = fees.toMoney();

        categories.get(CategoryType.TAXES).valuation = taxes.toMoney();

        categories.get(CategoryType.TRANSFERS).valuation = deposits.substract(removals).toMoney();
        categories.get(CategoryType.TRANSFERS).positions.add(new Position(Messages.LabelDeposits, deposits.toMoney()));
        categories.get(CategoryType.TRANSFERS).positions.add(new Position(Messages.LabelRemovals, removals.toMoney()));
    }

    private void addCurrencyGains()
    {
        Map<String, MutableMoney> currency2money = new HashMap<String, MutableMoney>();

        for (AccountSnapshot snapshot : snapshotStart.getAccounts())
        {
            if (converter.getTermCurrency().equals(snapshot.getAccount().getCurrencyCode()))
                continue;

            MutableMoney value = currency2money.computeIfAbsent(snapshot.getAccount().getCurrencyCode(),
                            c -> MutableMoney.of(converter.getTermCurrency()));

            // subtract initial values
            value.substract(snapshot.getFunds());

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
                    case TRANSFER_IN:
                    case SELL:
                        value.substract(t.getMonetaryAmount().with(converter.at(t.getDate())));
                        break;
                    case REMOVAL:
                    case FEES:
                    case TAXES:
                    case BUY:
                    case TRANSFER_OUT:
                        value.add(t.getMonetaryAmount().with(converter.at(t.getDate())));
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
}
