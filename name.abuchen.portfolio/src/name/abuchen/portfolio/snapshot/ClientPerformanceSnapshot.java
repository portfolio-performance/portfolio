package name.abuchen.portfolio.snapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Values;

public class ClientPerformanceSnapshot
{
    public static class Position
    {
        private long valuation;
        private String label;
        private Security security;

        public Position(Security security, long valuation)
        {
            this.label = security.getName();
            this.valuation = valuation;
            this.security = security;
        }

        public Position(String label, long valuation)
        {
            this.label = label;
            this.valuation = valuation;
        }

        public long getValuation()
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
        private long valuation;

        public Category(String label, long valuation)
        {
            this.label = label;
            this.valuation = valuation;
        }

        public long getValuation()
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

    /* package */enum CategoryType
    {
        INITIAL_VALUE, CAPITAL_GAINS, EARNINGS, FEES, TAXES, TRANSFERS, FINAL_VALUE, PERFORMANCE
    }

    private Client client;
    private ClientSnapshot snapshotStart;
    private ClientSnapshot snapshotEnd;
    private EnumMap<CategoryType, Category> categories;
    private List<Transaction> earnings;

    public ClientPerformanceSnapshot(Client client, Date startDate, Date endDate)
    {
        this.client = client;
        this.snapshotStart = ClientSnapshot.create(client, startDate);
        this.snapshotEnd = ClientSnapshot.create(client, endDate);
        this.categories = new EnumMap<CategoryType, Category>(CategoryType.class);
        this.earnings = new ArrayList<Transaction>();

        calculate();
    }
    
    public ClientPerformanceSnapshot(Client client, ReportingPeriod period)
    {
        this(client, period.getStartDate(), period.getEndDate());
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

    public List<Transaction> getEarnings()
    {
        return earnings;
    }

    /* package */EnumMap<CategoryType, Category> getCategoryMap()
    {
        return categories;
    }

    private void calculate()
    {
        categories.put(CategoryType.INITIAL_VALUE, new Category( //
                        String.format(Messages.ColumnInitialValue, snapshotStart.getTime()), snapshotStart.getAssets()));

        categories.put(CategoryType.CAPITAL_GAINS, new Category(Messages.ColumnCapitalGains, 0));
        categories.put(CategoryType.EARNINGS, new Category(Messages.ColumnEarnings, 0));
        categories.put(CategoryType.FEES, new Category(Messages.ColumnPaidFees, 0));
        categories.put(CategoryType.TAXES, new Category(Messages.ColumnPaidTaxes, 0));
        categories.put(CategoryType.TRANSFERS, new Category(Messages.ColumnTransfers, 0));

        categories.put(CategoryType.FINAL_VALUE, new Category( //
                        String.format(Messages.ColumnFinalValue, snapshotEnd.getTime()), snapshotEnd.getAssets()));

        ClientIRRYield yield = ClientIRRYield.create(client, snapshotStart, snapshotEnd);
        categories.put(CategoryType.PERFORMANCE, new Category(Messages.ColumnPerformance, (int) (yield.getIrr() * 100)));

        addCapitalGains();
        addEarnings();
    }

    private void addCapitalGains()
    {
        long startDate = snapshotStart.getTime().getTime();
        long endDate = snapshotEnd.getTime().getTime();

        Map<Security, Long> valuation = new HashMap<Security, Long>();
        for (Security s : client.getSecurities())
            valuation.put(s, Long.valueOf(0));

        for (PortfolioSnapshot portfolio : snapshotStart.getPortfolios())
        {
            for (Map.Entry<Security, SecurityPosition> entry : portfolio.getPositionsBySecurity().entrySet())
            {
                Long v = valuation.get(entry.getKey());
                valuation.put(entry.getKey(), v.longValue() - entry.getValue().calculateValue());
            }

            for (PortfolioTransaction t : portfolio.getSource().getTransactions())
            {
                if (t.getDate().getTime() > startDate && t.getDate().getTime() <= endDate)
                {
                    switch (t.getType())
                    {
                        case BUY:
                        case DELIVERY_INBOUND:
                        case TRANSFER_IN:
                        {
                            Long v = valuation.get(t.getSecurity());
                            valuation.put(t.getSecurity(), v.longValue() - t.getAmount());
                            break;
                        }
                        case SELL:
                        case DELIVERY_OUTBOUND:
                        case TRANSFER_OUT:
                        {
                            Long v = valuation.get(t.getSecurity());
                            valuation.put(t.getSecurity(), v.longValue() + t.getAmount());
                            break;
                        }
                        default:
                            throw new UnsupportedOperationException();
                    }
                }

            }
        }

        for (PortfolioSnapshot portfolio : snapshotEnd.getPortfolios())
        {
            for (Map.Entry<Security, SecurityPosition> entry : portfolio.getPositionsBySecurity().entrySet())
            {
                Long v = valuation.get(entry.getKey());
                valuation.put(entry.getKey(), v.longValue() + entry.getValue().calculateValue());
            }
        }

        long valueGained = 0;
        for (Long v : valuation.values())
            valueGained += v.longValue();

        categories.get(CategoryType.CAPITAL_GAINS).valuation = valueGained;

        for (Security security : sortedSecurities())
        {
            Long value = valuation.get(security);
            if (value == null || value == 0)
                continue;
            categories.get(CategoryType.CAPITAL_GAINS).positions.add(new Position(security, value));
        }
    }

    private List<Security> sortedSecurities()
    {
        List<Security> securities = new ArrayList<Security>(client.getSecurities());
        Collections.sort(securities, new Comparator<Security>()
        {
            public int compare(Security o1, Security o2)
            {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return securities;
    }

    private void addEarnings()
    {
        long startDate = snapshotStart.getTime().getTime();
        long endDate = snapshotEnd.getTime().getTime();

        long earnings = 0;
        long otherEarnings = 0;
        long fees = 0;
        long taxes = 0;
        long deposits = 0;
        long removals = 0;

        Map<Security, Long> earningsBySecurity = new HashMap<Security, Long>();

        for (Account account : client.getAccounts())
        {
            for (AccountTransaction t : account.getTransactions())
            {
                if (t.getDate().getTime() > startDate && t.getDate().getTime() <= endDate)
                {
                    switch (t.getType())
                    {
                        case DIVIDENDS:
                        case INTEREST:
                            this.earnings.add(t);
                            earnings += t.getAmount();
                            if (t.getSecurity() != null)
                            {
                                Long v = earningsBySecurity.get(t.getSecurity());
                                v = v == null ? t.getAmount() : v + t.getAmount();
                                earningsBySecurity.put(t.getSecurity(), v);
                            }
                            else
                            {
                                otherEarnings += t.getAmount();
                            }
                            break;
                        case DEPOSIT:
                            deposits += t.getAmount();
                            break;
                        case REMOVAL:
                            removals += t.getAmount();
                            break;
                        case FEES:
                            fees += t.getAmount();
                            break;
                        case TAXES:
                            taxes += t.getAmount();
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
        }

        for (Portfolio portfolio : client.getPortfolios())
        {
            for (PortfolioTransaction t : portfolio.getTransactions())
            {
                if (t.getDate().getTime() > startDate && t.getDate().getTime() <= endDate)
                {
                    switch (t.getType())
                    {
                        case DELIVERY_INBOUND:
                            deposits += t.getAmount();
                            break;
                        case DELIVERY_OUTBOUND:
                            removals += t.getAmount();
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
        }

        categories.get(CategoryType.EARNINGS).valuation = earnings;
        for (Security security : sortedSecurities())
        {
            Long value = earningsBySecurity.get(security);
            if (value == null || value == 0)
                continue;
            categories.get(CategoryType.EARNINGS).positions.add(new Position(security, value));
        }
        if (otherEarnings > 0)
            categories.get(CategoryType.EARNINGS).positions.add(new Position(Messages.LabelInterest, otherEarnings));

        categories.get(CategoryType.FEES).valuation = fees;

        categories.get(CategoryType.TAXES).valuation = taxes;

        categories.get(CategoryType.TRANSFERS).valuation = deposits - removals;
        categories.get(CategoryType.TRANSFERS).positions.add(new Position(Messages.LabelDeposits, deposits));
        categories.get(CategoryType.TRANSFERS).positions.add(new Position(Messages.LabelRemovals, removals));
    }

    @Override
    @SuppressWarnings("nls")
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        for (Category c : categories.values())
            buf.append(String.format("%-53s %,10.2f\n", c.label, c.valuation / Values.Amount.divider()));
        return buf.toString();
    }
}
