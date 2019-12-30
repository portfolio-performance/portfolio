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
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.util.Interval;

public class SecurityPerformanceSnapshot
{
    public static SecurityPerformanceSnapshot create(Client client, CurrencyConverter converter, Interval interval)
    {
        Map<Security, SecurityPerformanceRecord> transactions = initRecords(client);

        for (Account account : client.getAccounts())
            extractSecurityRelatedAccountTransactions(account, interval, transactions);
        for (Portfolio portfolio : client.getPortfolios())
        {
            extractSecurityRelatedPortfolioTransactions(portfolio, interval, transactions);
            addPseudoValuationTansactions(portfolio, converter, interval, transactions);
        }

        return doCreateSnapshot(client, converter, transactions, interval);
    }

    public static SecurityPerformanceSnapshot create(Client client, CurrencyConverter converter, Portfolio portfolio,
                    Interval interval)
    {
        return create(new PortfolioClientFilter(portfolio).filter(client), converter, interval);
    }

    private static Map<Security, SecurityPerformanceRecord> initRecords(Client client)
    {
        Map<Security, SecurityPerformanceRecord> records = new HashMap<>();

        for (Security s : client.getSecurities())
            records.put(s, new SecurityPerformanceRecord(client, s));
        return records;
    }

    private static SecurityPerformanceSnapshot doCreateSnapshot(Client client, CurrencyConverter converter,
                    Map<Security, SecurityPerformanceRecord> records, Interval interval)
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
                record.calculate(client, converter, interval);
            }
        }

        return new SecurityPerformanceSnapshot(list);
    }

    private static void extractSecurityRelatedAccountTransactions(Account account, Interval interval,
                    Map<Security, SecurityPerformanceRecord> records)
    {
        for (AccountTransaction t : account.getTransactions())
        {
            if (t.getSecurity() == null)
                continue;

            if (!interval.contains(t.getDateTime()))
                continue;

            switch (t.getType())
            {
                case DIVIDENDS:
                case INTEREST:
                    DividendTransaction dt = DividendTransaction.from(t);
                    dt.setAccount(account);
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

    private static void extractSecurityRelatedPortfolioTransactions(Portfolio portfolio, Interval interval,
                    Map<Security, SecurityPerformanceRecord> records)
    {
        portfolio.getTransactions().stream() //
                        .filter(t -> interval.contains(t.getDateTime())) //
                        .forEach(t -> records.get(t.getSecurity()).addTransaction(t));
    }

    private static void addPseudoValuationTansactions(Portfolio portfolio, CurrencyConverter converter,
                    Interval interval, Map<Security, SecurityPerformanceRecord> records)
    {
        PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, converter, interval.getStart());
        for (SecurityPosition position : snapshot.getPositions())
        {
            records.get(position.getSecurity())
                            .addTransaction(new DividendInitialTransaction(position,
                                            interval.getStart().atStartOfDay()));
        }

        snapshot = PortfolioSnapshot.create(portfolio, converter, interval.getEnd());
        for (SecurityPosition position : snapshot.getPositions())
        {
            records.get(position.getSecurity())
                            .addTransaction(new DividendFinalTransaction(position, interval.getEnd().atStartOfDay()));
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
