package name.abuchen.portfolio.snapshot;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
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
        account.addTransaction(new AccountTransaction(Dates.date(2010, Calendar.JANUARY, 1), CurrencyUnit.EUR, 1000_00,
                        null, AccountTransaction.Type.DEPOSIT));
        account.addTransaction(new AccountTransaction(Dates.date(2011, Calendar.JUNE, 1), CurrencyUnit.EUR, 50_00,
                        null, AccountTransaction.Type.INTEREST));
        client.addAccount(account);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);
        assertNotNull(snapshot);

        assertNotNull(snapshot.getStartClientSnapshot());
        assertEquals(startDate, snapshot.getStartClientSnapshot().getTime());

        assertNotNull(snapshot.getEndClientSnapshot());
        assertEquals(endDate, snapshot.getEndClientSnapshot().getTime());

        List<Category> categories = snapshot.getCategories();
        assertNotNull(categories);
        assertEquals(7, categories.size());

        Map<CategoryType, Category> result = snapshot.getCategoryMap();
        assertThat(result.get(CategoryType.INITIAL_VALUE).getValuation(), is(Money.of(CurrencyUnit.EUR, 1000_00)));
        assertThat(result.get(CategoryType.EARNINGS).getValuation(), is(Money.of(CurrencyUnit.EUR, 50_00)));
        assertThat(result.get(CategoryType.FINAL_VALUE).getValuation(), is(Money.of(CurrencyUnit.EUR, 1050_00)));
    }

    @Test
    public void testDepositPlusInterestFirstDay()
    {
        Client client = new Client();

        Account account = new Account();
        account.addTransaction(new AccountTransaction(Dates.date(2010, Calendar.JANUARY, 1), CurrencyUnit.EUR, 1000_00,
                        null, AccountTransaction.Type.DEPOSIT));
        account.addTransaction(new AccountTransaction(Dates.date(2010, Calendar.DECEMBER, 31), CurrencyUnit.EUR, 50_00,
                        null, AccountTransaction.Type.INTEREST));
        client.addAccount(account);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        Map<CategoryType, Category> result = snapshot.getCategoryMap();
        assertThat(result.get(CategoryType.INITIAL_VALUE).getValuation(), is(Money.of(CurrencyUnit.EUR, 1050_00)));
        assertThat(result.get(CategoryType.EARNINGS).getValuation(), is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(result.get(CategoryType.FINAL_VALUE).getValuation(), is(Money.of(CurrencyUnit.EUR, 1050_00)));
    }

    @Test
    public void testDepositPlusInterestLastDay()
    {
        Client client = new Client();

        Account account = new Account();
        account.addTransaction(new AccountTransaction(Dates.date(2010, Calendar.JANUARY, 1), CurrencyUnit.EUR, 1000_00,
                        null, AccountTransaction.Type.DEPOSIT));
        account.addTransaction(new AccountTransaction(Dates.date(2011, Calendar.DECEMBER, 31), CurrencyUnit.EUR, 50_00,
                        null, AccountTransaction.Type.INTEREST));
        client.addAccount(account);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        Map<CategoryType, Category> result = snapshot.getCategoryMap();
        assertThat(result.get(CategoryType.INITIAL_VALUE).getValuation(), is(Money.of(CurrencyUnit.EUR, 1000_00)));
        assertThat(result.get(CategoryType.EARNINGS).getValuation(), is(Money.of(CurrencyUnit.EUR, 50_00)));
        assertThat(result.get(CategoryType.CAPITAL_GAINS).getValuation(), is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(result.get(CategoryType.FINAL_VALUE).getValuation(), is(Money.of(CurrencyUnit.EUR, 1050_00)));
    }

    @Test
    public void testEarningsFromSecurity()
    {
        Client client = new Client();

        Security security = new Security();
        client.addSecurity(security);

        Portfolio portfolio = new Portfolio();
        portfolio.setReferenceAccount(new Account());
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), CurrencyUnit.EUR,
                        1_00, security, 10, PortfolioTransaction.Type.BUY, 0, 0));
        client.addPortfolio(portfolio);

        Account account = new Account();
        account.addTransaction(new AccountTransaction(Dates.date(2011, Calendar.JANUARY, 31), CurrencyUnit.EUR, 50_00,
                        security, AccountTransaction.Type.INTEREST));
        client.addAccount(account);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        Map<CategoryType, Category> result = snapshot.getCategoryMap();
        assertThat(result.get(CategoryType.EARNINGS).getValuation(), is(Money.of(CurrencyUnit.EUR, 50_00)));
        assertThat(result.get(CategoryType.EARNINGS).getPositions().size(), is(1));
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
        portfolio.setReferenceAccount(new Account());
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), CurrencyUnit.EUR,
                        1_00, security, 1000000, PortfolioTransaction.Type.BUY, 0, 0));
        client.addPortfolio(portfolio);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        Map<CategoryType, Category> result = snapshot.getCategoryMap();
        assertThat(result.get(CategoryType.INITIAL_VALUE).getValuation(), is(Money.of(CurrencyUnit.EUR, 1000_00)));
        assertThat(result.get(CategoryType.EARNINGS).getValuation(), is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(result.get(CategoryType.CAPITAL_GAINS).getValuation(), is(Money.of(CurrencyUnit.EUR, 100_00)));
        assertThat(result.get(CategoryType.FINAL_VALUE).getValuation(), is(Money.of(CurrencyUnit.EUR, 1100_00)));
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
        portfolio.setReferenceAccount(new Account());
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), CurrencyUnit.EUR,
                        1_00, security, 1000000, PortfolioTransaction.Type.BUY, 0, 0));
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2011, Calendar.JANUARY, 15), CurrencyUnit.EUR,
                        99_00, security, 100000, PortfolioTransaction.Type.BUY, 0, 0));
        client.addPortfolio(portfolio);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        Map<CategoryType, Category> result = snapshot.getCategoryMap();
        assertThat(result.get(CategoryType.INITIAL_VALUE).getValuation(), is(Money.of(CurrencyUnit.EUR, 1000_00)));
        assertThat(result.get(CategoryType.EARNINGS).getValuation(), is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(result.get(CategoryType.CAPITAL_GAINS).getValuation(),
                        is(Money.of(CurrencyUnit.EUR, 100_00 + (110_00 - 99_00))));
        assertThat(result.get(CategoryType.FINAL_VALUE).getValuation(), is(Money.of(CurrencyUnit.EUR, 1210_00)));
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
        portfolio.setReferenceAccount(new Account());
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), CurrencyUnit.EUR,
                        1_00, security, 1000000, PortfolioTransaction.Type.BUY, 0, 0));
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2011, Calendar.JANUARY, 15), CurrencyUnit.EUR,
                        99_00, security, 100000, PortfolioTransaction.Type.SELL, 0, 0));
        client.addPortfolio(portfolio);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        Map<CategoryType, Category> result = snapshot.getCategoryMap();
        assertThat(result.get(CategoryType.INITIAL_VALUE).getValuation(), is(Money.of(CurrencyUnit.EUR, 1000_00)));
        assertThat(result.get(CategoryType.EARNINGS).getValuation(), is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(result.get(CategoryType.CAPITAL_GAINS).getValuation(),
                        is(Money.of(CurrencyUnit.EUR, 10_00 * 9 + (99_00 - 100_00))));
        assertThat(result.get(CategoryType.FINAL_VALUE).getValuation(), is(Money.of(CurrencyUnit.EUR, 110_00 * 9)));
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
        portfolio.setReferenceAccount(new Account());
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2010, Calendar.JANUARY, 1), CurrencyUnit.EUR,
                        1_00, security, 1000000, PortfolioTransaction.Type.BUY, 0, 0));
        portfolio.addTransaction(new PortfolioTransaction(Dates.date(2011, Calendar.JANUARY, 15), CurrencyUnit.EUR,
                        99_00, security, 100000, PortfolioTransaction.Type.SELL, 1, 0));
        client.addPortfolio(portfolio);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        Map<CategoryType, Category> result = snapshot.getCategoryMap();
        assertThat(result.get(CategoryType.CAPITAL_GAINS).getValuation(),
                        is(Money.of(CurrencyUnit.EUR, 1000 * 9 + (9900 - 10000) + 1)));
        assertThat(result.get(CategoryType.FEES).getValuation(), is(Money.of(CurrencyUnit.EUR, 1)));
    }

}
