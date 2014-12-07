package name.abuchen.portfolio.snapshot.security;

import java.util.ArrayList;
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

        return doCreateSnapshot(client, transactions, period);
    }

    public static SecurityPerformanceSnapshot create(Client client, Portfolio portfolio, ReportingPeriod period)
    {
        // FIXME create pseudo client --> transferals must add up
        Map<Security, SecurityPerformanceRecord> transactions = initRecords(client);

        Date startDate = period.getStartDate();
        Date endDate = period.getEndDate();

        if (portfolio.getReferenceAccount() != null)
            extractSecurityRelatedAccountTransactions(portfolio.getReferenceAccount(), startDate, endDate, transactions);
        extractSecurityRelatedPortfolioTransactions(portfolio, startDate, endDate, transactions);
        addPseudoValuationTansactions(portfolio, startDate, endDate, transactions);

        return doCreateSnapshot(client, transactions, period);
    }

    private static Map<Security, SecurityPerformanceRecord> initRecords(Client client)
    {
        Map<Security, SecurityPerformanceRecord> records = new HashMap<Security, SecurityPerformanceRecord>();

        for (Security s : client.getSecurities())
            records.put(s, new SecurityPerformanceRecord(s));
        return records;
    }

    private static SecurityPerformanceSnapshot doCreateSnapshot(Client client,
                    Map<Security, SecurityPerformanceRecord> records, ReportingPeriod period)
    {
        List<SecurityPerformanceRecord> list = new ArrayList<SecurityPerformanceRecord>(records.values());

        for (Iterator<SecurityPerformanceRecord> iter = list.iterator(); iter.hasNext();)
        {
            SecurityPerformanceRecord record = iter.next();
            if (record.getTransactions().isEmpty())
            {
                // remove records that have no transactions
                // during the reporting period
                iter.remove();
            }
            else
            {
                // calculate values for each security
                record.calculate(client, period);
            }
        }

        return new SecurityPerformanceSnapshot(list);
    }

    private static void extractSecurityRelatedAccountTransactions(Account account, Date startDate, Date endDate,
                    Map<Security, SecurityPerformanceRecord> records)
    {
        for (AccountTransaction t : account.getTransactions())
        {
            if (t.getSecurity() == null)
                continue;

            if (t.getDate().getTime() <= startDate.getTime())
                continue;

            if (t.getDate().getTime() > endDate.getTime())
                continue;

            if (t.getType() == AccountTransaction.Type.DIVIDENDS //
                            || t.getType() == AccountTransaction.Type.INTEREST)
            {
                DividendTransaction dt = new DividendTransaction();
                dt.setDate(t.getDate());
                dt.setSecurity(t.getSecurity());
                dt.setAccount(account);
                dt.setAmount(t.getAmount());
                dt.setShares(t.getShares());
                dt.setNote(t.getNote());
                records.get(t.getSecurity()).addTransaction(dt);
            }
            else if (t.getType() == AccountTransaction.Type.TAX_REFUND)
            {
                records.get(t.getSecurity()).addTransaction(t);
            }
        }
    }

    private static void extractSecurityRelatedPortfolioTransactions(Portfolio portfolio, Date startDate, Date endDate,
                    Map<Security, SecurityPerformanceRecord> records)
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
                        records.get(t.getSecurity()).addTransaction(t);
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            }

        }
    }

    private static void addPseudoValuationTansactions(Portfolio portfolio, Date startDate, Date endDate,
                    Map<Security, SecurityPerformanceRecord> records)
    {
        PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, startDate);
        for (SecurityPosition position : snapshot.getPositions())
        {
            records.get(position.getSecurity()).addTransaction(new DividendInitialTransaction(position, startDate));
        }

        snapshot = PortfolioSnapshot.create(portfolio, endDate);
        for (SecurityPosition position : snapshot.getPositions())
        {
            records.get(position.getSecurity()).addTransaction(new DividendFinalTransaction(position, endDate));
        }
    }

    private List<SecurityPerformanceRecord> records;

    private SecurityPerformanceSnapshot(List<SecurityPerformanceRecord> records)
    {
        this.records = records;
    }

    public List<SecurityPerformanceRecord> getRecords()
    {
        return records;
    }
}
