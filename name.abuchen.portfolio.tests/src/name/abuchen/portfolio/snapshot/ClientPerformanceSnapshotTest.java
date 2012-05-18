package name.abuchen.portfolio.snapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.Category;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.util.Dates;

import org.junit.Test;

public class ClientPerformanceSnapshotTest
{
    private final Date startDate = Dates.date(2010, Calendar.DECEMBER, 31);
    private final Date endDate = Dates.date(2011, Calendar.DECEMBER, 31);

    @Test
    public void testDepositPlusInterest()
    {
        Client client = new Client();

        Account account = new Account();
        account.addTransaction(new AccountTransaction(Dates.date(2010, Calendar.JANUARY, 1), null,
                        AccountTransaction.Type.DEPOSIT, 100000));
        account.addTransaction(new AccountTransaction(Dates.date(2011, Calendar.JUNE, 1), null,
                        AccountTransaction.Type.INTEREST, 5000));
        client.addAccount(account);

        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, startDate, endDate);
        assertNotNull(snapshot);

        assertNotNull(snapshot.getStartClientSnapshot());
        assertEquals(startDate, snapshot.getStartClientSnapshot().getTime());

        assertNotNull(snapshot.getEndClientSnapshot());
        assertEquals(endDate, snapshot.getEndClientSnapshot().getTime());

        List<Category> categories = snapshot.getCategories();
        assertNotNull(categories);
        assertEquals(8, categories.size());

        EnumMap<CategoryType, Category> result = snapshot.getCategoryMap();
        assertEquals(100000, result.get(CategoryType.INITIAL_VALUE).getValuation());
        assertEquals(5000, result.get(CategoryType.EARNINGS).getValuation());
        assertEquals(105000, result.get(CategoryType.FINAL_VALUE).getValuation());
    }

    @Test
    public void testDepositPlusInterestFirstDay()
    {
        Client client = new Client();

        Account account = new Account();
        account.addTransaction(new AccountTransaction(Dates.date(2010, Calendar.JANUARY, 1), null,
                        AccountTransaction.Type.DEPOSIT, 100000));
        account.addTransaction(new AccountTransaction(Dates.date(2010, Calendar.DECEMBER, 31), null,
                        AccountTransaction.Type.INTEREST, 5000));
        client.addAccount(account);

        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, startDate, endDate);

        EnumMap<CategoryType, Category> result = snapshot.getCategoryMap();
        assertEquals(105000, result.get(CategoryType.INITIAL_VALUE).getValuation());
        assertEquals(0, result.get(CategoryType.EARNINGS).getValuation());
        assertEquals(105000, result.get(CategoryType.FINAL_VALUE).getValuation());
    }

    @Test
    public void testDepositPlusInterestLastDay()
    {
        Client client = new Client();

        Account account = new Account();
        account.addTransaction(new AccountTransaction(Dates.date(2010, Calendar.JANUARY, 1), null,
                        AccountTransaction.Type.DEPOSIT, 100000));
        account.addTransaction(new AccountTransaction(Dates.date(2011, Calendar.DECEMBER, 31), null,
                        AccountTransaction.Type.INTEREST, 5000));
        client.addAccount(account);

        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, startDate, endDate);

        EnumMap<CategoryType, Category> result = snapshot.getCategoryMap();
        assertEquals(100000, result.get(CategoryType.INITIAL_VALUE).getValuation());
        assertEquals(5000, result.get(CategoryType.EARNINGS).getValuation());
        assertEquals(0, result.get(CategoryType.CAPITAL_GAINS).getValuation());
        assertEquals(105000, result.get(CategoryType.FINAL_VALUE).getValuation());
    }

    @Test
    public void testEarningsFromSecurity()
    {
        Client client = new Client();

        Security security = new Security();
        client.addSecurity(security);

        Portfolio portfolio = new Portfolio();
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), security,
                        PortfolioTransaction.Type.BUY, 10, 100, 0));
        client.addPortfolio(portfolio);

        Account account = new Account();
        account.addTransaction(new AccountTransaction(Dates.date(2011, Calendar.JANUARY, 31), security,
                        AccountTransaction.Type.INTEREST, 5000));
        client.addAccount(account);

        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, startDate, endDate);

        EnumMap<CategoryType, Category> result = snapshot.getCategoryMap();
        assertEquals(5000, result.get(CategoryType.EARNINGS).getValuation());
    }

    @Test
    public void testCapitalGains()
    {
        Client client = new Client();

        Security security = new Security();
        security.addPrice(new SecurityPrice(Dates.date(2010, Calendar.JANUARY, 1), 10000));
        security.addPrice(new SecurityPrice(Dates.date(2011, Calendar.JUNE, 1), 11000));
        client.addSecurity(security);

        Portfolio portfolio = new Portfolio();
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), security,
                        PortfolioTransaction.Type.BUY, 1000000, 100, 0));
        client.addPortfolio(portfolio);

        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, startDate, endDate);

        EnumMap<CategoryType, Category> result = snapshot.getCategoryMap();
        assertEquals(100000, result.get(CategoryType.INITIAL_VALUE).getValuation());
        assertEquals(0, result.get(CategoryType.EARNINGS).getValuation());
        assertEquals(10000, result.get(CategoryType.CAPITAL_GAINS).getValuation());
        assertEquals(110000, result.get(CategoryType.FINAL_VALUE).getValuation());
    }

    @Test
    public void testCapitalGainsWithBuyDuringReportPeriod()
    {
        Client client = new Client();

        Security security = new Security();
        security.addPrice(new SecurityPrice(Dates.date(2010, Calendar.JANUARY, 1), 10000));
        security.addPrice(new SecurityPrice(Dates.date(2011, Calendar.JUNE, 1), 11000));
        client.addSecurity(security);

        Portfolio portfolio = new Portfolio();
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), security,
                        PortfolioTransaction.Type.BUY, 1000000, 100, 0));
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2011, Calendar.JANUARY, 15), security,
                        PortfolioTransaction.Type.BUY, 100000, 9900, 0));
        client.addPortfolio(portfolio);

        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, startDate, endDate);

        EnumMap<CategoryType, Category> result = snapshot.getCategoryMap();
        assertEquals(100000, result.get(CategoryType.INITIAL_VALUE).getValuation());
        assertEquals(0, result.get(CategoryType.EARNINGS).getValuation());
        assertEquals(10000 + (11000 - 9900), result.get(CategoryType.CAPITAL_GAINS).getValuation());
        assertEquals(121000, result.get(CategoryType.FINAL_VALUE).getValuation());
    }

    @Test
    public void testCapitalGainsWithPartialSellDuringReportPeriod()
    {
        Client client = new Client();

        Security security = new Security();
        security.addPrice(new SecurityPrice(Dates.date(2010, Calendar.JANUARY, 1), 10000));
        security.addPrice(new SecurityPrice(Dates.date(2011, Calendar.JUNE, 1), 11000));
        client.addSecurity(security);

        Portfolio portfolio = new Portfolio();
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), security,
                        PortfolioTransaction.Type.BUY, 1000000, 100, 0));
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2011, Calendar.JANUARY, 15), security,
                        PortfolioTransaction.Type.SELL, 100000, 9900, 0));
        client.addPortfolio(portfolio);

        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, startDate, endDate);

        EnumMap<CategoryType, Category> result = snapshot.getCategoryMap();
        assertEquals(100000, result.get(CategoryType.INITIAL_VALUE).getValuation());
        assertEquals(0, result.get(CategoryType.EARNINGS).getValuation());
        assertEquals(1000 * 9 + (9900 - 10000), result.get(CategoryType.CAPITAL_GAINS).getValuation());
        assertEquals(11000 * 9, result.get(CategoryType.FINAL_VALUE).getValuation());
    }

    @Test
    public void testCapitalGainsWithPartialSellDuringReportPeriodWithFees()
    {
        Client client = new Client();

        Security security = new Security();
        security.addPrice(new SecurityPrice(Dates.date(2010, Calendar.JANUARY, 1), 10000));
        security.addPrice(new SecurityPrice(Dates.date(2011, Calendar.JUNE, 1), 11000));
        client.addSecurity(security);

        Portfolio portfolio = new Portfolio();
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), security,
                        PortfolioTransaction.Type.BUY, 1000000, 100, 0));
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2011, Calendar.JANUARY, 15), security,
                        PortfolioTransaction.Type.SELL, 100000, 9900, 1));
        client.addPortfolio(portfolio);

        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, startDate, endDate);

        EnumMap<CategoryType, Category> result = snapshot.getCategoryMap();
        assertEquals(1000 * 9 + (9900 - 10000), result.get(CategoryType.CAPITAL_GAINS).getValuation());
    }

}
