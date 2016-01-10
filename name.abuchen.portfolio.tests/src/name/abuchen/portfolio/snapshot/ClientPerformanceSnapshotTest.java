package name.abuchen.portfolio.snapshot;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import name.abuchen.portfolio.AccountBuilder;
import name.abuchen.portfolio.PortfolioBuilder;
import name.abuchen.portfolio.SecurityBuilder;
import name.abuchen.portfolio.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.MutableMoney;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.Category;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;

@SuppressWarnings("nls")
public class ClientPerformanceSnapshotTest
{
    private final LocalDate startDate = LocalDate.of(2010, Month.DECEMBER, 31);
    private final LocalDate endDate = LocalDate.of(2011, Month.DECEMBER, 31);

    @Test
    public void testDepositPlusInterest()
    {
        Client client = new Client();

        Account account = new Account();
        account.addTransaction(new AccountTransaction(LocalDate.of(2010, Month.JANUARY, 1), CurrencyUnit.EUR, 1000_00,
                        null, AccountTransaction.Type.DEPOSIT));
        account.addTransaction(new AccountTransaction(LocalDate.of(2011, Month.JUNE, 1), CurrencyUnit.EUR, 50_00, null,
                        AccountTransaction.Type.INTEREST));
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
        assertThat(categories.size(), is(ClientPerformanceSnapshot.CategoryType.values().length));

        Map<CategoryType, Category> result = snapshot.getCategoryMap();
        assertThat(result.get(CategoryType.INITIAL_VALUE).getValuation(), is(Money.of(CurrencyUnit.EUR, 1000_00)));
        assertThat(result.get(CategoryType.EARNINGS).getValuation(), is(Money.of(CurrencyUnit.EUR, 50_00)));
        assertThat(result.get(CategoryType.FINAL_VALUE).getValuation(), is(Money.of(CurrencyUnit.EUR, 1050_00)));

        assertThat(snapshot.getAbsoluteDelta(),
                        is(snapshot.getValue(CategoryType.FINAL_VALUE)
                                        .subtract(snapshot.getValue(CategoryType.TRANSFERS))
                                        .subtract(snapshot.getValue(CategoryType.INITIAL_VALUE))));
    }

    @Test
    public void testDepositPlusInterestFirstDay()
    {
        Client client = new Client();

        Account account = new Account();
        account.addTransaction(new AccountTransaction(LocalDate.of(2010, Month.JANUARY, 1), CurrencyUnit.EUR, 1000_00,
                        null, AccountTransaction.Type.DEPOSIT));
        account.addTransaction(new AccountTransaction(LocalDate.of(2010, Month.DECEMBER, 31), CurrencyUnit.EUR, 50_00,
                        null, AccountTransaction.Type.INTEREST));
        client.addAccount(account);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        Map<CategoryType, Category> result = snapshot.getCategoryMap();
        assertThat(result.get(CategoryType.INITIAL_VALUE).getValuation(), is(Money.of(CurrencyUnit.EUR, 1050_00)));
        assertThat(result.get(CategoryType.EARNINGS).getValuation(), is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(result.get(CategoryType.FINAL_VALUE).getValuation(), is(Money.of(CurrencyUnit.EUR, 1050_00)));

        assertThat(snapshot.getAbsoluteDelta(),
                        is(snapshot.getValue(CategoryType.FINAL_VALUE)
                                        .subtract(snapshot.getValue(CategoryType.TRANSFERS))
                                        .subtract(snapshot.getValue(CategoryType.INITIAL_VALUE))));
    }

    @Test
    public void testDepositPlusInterestLastDay()
    {
        Client client = new Client();

        Account account = new Account();
        account.addTransaction(new AccountTransaction(LocalDate.of(2010, Month.JANUARY, 1), CurrencyUnit.EUR, 1000_00,
                        null, AccountTransaction.Type.DEPOSIT));
        account.addTransaction(new AccountTransaction(LocalDate.of(2011, Month.DECEMBER, 31), CurrencyUnit.EUR, 50_00,
                        null, AccountTransaction.Type.INTEREST));
        client.addAccount(account);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        Map<CategoryType, Category> result = snapshot.getCategoryMap();
        assertThat(result.get(CategoryType.INITIAL_VALUE).getValuation(), is(Money.of(CurrencyUnit.EUR, 1000_00)));
        assertThat(result.get(CategoryType.EARNINGS).getValuation(), is(Money.of(CurrencyUnit.EUR, 50_00)));
        assertThat(result.get(CategoryType.CAPITAL_GAINS).getValuation(), is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(result.get(CategoryType.FINAL_VALUE).getValuation(), is(Money.of(CurrencyUnit.EUR, 1050_00)));

        assertThat(snapshot.getAbsoluteDelta(),
                        is(snapshot.getValue(CategoryType.FINAL_VALUE)
                                        .subtract(snapshot.getValue(CategoryType.TRANSFERS))
                                        .subtract(snapshot.getValue(CategoryType.INITIAL_VALUE))));
    }

    @Test
    public void testEarningsFromSecurity()
    {
        Client client = new Client();

        Security security = new Security();
        client.addSecurity(security);

        Portfolio portfolio = new Portfolio();
        portfolio.setReferenceAccount(new Account());
        portfolio.addTransaction(new PortfolioTransaction(LocalDate.of(2010, Month.JANUARY, 1), CurrencyUnit.EUR, 1_00,
                        security, Values.Share.factorize(10), PortfolioTransaction.Type.BUY, 0, 0));
        client.addPortfolio(portfolio);

        Account account = new Account();
        account.addTransaction(new AccountTransaction(LocalDate.of(2011, Month.JANUARY, 31), CurrencyUnit.EUR, 50_00,
                        security, AccountTransaction.Type.INTEREST));
        client.addAccount(account);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        Map<CategoryType, Category> result = snapshot.getCategoryMap();
        assertThat(result.get(CategoryType.EARNINGS).getValuation(), is(Money.of(CurrencyUnit.EUR, 50_00)));
        assertThat(result.get(CategoryType.EARNINGS).getPositions().size(), is(1));

        assertThat(snapshot.getAbsoluteDelta(),
                        is(snapshot.getValue(CategoryType.FINAL_VALUE)
                                        .subtract(snapshot.getValue(CategoryType.TRANSFERS))
                                        .subtract(snapshot.getValue(CategoryType.INITIAL_VALUE))));
    }

    @Test
    public void testCapitalGains()
    {
        Client client = new Client();

        Security security = new Security();
        security.addPrice(new SecurityPrice(LocalDate.of(2010, Month.JANUARY, 1), 10000));
        security.addPrice(new SecurityPrice(LocalDate.of(2011, Month.JUNE, 1), 11000));
        client.addSecurity(security);

        Portfolio portfolio = new Portfolio();
        portfolio.setReferenceAccount(new Account());
        portfolio.addTransaction(new PortfolioTransaction(LocalDate.of(2010, Month.JANUARY, 1), CurrencyUnit.EUR, 1_00,
                        security, Values.Share.factorize(10), PortfolioTransaction.Type.BUY, 0, 0));
        client.addPortfolio(portfolio);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        Map<CategoryType, Category> result = snapshot.getCategoryMap();
        assertThat(result.get(CategoryType.INITIAL_VALUE).getValuation(), is(Money.of(CurrencyUnit.EUR, 1000_00)));
        assertThat(result.get(CategoryType.EARNINGS).getValuation(), is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(result.get(CategoryType.CAPITAL_GAINS).getValuation(), is(Money.of(CurrencyUnit.EUR, 100_00)));
        assertThat(result.get(CategoryType.FINAL_VALUE).getValuation(), is(Money.of(CurrencyUnit.EUR, 1100_00)));

        assertThat(snapshot.getAbsoluteDelta(),
                        is(snapshot.getValue(CategoryType.FINAL_VALUE)
                                        .subtract(snapshot.getValue(CategoryType.TRANSFERS))
                                        .subtract(snapshot.getValue(CategoryType.INITIAL_VALUE))));
    }

    @Test
    public void testCapitalGainsWithBuyDuringReportPeriod()
    {
        Client client = new Client();

        Security security = new Security();
        security.addPrice(new SecurityPrice(LocalDate.of(2010, Month.JANUARY, 1), 10000));
        security.addPrice(new SecurityPrice(LocalDate.of(2011, Month.JUNE, 1), 11000));
        client.addSecurity(security);

        Portfolio portfolio = new Portfolio();
        portfolio.setReferenceAccount(new Account());
        portfolio.addTransaction(new PortfolioTransaction("2010-01-01", CurrencyUnit.EUR, 1_00, security,
                        Values.Share.factorize(10), PortfolioTransaction.Type.BUY, 0, 0));
        portfolio.addTransaction(new PortfolioTransaction("2011-01-15", CurrencyUnit.EUR, 99_00, security,
                        Values.Share.factorize(1), PortfolioTransaction.Type.DELIVERY_INBOUND, 0, 0));
        client.addPortfolio(portfolio);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        Map<CategoryType, Category> result = snapshot.getCategoryMap();
        assertThat(result.get(CategoryType.INITIAL_VALUE).getValuation(), is(Money.of(CurrencyUnit.EUR, 1000_00)));
        assertThat(result.get(CategoryType.EARNINGS).getValuation(), is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(result.get(CategoryType.CAPITAL_GAINS).getValuation(),
                        is(Money.of(CurrencyUnit.EUR, 100_00 + (110_00 - 99_00))));
        assertThat(result.get(CategoryType.FINAL_VALUE).getValuation(), is(Money.of(CurrencyUnit.EUR, 1210_00)));

        assertThat(snapshot.getAbsoluteDelta(),
                        is(snapshot.getValue(CategoryType.FINAL_VALUE)
                                        .subtract(snapshot.getValue(CategoryType.TRANSFERS))
                                        .subtract(snapshot.getValue(CategoryType.INITIAL_VALUE))));
    }

    @Test
    public void testCapitalGainsWithPartialSellDuringReportPeriod()
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addPrice("2010-01-01", 100_00) //
                        .addPrice("2011-06-01", 110_00) //
                        .addTo(client);

        Account account = new AccountBuilder() //
                        .deposit_("2010-01-01", 1_00) //
                        .withdraw("2011-01-15", 99_00) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .buy(security, "2010-01-01", Values.Share.factorize(10), 1_00) //
                        .sell(security, "2011-01-15", Values.Share.factorize(1), 99_00) //
                        .addTo(client);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        assertThat(snapshot.getValue(CategoryType.INITIAL_VALUE), is(Money.of(CurrencyUnit.EUR, 1000_00)));
        assertThat(snapshot.getValue(CategoryType.EARNINGS), is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(snapshot.getValue(CategoryType.CAPITAL_GAINS),
                        is(Money.of(CurrencyUnit.EUR, 10_00 * 9 + (99_00 - 100_00))));
        assertThat(snapshot.getValue(CategoryType.FINAL_VALUE), is(Money.of(CurrencyUnit.EUR, 110_00 * 9)));

        assertThat(snapshot.getAbsoluteDelta(),
                        is(snapshot.getValue(CategoryType.FINAL_VALUE)
                                        .subtract(snapshot.getValue(CategoryType.TRANSFERS))
                                        .subtract(snapshot.getValue(CategoryType.INITIAL_VALUE))));
    }

    @Test
    public void testCapitalGainsWithPartialSellDuringReportPeriodWithFees()
    {
        Client client = new Client();

        Security security = new SecurityBuilder() //
                        .addPrice("2010-01-01", 100_00) //
                        .addPrice("2011-06-01", 110_00) //
                        .addTo(client);

        Account account = new AccountBuilder() //
                        .deposit_("2010-01-01", 1_00) //
                        .withdraw("2011-01-15", 99_00) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .buy(security, "2010-01-01", Values.Share.factorize(10), 1_00) //
                        .sell(security, "2011-01-15", Values.Share.factorize(1), 99_00, 1) //
                        .addTo(client);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        assertThat(snapshot.getValue(CategoryType.CAPITAL_GAINS),
                        is(Money.of(CurrencyUnit.EUR, 1000 * 9 + (9900 - 10000) + 1)));
        assertThat(snapshot.getValue(CategoryType.FEES), is(Money.of(CurrencyUnit.EUR, 1)));

        assertThat(snapshot.getAbsoluteDelta(),
                        is(snapshot.getValue(CategoryType.FINAL_VALUE)
                                        .subtract(snapshot.getValue(CategoryType.TRANSFERS))
                                        .subtract(snapshot.getValue(CategoryType.INITIAL_VALUE))));
    }

    @Test
    public void testCurrencyGainsWithIntermediateTransactions()
    {
        Client client = new Client();

        new AccountBuilder("USD") //
                        .deposit_("2015-01-01", 1000_00) //
                        .deposit_("2015-01-05", 200_00) //
                        .withdraw("2015-01-10", 500_00) //
                        .fees____("2015-01-12", 20_00) //
                        .addTo(client);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, //
                        LocalDate.parse("2015-01-02"), LocalDate.parse("2015-01-15"));

        assertThat(snapshot.getValue(CategoryType.INITIAL_VALUE),
                        is(Money.of(CurrencyUnit.EUR, Math.round(1000_00 * (1 / 1.2043)))));

        assertThat(snapshot.getValue(CategoryType.FINAL_VALUE),
                        is(Money.of(CurrencyUnit.EUR, Math.round(680_00 * (1 / 1.1708)))));

        assertThat(snapshot.getAbsoluteDelta(),
                        is(snapshot.getValue(CategoryType.FINAL_VALUE)
                                        .subtract(snapshot.getValue(CategoryType.TRANSFERS))
                                        .subtract(snapshot.getValue(CategoryType.INITIAL_VALUE))));
    }

    @Test
    public void testCurrencyGainsWithTransferalInOtherCurrencies()
    {
        Client client = new Client();

        Account usd = new AccountBuilder("USD") //
                        .deposit_("2015-01-01", 1000_00) //
                        .addTo(client);

        Account cad = new AccountBuilder("CAD") //
                        .deposit_("2015-01-01", 1000_00) //
                        .addTo(client);
        
        // insert account transfer

        AccountTransferEntry entry = new AccountTransferEntry(usd, cad);
        entry.setDate(LocalDate.parse("2015-01-10"));

        AccountTransaction source = entry.getSourceTransaction();
        AccountTransaction target = entry.getTargetTransaction();

        source.setMonetaryAmount(Money.of("USD", 500_00));
        target.setMonetaryAmount(Money.of("CAD", 1000_00));

        source.addUnit(new Unit(Unit.Type.GROSS_VALUE, source.getMonetaryAmount(), target.getMonetaryAmount(),
                        BigDecimal.valueOf(.5)));

        entry.insert();

        // check currency gain calculation of client performance snapshot

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, //
                        LocalDate.parse("2015-01-01"), LocalDate.parse("2015-01-15"));

        MutableMoney currencyGains = MutableMoney.of(converter.getTermCurrency());
        currencyGains.subtract(snapshot.getValue(CategoryType.INITIAL_VALUE));
        currencyGains.subtract(snapshot.getValue(CategoryType.CAPITAL_GAINS));
        currencyGains.subtract(snapshot.getValue(CategoryType.EARNINGS));
        currencyGains.add(snapshot.getValue(CategoryType.FEES));
        currencyGains.add(snapshot.getValue(CategoryType.TAXES));
        currencyGains.add(snapshot.getValue(CategoryType.TRANSFERS));
        currencyGains.add(snapshot.getValue(CategoryType.FINAL_VALUE));

        assertThat(snapshot.getCategoryByType(CategoryType.CURRENCY_GAINS).getValuation(), is(currencyGains.toMoney()));
    }
}
