package name.abuchen.portfolio.snapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
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
        Map<Security, SecurityPerformanceRecord> transactions = initRecords(client);

        Date startDate = period.getStartDate();
        Date endDate = period.getEndDate();

        for (Account account : client.getAccounts())
            extractSecurityRelatedAccountTransactions(account, startDate, endDate, transactions);
        for (Portfolio portfolio : client.getPortfolios())
        {
            extractSecurityRelatedPortfolioTransactions(portfolio, startDate, endDate, transactions);
            addPseudoValuationTansactions(portfolio, startDate, endDate, transactions);
        }

        return doCreateSnapshot(transactions, endDate);
    }

    public static SecurityPerformanceSnapshot create(Client client, Portfolio portfolio, ReportingPeriod period)
    {
        Map<Security, SecurityPerformanceRecord> transactions = initRecords(client);

        Date startDate = period.getStartDate();
        Date endDate = period.getEndDate();

        if (portfolio.getReferenceAccount() != null)
            extractSecurityRelatedAccountTransactions(portfolio.getReferenceAccount(), startDate, endDate, transactions);
        extractSecurityRelatedPortfolioTransactions(portfolio, startDate, endDate, transactions);
        addPseudoValuationTansactions(portfolio, startDate, endDate, transactions);

        return doCreateSnapshot(transactions, endDate);
    }

    private static Map<Security, SecurityPerformanceRecord> initRecords(Client client)
    {
        Map<Security, SecurityPerformanceRecord> transactions = new HashMap<Security, SecurityPerformanceRecord>();

        for (Security s : client.getSecurities())
            transactions.put(s, new SecurityPerformanceRecord(s));
        return transactions;
    }

    private static SecurityPerformanceSnapshot doCreateSnapshot(Map<Security, SecurityPerformanceRecord> transactions,
                    Date endDate)
    {
        for (SecurityPerformanceRecord c : transactions.values())
            c.prepare(endDate);

        for (Iterator<Map.Entry<Security, SecurityPerformanceRecord>> iter = transactions.entrySet().iterator(); iter
                        .hasNext();)
        {
            Map.Entry<Security, SecurityPerformanceRecord> entry = iter.next();
            SecurityPerformanceRecord d = entry.getValue();
            if (d.getTransactions().isEmpty())
                iter.remove();
            else if (d.getStockShares() == 0)
                iter.remove();
        }

        // prepare pseudo summarize

        SecurityPerformanceRecord sum1 = null;

        for (SecurityPerformanceRecord c : transactions.values())
        {
            if (c.getSecurity().getName().equalsIgnoreCase("_summe_"))
            {
                sum1 = c;
                break;
            }
        }

        if (sum1 != null)
        {

            SecurityPerformanceRecord sum = sum1;
            // DivRecord sum = new DivRecord(sum1.getSecurity());
            // transactions.values().add(sum); // crasht mit new
            // DivRecord(sum1.getSecurity());

            for (SecurityPerformanceRecord c : transactions.values())
            {
                if (c != sum)
                    sum.summarize(c);
            }

        }

        return new SecurityPerformanceSnapshot(transactions.values());
    }

    private static void extractSecurityRelatedAccountTransactions(Account account, Date startDate, Date endDate,
                    Map<Security, SecurityPerformanceRecord> transactions)
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
                        {
                            DividendTransaction dt = new DividendTransaction();
                            dt.setDate(t.getDate());
                            dt.setSecurity(t.getSecurity());
                            dt.setAccount(account);
                            dt.setAmountAndShares(t.getAmount(), t.getShares());
                            transactions.get(t.getSecurity()).add(dt);
                        }
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
                    Map<Security, SecurityPerformanceRecord> transactions)
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
                    Map<Security, SecurityPerformanceRecord> transactions)
    {
        PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, startDate);
        for (SecurityPosition position : snapshot.getPositions())
        {
            transactions.get(position.getSecurity()).add(new DividendInitialTransaction(position, startDate));
        }

        snapshot = PortfolioSnapshot.create(portfolio, endDate);
        for (SecurityPosition position : snapshot.getPositions())
        {
            transactions.get(position.getSecurity()).add(new DividendFinalTransaction(position, endDate));
        }
    }

    private Collection<SecurityPerformanceRecord> calculations;

    private SecurityPerformanceSnapshot(Collection<SecurityPerformanceRecord> calculations)
    {
        this.calculations = calculations;
    }

    public List<SecurityPerformanceRecord> getRecords()
    {
        return new ArrayList<SecurityPerformanceRecord>(calculations);
    }

    public static class DividendTransaction extends Transaction
    {
        // public enum Type
        // {
        // DEPOSIT, REMOVAL, INTEREST, DIVIDENDS, FEES, TAXES, BUY, SELL,
        // TRANSFER_IN, TRANSFER_OUT;
        //
        //            private static final ResourceBundle RESOURCES = ResourceBundle.getBundle("name.abuchen.portfolio.model.labels"); //$NON-NLS-1$
        //
        // public String toString()
        // {
        //                return RESOURCES.getString("account." + name()); //$NON-NLS-1$
        // }
        // }

        // private Type type;

        long amount;
        private Account account;
        long shares;
        private long dividendPerShare;
        private boolean isDiv12;
        private int divEventId;

        public DividendTransaction()
        {}

        // public DividendTransaction(Date date, Security security, Type type,
        // long amount)
        // {
        // super(date, security);
        // this.type = type;
        // this.amount = amount;
        // }

        // public Type getType()
        // {
        // return type;
        // }
        //
        // public void setType(Type type)
        // {
        // this.type = type;
        // }

        public Account getAccount()
        {
            return account;
        }

        public void setAccount(Account account)
        {
            this.account = account;
        }

        @Override
        public long getAmount()
        {
            return amount;
        }

        public void setAmountAndShares(long amount, long shares)
        {
            this.amount = amount;
            this.shares = shares;
            this.dividendPerShare = amountFractionPerShare(amount, shares);
        }

        public long getShares()
        {
            return shares;
        }

        public long getDividendPerShare()
        {
            return dividendPerShare;
        }

        public boolean getIsDiv12()
        {
            return isDiv12;
        }

        public void setIsDiv12(boolean isDiv12)
        {
            this.isDiv12 = isDiv12;
        }

        public int getDivEventId()
        {
            return divEventId;
        }

        public void setDivEventId(int divEventId)
        {
            this.divEventId = divEventId;
        }

        static public long amountFractionPerShare(long amount, long shares)
        {
            if (shares == 0)
                return 0;

            return Math.round((double) (amount * (Values.AmountFraction.factor() / Values.Amount.factor()) * Values.Share
                            .divider()) / (double) shares);
        }

        static public long amountPerShare(long amount, long shares)
        {
            if (shares != 0)
            {
                return Math.round((double) amount / (double) shares * Values.Share.divider());
            }
            else
            {
                return 0;
            }
        }

        static public long amountTimesShares(long price, long shares)
        {
            if (shares != 0)
            {
                return Math.round((double) price * (double) shares / Values.Share.divider());
            }
            else
            {
                return 0;
            }
        }

    }

    public static class DividendInitialTransaction extends Transaction
    {
        private SecurityPosition position;

        public DividendInitialTransaction(SecurityPosition position, Date time)
        {
            this.position = position;
            this.setSecurity(position.getSecurity());
            this.setDate(time);
        }

        @Override
        public long getAmount()
        {
            return position.calculateValue();
        }

        public SecurityPosition getPosition()
        {
            return position;
        }
    }

    public static class DividendFinalTransaction extends Transaction
    {
        private SecurityPosition position;

        public DividendFinalTransaction(SecurityPosition position, Date time)
        {
            this.position = position;
            this.setSecurity(position.getSecurity());
            this.setDate(time);
        }

        @Override
        public long getAmount()
        {
            return position.calculateValue();
        }

        public SecurityPosition getPosition()
        {
            return position;
        }
    }
}
