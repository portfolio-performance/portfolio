package name.abuchen.portfolio.snapshot.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.util.Interval;

public class SecurityPerformanceSnapshot
{
    public static SecurityPerformanceSnapshot create(Client client, CurrencyConverter converter, Interval interval)
    {
        Map<Security, SecurityPerformanceRecord.Builder> transactions = initRecords(client);

        for (Account account : client.getAccounts())
        {
            extractSecurityRelatedAccountTransactions(account, interval, transactions);
        }

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

    public static SecurityPerformanceSnapshot create(Client client, CurrencyConverter converter, Interval interval,
                    ClientSnapshot valuationAtStart, ClientSnapshot valuationAtEnd)
    {
        Map<Security, SecurityPerformanceRecord.Builder> transactions = initRecords(client);

        for (Account account : client.getAccounts())
            extractSecurityRelatedAccountTransactions(account, interval, transactions);

        for (Portfolio portfolio : client.getPortfolios())
            extractSecurityRelatedPortfolioTransactions(portfolio, interval, transactions);

        for (PortfolioSnapshot snapshot : valuationAtStart.getPortfolios())
        {
            for (SecurityPosition position : snapshot.getPositions())
            {
                transactions.get(position.getSecurity()).addLineItem(CalculationLineItem
                                .atStart(snapshot.getPortfolio(), position, interval.getStart().atStartOfDay()));
            }
        }

        for (PortfolioSnapshot snapshot : valuationAtEnd.getPortfolios())
        {
            for (SecurityPosition position : snapshot.getPositions())
            {
                transactions.get(position.getSecurity()).addLineItem(CalculationLineItem.atEnd(snapshot.getPortfolio(),
                                position, interval.getEnd().atStartOfDay()));
            }
        }

        return doCreateSnapshot(client, converter, transactions, interval);
    }

    private static Map<Security, SecurityPerformanceRecord.Builder> initRecords(Client client)
    {
        Map<Security, SecurityPerformanceRecord.Builder> records = new HashMap<>();

        for (Security s : client.getSecurities())
            records.put(s, new SecurityPerformanceRecord.Builder(s));
        return records;
    }

    private static SecurityPerformanceSnapshot doCreateSnapshot(Client client, CurrencyConverter converter,
                    Map<Security, SecurityPerformanceRecord.Builder> records, Interval interval)
    {
        List<SecurityPerformanceRecord> list = new ArrayList<>();

        for (SecurityPerformanceRecord.Builder record : records.values())
        {
            if (record.isEmpty())
                continue;

            list.add(record.build(client, converter, interval));
        }

        return new SecurityPerformanceSnapshot(list);
    }

    private static void extractSecurityRelatedAccountTransactions(Account account, Interval interval,
                    Map<Security, SecurityPerformanceRecord.Builder> records)
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
                case TAXES:
                case TAX_REFUND:
                case FEES:
                case FEES_REFUND:
                    records.get(t.getSecurity()).addLineItem(CalculationLineItem.of(account, t));
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
                    Map<Security, SecurityPerformanceRecord.Builder> records)
    {
        portfolio.getTransactions().stream() //
                        .filter(t -> interval.contains(t.getDateTime())) //
                        .forEach(t -> records.get(t.getSecurity()).addLineItem(CalculationLineItem.of(portfolio, t)));
    }

    private static void addPseudoValuationTansactions(Portfolio portfolio, CurrencyConverter converter,
                    Interval interval, Map<Security, SecurityPerformanceRecord.Builder> records)
    {
        PortfolioSnapshot snapshot = PortfolioSnapshot.create(portfolio, converter, interval.getStart());
        for (SecurityPosition position : snapshot.getPositions())
        {
            records.get(position.getSecurity()).addLineItem(
                            CalculationLineItem.atStart(portfolio, position, interval.getStart().atStartOfDay()));
        }

        snapshot = PortfolioSnapshot.create(portfolio, converter, interval.getEnd());
        for (SecurityPosition position : snapshot.getPositions())
        {
            records.get(position.getSecurity()).addLineItem(
                            CalculationLineItem.atEnd(portfolio, position, interval.getEnd().atStartOfDay()));
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
