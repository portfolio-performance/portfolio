package name.abuchen.portfolio.snapshot.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;

public class SecurityPerformanceSnapshot
{
    public static SecurityPerformanceSnapshot create(Client client, CurrencyConverter converter, ReportingPeriod period)
    {
        Map<Security, SecurityPerformanceRecord> transactions = initRecords(client);

        for (Account account : client.getAccounts())
            extractSecurityRelatedAccountTransactions(account, period, transactions);
        for (Portfolio portfolio : client.getPortfolios())
        {
            extractSecurityRelatedPortfolioTransactions(portfolio, period, transactions);
            addPseudoValuationTansactions(portfolio, converter, period, transactions);
        }

        return doCreateSnapshot(client, converter, transactions, period);
    }

    public static SecurityPerformanceSnapshot create(Client client, CurrencyConverter converter, Portfolio portfolio,
                    ReportingPeriod period)
    {
        return create(new PortfolioClientFilter(portfolio).filter(client), converter, period);
    }

    private static Map<Security, SecurityPerformanceRecord> initRecords(Client client)
    {
        Map<Security, SecurityPerformanceRecord> records = new HashMap<>();

        for (Security s : client.getSecurities())
            records.put(s, new SecurityPerformanceRecord(s));
        return records;
    }

    private static SecurityPerformanceSnapshot doCreateSnapshot(Client client, CurrencyConverter converter,
                    Map<Security, SecurityPerformanceRecord> records, ReportingPeriod period)
    {
        List<SecurityPerformanceRecord> list = new ArrayList<>(records.values());

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
                record.calculate(client, converter, period);
            }
        }

        return new SecurityPerformanceSnapshot(list);
    }

    private static void extractSecurityRelatedAccountTransactions(Account account, ReportingPeriod period,
                    Map<Security, SecurityPerformanceRecord> records)
    {
        for (AccountTransaction t : account.getTransactions())
        {
            if (t.getSecurity() == null)
                continue;

            if (!period.containsTransaction().test(t))
                continue;

            switch (t.getType())
            {
                case DIVIDENDS:
                case INTEREST:
                    DividendTransaction dt = new DividendTransaction();
                    dt.setDateTime(t.getDateTime());
                    dt.setSecurity(t.getSecurity());
                    dt.setAccount(account);
                    dt.setCurrencyCode(t.getCurrencyCode());
                    dt.setAmount(t.getAmount());
                    dt.setShares(t.getShares());
                    dt.setNote(t.getNote());
                    dt.addUnits(t.getUnits());
                    records.get(t.getSecurity()).addTransaction(dt);
                    break;
                case TAXES:
                case TAX_REFUND:
                case FEES:
                case FEES_REFUND:
                    records.get(t.getSecurity()).addTransaction(t);
                    break;
                case BUY:
                case SELL:
                    break;
                case DEPOSIT:
                case REMOVAL:
                case TRANSFER_IN:
                case TRANSFER_OUT:
                case INTEREST_CHARGE:
                default:
                    throw new IllegalArgumentException(t.toString());
            }
        }
    }

    private static void extractSecurityRelatedPortfolioTransactions(Portfolio portfolio, ReportingPeriod period,
                    Map<Security, SecurityPerformanceRecord> records)
    {
        portfolio.getTransactions().stream() //
                        .filter(period.containsTransaction()) //
                        .forEach(t -> records.get(t.getSecurity()).addTransaction(t));
    }

    private static void addPseudoValuationTansactions(Portfolio portfolio, CurrencyConverter converter,
                    ReportingPeriod period, Map<Security, SecurityPerformanceRecord> records)
    {
        PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, converter, period.getStartDate());
        for (SecurityPosition position : snapshot.getPositions())
        {
            records.get(position.getSecurity())
                            .addTransaction(new DividendInitialTransaction(position, period.getStartDate().atStartOfDay()));
        }

        snapshot = PortfolioSnapshot.create(portfolio, converter, period.getEndDate());
        for (SecurityPosition position : snapshot.getPositions())
        {
            records.get(position.getSecurity())
                            .addTransaction(new DividendFinalTransaction(position, period.getEndDate().atStartOfDay()));
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
