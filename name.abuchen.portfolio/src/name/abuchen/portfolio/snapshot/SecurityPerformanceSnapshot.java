package name.abuchen.portfolio.snapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.math.IRR;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Adaptable;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Values;

public class SecurityPerformanceSnapshot
{
    public static SecurityPerformanceSnapshot create(Client client, ReportingPeriod period)
    {
        Map<Security, Record> transactions = initRecords(client);
        
        Date startDate = period.getStartDate();
        Date endDate = period.getEndDate();

        for (Account account : client.getAccounts())
            extractSecurityRelatedAccountTransactions(account, startDate, endDate, transactions);
        for (Portfolio portfolio : client.getPortfolios())
        {
            extractSecurityRelatedPortfolioTransactions(portfolio, startDate, endDate, transactions);
            addPseudoValuationTansactions(portfolio, startDate, endDate, transactions);
        }

        return doCreateSnapshot(transactions);
    }

    public static SecurityPerformanceSnapshot create(Client client, Portfolio portfolio, Date startDate, Date endDate)
    {
        Map<Security, Record> transactions = initRecords(client);

        if (portfolio.getReferenceAccount() != null)
            extractSecurityRelatedAccountTransactions(portfolio.getReferenceAccount(), startDate, endDate, transactions);
        extractSecurityRelatedPortfolioTransactions(portfolio, startDate, endDate, transactions);
        addPseudoValuationTansactions(portfolio, startDate, endDate, transactions);

        return doCreateSnapshot(transactions);
    }

    private static Map<Security, Record> initRecords(Client client)
    {
        Map<Security, Record> transactions = new HashMap<Security, Record>();

        for (Security s : client.getSecurities())
            transactions.put(s, new Record(s));
        return transactions;
    }

    private static SecurityPerformanceSnapshot doCreateSnapshot(Map<Security, Record> transactions)
    {
        for (Record c : transactions.values())
            c.prepare();

        for (Iterator<Map.Entry<Security, Record>> iter = transactions.entrySet().iterator(); iter.hasNext();)
        {
            Map.Entry<Security, Record> entry = iter.next();
            if (entry.getValue().transactions.isEmpty())
                iter.remove();
        }

        return new SecurityPerformanceSnapshot(transactions.values());
    }

    private static void extractSecurityRelatedAccountTransactions(Account account, Date startDate, Date endDate,
                    Map<Security, Record> transactions)
    {
        for (AccountTransaction t : account.getTransactions())
        {
            if (t.getDate().getTime() > startDate.getTime() && t.getDate().getTime() <= endDate.getTime())
            {
                switch (t.getType())
                {
                    case INTEREST:
                    case DIVIDENDS:
                        if (t.getSecurity() != null)
                            transactions.get(t.getSecurity()).add(t);
                        break;
                    case FEES:
                    case TAXES:
                    case DEPOSIT:
                    case REMOVAL:
                    case BUY:
                    case SELL:
                    case TRANSFER_IN:
                    case TRANSFER_OUT:
                        // transactions.get(null).add(t);
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            }
        }
    }

    private static void extractSecurityRelatedPortfolioTransactions(Portfolio portfolio, Date startDate, Date endDate,
                    Map<Security, Record> transactions)
    {
        for (PortfolioTransaction t : portfolio.getTransactions())
        {
            if (t.getDate().getTime() > startDate.getTime() && t.getDate().getTime() <= endDate.getTime())
            {
                switch (t.getType())
                {
                    case TRANSFER_IN:
                    case TRANSFER_OUT:
                    case BUY:
                    case SELL:
                    case DELIVERY_INBOUND:
                    case DELIVERY_OUTBOUND:
                        transactions.get(t.getSecurity()).add(t);
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            }

        }
    }

    private static void addPseudoValuationTansactions(Portfolio portfolio, Date startDate, Date endDate,
                    Map<Security, Record> transactions)
    {
        PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, startDate);
        for (SecurityPosition position : snapshot.getPositions())
        {
            transactions.get(position.getSecurity()).add(new SecurityPositionTransaction(true, position, startDate));
        }

        snapshot = PortfolioSnapshot.create(portfolio, endDate);
        for (SecurityPosition position : snapshot.getPositions())
        {
            transactions.get(position.getSecurity()).add(new SecurityPositionTransaction(false, position, endDate));
        }
    }

    private Collection<Record> calculations;

    private SecurityPerformanceSnapshot(Collection<Record> calculations)
    {
        this.calculations = calculations;
    }

    public List<Record> getRecords()
    {
        return new ArrayList<Record>(calculations);
    }

    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();

        for (Record record : calculations)
        {
            buf.append(record.toString());
            buf.append("\n\n"); //$NON-NLS-1$
        }

        return buf.toString();
    }

    private static class SecurityPositionTransaction extends Transaction
    {
        private boolean isStart;
        private SecurityPosition position;

        public SecurityPositionTransaction(boolean isStart, SecurityPosition position, Date time)
        {
            this.isStart = isStart;
            this.position = position;
            this.setSecurity(position.getSecurity());
            this.setDate(time);
        }

        @Override
        public long getAmount()
        {
            return position.calculateValue() * (isStart ? -1 : 1);
        }

        @Override
        @SuppressWarnings("nls")
        public String toString()
        {
            return String.format("%tF                                 %,10.2f", getDate(),
                            getAmount() / Values.Amount.divider());
        }

    }

    public static class Record implements Adaptable
    {
        private final Security security;
        private List<Transaction> transactions = new ArrayList<Transaction>();

        private long delta;
        private double irr;

        /* package */Record(Security security)
        {
            this.security = security;
        }

        public Security getSecurity()
        {
            return security;
        }

        public double getIrr()
        {
            return irr;
        }

        public long getDelta()
        {
            return delta;
        }

        public List<Transaction> getTransactions()
        {
            return transactions;
        }

        @Override
        public <T> T adapt(Class<T> type)
        {
            return type == Security.class ? type.cast(security) : null;
        }

        void add(Transaction t)
        {
            transactions.add(t);
        }

        void prepare()
        {
            Collections.sort(transactions);

            if (!transactions.isEmpty())
            {
                calculateDelta();
                calculateIRR();
            }
        }

        private void calculateIRR()
        {
            List<Date> dates = new ArrayList<Date>();
            List<Double> values = new ArrayList<Double>();

            for (Transaction t : transactions)
            {
                Calendar cal = Calendar.getInstance();
                cal.setTime(t.getDate());
                dates.add(t.getDate());

                if (t instanceof SecurityPositionTransaction)
                {
                    values.add(((SecurityPositionTransaction) t).getAmount() / Values.Amount.divider());
                }
                else if (t instanceof AccountTransaction)
                {
                    values.add(((AccountTransaction) t).getAmount() / Values.Amount.divider());
                }
                else if (t instanceof PortfolioTransaction)
                {
                    PortfolioTransaction pt = (PortfolioTransaction) t;
                    switch (pt.getType())
                    {
                        case BUY:
                        case DELIVERY_INBOUND:
                        case TRANSFER_IN:
                            values.add(-pt.getAmount() / Values.Amount.divider());
                            break;
                        case SELL:
                        case DELIVERY_OUTBOUND:
                        case TRANSFER_OUT:
                            values.add(pt.getAmount() / Values.Amount.divider());
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                }
                else
                {
                    throw new UnsupportedOperationException();
                }
            }

            this.irr = IRR.calculate(dates, values);
        }

        private void calculateDelta()
        {
            for (Transaction t : transactions)
            {
                if (t instanceof SecurityPositionTransaction)
                {
                    delta += ((SecurityPositionTransaction) t).getAmount();
                }
                else if (t instanceof AccountTransaction)
                {
                    delta += ((AccountTransaction) t).getAmount();
                }
                else if (t instanceof PortfolioTransaction)
                {
                    PortfolioTransaction pt = (PortfolioTransaction) t;
                    switch (pt.getType())
                    {
                        case BUY:
                        case DELIVERY_INBOUND:
                            delta -= pt.getAmount();
                            break;
                        case SELL:
                        case DELIVERY_OUTBOUND:
                            delta += pt.getAmount();
                            break;
                        case TRANSFER_IN:
                        case TRANSFER_OUT:
                            // transferals do not contribute to the delta
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                }
                else
                {
                    throw new UnsupportedOperationException();
                }
            }
        }

        @Override
        @SuppressWarnings("nls")
        public String toString()
        {
            StringBuilder buf = new StringBuilder();

            buf.append(security.getName()).append("\n");

            if (!transactions.isEmpty())
            {
                buf.append(String.format("%-46s %,10.2f\n", "Absolute", delta / Values.Amount.divider()));
                buf.append(String.format("%-46s %,10.2f\n", "IRR", irr * 100));

                for (Transaction t : transactions)
                {
                    buf.append("\t").append(String.valueOf(t)).append("\n");
                }
            }

            return buf.toString();
        }

    }
}
