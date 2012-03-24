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
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;

public class SecurityPerformanceSnapshot
{
    public static SecurityPerformanceSnapshot create(Client client, Date startDate, Date endDate)
    {
        Map<Security, Record> transactions = new HashMap<Security, Record>();

        // null -> cash
        // transactions.put(null, new ArrayList<Transaction>());
        for (Security s : client.getSecurities())
            transactions.put(s, new Record(s));

        extractSecurityRelatedAccountTransactions(client, startDate, endDate, transactions);
        extractSecurityRelatedPortfolioTransactions(client, startDate, endDate, transactions);
        addPseudoValudationTansactions(client, startDate, endDate, transactions);

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

    private static void extractSecurityRelatedAccountTransactions(Client client, Date startDate, Date endDate,
                    Map<Security, Record> transactions)
    {
        for (Account account : client.getAccounts())
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
    }

    private static void extractSecurityRelatedPortfolioTransactions(Client client, Date startDate, Date endDate,
                    Map<Security, Record> transactions)
    {
        for (Portfolio portfolio : client.getPortfolios())
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
                            transactions.get(t.getSecurity()).add(t);
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                }

            }
        }
    }

    private static void addPseudoValudationTansactions(Client client, Date startDate, Date endDate,
                    Map<Security, Record> transactions)
    {
        for (Portfolio portfolio : client.getPortfolios())
        {
            PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, startDate);
            for (SecurityPosition position : snapshot.getPositions())
            {
                transactions.get(position.getSecurity())
                                .add(new SecurityPositionTransaction(true, position, startDate));
            }

            snapshot = PortfolioSnapshot.create(portfolio, endDate);
            for (SecurityPosition position : snapshot.getPositions())
            {
                transactions.get(position.getSecurity()).add(new SecurityPositionTransaction(false, position, endDate));
            }
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
        boolean isStart;
        SecurityPosition position;

        public SecurityPositionTransaction(boolean isStart, SecurityPosition position, Date time)
        {
            this.isStart = isStart;
            this.position = position;
            this.setSecurity(position.getSecurity());
            this.setDate(time);
        }

        @Override
        public int getAmount()
        {
            return position.calculateValue() * (isStart ? -1 : 1);
        }

        @Override
        @SuppressWarnings("nls")
        public String toString()
        {
            return String.format("%tF                                 %,10.2f", getDate(), getAmount() / 100d);
        }

    }

    public static class Record
    {
        private final Security security;
        private List<Transaction> transactions = new ArrayList<Transaction>();

        private int delta;
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

        public int getDelta()
        {
            return delta;
        }

        public List<Transaction> getTransactions()
        {
            return transactions;
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
                    values.add(((SecurityPositionTransaction) t).getAmount() / 100d);
                }
                else if (t instanceof AccountTransaction)
                {
                    values.add(((AccountTransaction) t).getAmount() / 100d);
                }
                else if (t instanceof PortfolioTransaction)
                {
                    PortfolioTransaction pt = (PortfolioTransaction) t;
                    switch (pt.getType())
                    {
                        case TRANSFER_IN:
                        case BUY:
                            values.add(-pt.getAmount() / 100d);
                            break;
                        case TRANSFER_OUT:
                        case SELL:
                            values.add(pt.getAmount() / 100d);
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
                        case TRANSFER_IN:
                        case BUY:
                            delta -= pt.getAmount();
                            break;
                        case TRANSFER_OUT:
                        case SELL:
                            delta += pt.getAmount();
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
                buf.append(String.format("%-46s %,10.2f\n", "Absolute", delta / 100d));
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
