package name.abuchen.portfolio.snapshot.security;

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
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.SecurityPosition;

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
}
