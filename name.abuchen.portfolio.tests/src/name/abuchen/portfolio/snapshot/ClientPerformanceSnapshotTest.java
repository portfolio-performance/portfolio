package name.abuchen.portfolio.snapshot;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

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
import name.abuchen.portfolio.model.Transaction;
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
        account.addTransaction(new AccountTransaction(LocalDateTime.of(2010, Month.JANUARY, 1, 0, 0), CurrencyUnit.EUR, 1000_00,
                        null, AccountTransaction.Type.DEPOSIT));
        account.addTransaction(new AccountTransaction(LocalDateTime.of(2011, Month.JUNE, 1, 0, 0), CurrencyUnit.EUR, 50_00, null,
                        AccountTransaction.Type.INTEREST));
        client.addAccount(account);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);
        assertNotNull(snapshot);

        assertNotNull(snapshot.getStartClientSnapshot());
        assertEquals(startDate, snapshot.getStartClientSnapshot().getTime().toLocalDate());

        assertNotNull(snapshot.getEndClientSnapshot());
        assertEquals(endDate, snapshot.getEndClientSnapshot().getTime().toLocalDate());

        List<Category> categories = snapshot.getCategories();
        assertNotNull(categories);
        assertThat(categories.size(), is(ClientPerformanceSnapshot.CategoryType.values().length));

        assertThat(snapshot.getValue(CategoryType.INITIAL_VALUE), is(Money.of(CurrencyUnit.EUR, 1000_00)));
        assertThat(snapshot.getValue(CategoryType.EARNINGS), is(Money.of(CurrencyUnit.EUR, 50_00)));
        assertThat(snapshot.getValue(CategoryType.FINAL_VALUE), is(Money.of(CurrencyUnit.EUR, 1050_00)));

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
        account.addTransaction(new AccountTransaction(LocalDateTime.of(2010, Month.JANUARY, 1, 0, 0), CurrencyUnit.EUR, 1000_00,
                        null, AccountTransaction.Type.DEPOSIT));
        account.addTransaction(new AccountTransaction(LocalDateTime.of(2010, Month.DECEMBER, 31, 0, 0), CurrencyUnit.EUR, 50_00,
                        null, AccountTransaction.Type.INTEREST));
        client.addAccount(account);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        assertThat(snapshot.getValue(CategoryType.INITIAL_VALUE), is(Money.of(CurrencyUnit.EUR, 1050_00)));
        assertThat(snapshot.getValue(CategoryType.EARNINGS), is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(snapshot.getValue(CategoryType.FINAL_VALUE), is(Money.of(CurrencyUnit.EUR, 1050_00)));

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
        account.addTransaction(new AccountTransaction(LocalDateTime.of(2010, Month.JANUARY, 1, 0, 0), CurrencyUnit.EUR, 1000_00,
                        null, AccountTransaction.Type.DEPOSIT));
        account.addTransaction(new AccountTransaction(LocalDateTime.of(2011, Month.DECEMBER, 31, 0, 0), CurrencyUnit.EUR, 50_00,
                        null, AccountTransaction.Type.INTEREST));
        client.addAccount(account);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        assertThat(snapshot.getValue(CategoryType.INITIAL_VALUE), is(Money.of(CurrencyUnit.EUR, 1000_00)));
        assertThat(snapshot.getValue(CategoryType.EARNINGS), is(Money.of(CurrencyUnit.EUR, 50_00)));
        assertThat(snapshot.getValue(CategoryType.CAPITAL_GAINS), is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(snapshot.getValue(CategoryType.FINAL_VALUE), is(Money.of(CurrencyUnit.EUR, 1050_00)));

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
        portfolio.addTransaction(new PortfolioTransaction(LocalDateTime.of(2010, Month.JANUARY, 1, 0, 0), CurrencyUnit.EUR, 1_00,
                        security, Values.Share.factorize(10), PortfolioTransaction.Type.BUY, 0, 0));
        client.addPortfolio(portfolio);

        Account account = new Account();
        account.addTransaction(new AccountTransaction(LocalDateTime.of(2011, Month.JANUARY, 31, 0, 0), CurrencyUnit.EUR, 50_00,
                        security, AccountTransaction.Type.INTEREST));
        client.addAccount(account);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        assertThat(snapshot.getValue(CategoryType.EARNINGS), is(Money.of(CurrencyUnit.EUR, 50_00)));
        assertThat(snapshot.getCategoryByType(CategoryType.EARNINGS).getPositions().size(), is(1));

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
        security.addPrice(new SecurityPrice(LocalDate.of(2010, Month.JANUARY, 1), Values.Quote.factorize(100)));
        security.addPrice(new SecurityPrice(LocalDate.of(2011, Month.JUNE, 1), Values.Quote.factorize(110)));
        client.addSecurity(security);

        Portfolio portfolio = new Portfolio();
        portfolio.setReferenceAccount(new Account());
        portfolio.addTransaction(new PortfolioTransaction(LocalDateTime.of(2010, Month.JANUARY, 1, 0, 0), CurrencyUnit.EUR, 1_00,
                        security, Values.Share.factorize(10), PortfolioTransaction.Type.BUY, 0, 0));
        client.addPortfolio(portfolio);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        assertThat(snapshot.getValue(CategoryType.INITIAL_VALUE), is(Money.of(CurrencyUnit.EUR, 1000_00)));
        assertThat(snapshot.getValue(CategoryType.EARNINGS), is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(snapshot.getValue(CategoryType.CAPITAL_GAINS), is(Money.of(CurrencyUnit.EUR, 100_00)));
        assertThat(snapshot.getValue(CategoryType.FINAL_VALUE), is(Money.of(CurrencyUnit.EUR, 1100_00)));

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
        security.addPrice(new SecurityPrice(LocalDate.of(2010, Month.JANUARY, 1), Values.Quote.factorize(100)));
        security.addPrice(new SecurityPrice(LocalDate.of(2011, Month.JUNE, 1), Values.Quote.factorize(110)));
        client.addSecurity(security);

        Portfolio portfolio = new Portfolio();
        portfolio.setReferenceAccount(new Account());
        portfolio.addTransaction(new PortfolioTransaction(LocalDateTime.of(2010, Month.JANUARY, 1, 0, 0), CurrencyUnit.EUR, 1_00, security,
                        Values.Share.factorize(10), PortfolioTransaction.Type.BUY, 0, 0));
        portfolio.addTransaction(new PortfolioTransaction(LocalDateTime.of(2011, Month.JANUARY, 15, 0, 0), CurrencyUnit.EUR, 99_00, security,
                        Values.Share.factorize(1), PortfolioTransaction.Type.DELIVERY_INBOUND, 0, 0));
        client.addPortfolio(portfolio);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        assertThat(snapshot.getValue(CategoryType.INITIAL_VALUE), is(Money.of(CurrencyUnit.EUR, 1000_00)));
        assertThat(snapshot.getValue(CategoryType.EARNINGS), is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(snapshot.getValue(CategoryType.CAPITAL_GAINS),
                        is(Money.of(CurrencyUnit.EUR, 100_00 + (110_00 - 99_00))));
        assertThat(snapshot.getValue(CategoryType.FINAL_VALUE), is(Money.of(CurrencyUnit.EUR, 1210_00)));

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
                        .addPrice("2010-01-01", Values.Quote.factorize(100)) //
                        .addPrice("2011-06-01", Values.Quote.factorize(110)) //
                        .addTo(client);

        Account account = new AccountBuilder() //
                        .deposit_(LocalDateTime.of(2010, 01, 01, 0, 0), 1_00) //
                        .withdraw(LocalDateTime.of(2011, 01, 15, 0, 0), 99_00) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .buy(security, LocalDateTime.of(2010, 01, 01, 0, 0), Values.Share.factorize(10), 1_00) //
                        .sell(security, LocalDateTime.of(2011, 01, 15, 0, 0), Values.Share.factorize(1), 99_00) //
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
                        .addPrice("2010-01-01", Values.Quote.factorize(100)) //
                        .addPrice("2011-06-01", Values.Quote.factorize(110)) //
                        .addTo(client);

        Account account = new AccountBuilder() //
                        .deposit_(LocalDateTime.of(2010, 01, 01, 0, 0), 1_00) //
                        .withdraw(LocalDateTime.of(2011, 01, 15, 0, 0), 99_00) //
                        .addTo(client);

        new PortfolioBuilder(account) //
                        .buy(security, LocalDateTime.of(2010, 01, 01, 0, 0), Values.Share.factorize(10), 1_00) //
                        .sell(security, LocalDateTime.of(2011, 01, 15, 0, 0), Values.Share.factorize(1), 99_00, 1) //
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
                        .deposit_(LocalDateTime.of(2015, 01, 01, 0, 0), 1000_00) //
                        .deposit_(LocalDateTime.of(2015, 01, 05, 0, 0), 200_00) //
                        .withdraw(LocalDateTime.of(2015, 01, 10, 0, 0), 500_00) //
                        .fees____(LocalDateTime.of(2015, 01, 12, 0, 0), 20_00) //
                        .addTo(client);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, //
                        LocalDate.of(2015, Month.JANUARY, 2), LocalDate.of(2015, Month.JANUARY, 15));

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
                        .deposit_(LocalDateTime.of(2015, 01, 01, 0, 0), 1000_00) //
                        .addTo(client);

        Account cad = new AccountBuilder("CAD") //
                        .deposit_(LocalDateTime.of(2015, 01, 01, 0, 0), 1000_00) //
                        .addTo(client);

        // insert account transfer

        AccountTransferEntry entry = new AccountTransferEntry(usd, cad);
        entry.setDate(LocalDateTime.of(2015, Month.JANUARY, 10, 0, 0));

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

    @Test
    public void testDividendTransactionWithTaxes()
    {
        Client client = new Client();

        Security security = new SecurityBuilder().addTo(client);
        Account account = new AccountBuilder().addTo(client);

        AccountTransaction dividend = new AccountTransaction();
        dividend.setDate(LocalDateTime.of(2011, Month.MARCH, 1, 0, 0));
        dividend.setType(AccountTransaction.Type.DIVIDENDS);
        dividend.setSecurity(security);
        dividend.setMonetaryAmount(Money.of(CurrencyUnit.EUR, 100_00));
        dividend.addUnit(new Transaction.Unit(Transaction.Unit.Type.TAX, Money.of(CurrencyUnit.EUR, 10_00)));

        assertThat(dividend.getGrossValue(), is(Money.of(CurrencyUnit.EUR, 110_00)));

        account.addTransaction(dividend);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        assertThat(snapshot.getValue(CategoryType.EARNINGS), is(Money.of(CurrencyUnit.EUR, 110_00)));
        assertThat(snapshot.getValue(CategoryType.TAXES), is(Money.of(CurrencyUnit.EUR, 10_00)));

        assertThat(snapshot.getEarnings().size(), is(1));
        assertThat(snapshot.getCategoryByType(CategoryType.EARNINGS).getPositions().get(0).getValuation(),
                        is(Money.of(CurrencyUnit.EUR, 110_00)));

        GroupEarningsByAccount.Item item = new GroupEarningsByAccount(snapshot).getItems().get(0);
        assertThat(item.getSum(), is(Money.of(CurrencyUnit.EUR, 110_00)));

        assertThatCalculationWorksOut(snapshot, converter);
    }

    @Test
    public void testInboundDeliveryWithFees()
    {
        Client client = new Client();

        Security security = new SecurityBuilder().addTo(client);
        Portfolio portfolio = new PortfolioBuilder().addTo(client);

        PortfolioTransaction delivery = new PortfolioTransaction();
        delivery.setDate(LocalDateTime.of(2011, Month.MARCH, 1, 0, 0));
        delivery.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
        delivery.setSecurity(security);
        delivery.setMonetaryAmount(Money.of(CurrencyUnit.EUR, 100_00));
        delivery.addUnit(new Transaction.Unit(Transaction.Unit.Type.TAX, Money.of(CurrencyUnit.EUR, 10_00)));
        delivery.addUnit(new Transaction.Unit(Transaction.Unit.Type.FEE, Money.of(CurrencyUnit.EUR, 10_00)));

        assertThat(delivery.getGrossValue(), is(Money.of(CurrencyUnit.EUR, 80_00)));

        portfolio.addTransaction(delivery);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        assertThat(snapshot.getValue(CategoryType.EARNINGS), is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(snapshot.getValue(CategoryType.FEES), is(Money.of(CurrencyUnit.EUR, 10_00)));
        assertThat(snapshot.getValue(CategoryType.TAXES), is(Money.of(CurrencyUnit.EUR, 10_00)));
        assertThat(snapshot.getValue(CategoryType.TRANSFERS), is(Money.of(CurrencyUnit.EUR, 100_00)));

        assertThatCalculationWorksOut(snapshot, converter);
    }

    @Test
    public void testInterestCharge()
    {
        Client client = new Client();

        Account account = new AccountBuilder() //
                        .interest_charge(LocalDateTime.of(2011, 01, 01, 0, 0), Values.Amount.factorize(100)) //
                        .addTo(client);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        assertThat(snapshot.getValue(CategoryType.EARNINGS),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(-100))));
        assertThat(snapshot.getCategoryByType(CategoryType.EARNINGS).getPositions().size(), is(1));
        assertThat(snapshot.getCategoryByType(CategoryType.EARNINGS).getPositions().get(0).getValuation(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(-100))));

        assertThat(snapshot.getEarnings().size(), is(1));
        assertThat(snapshot.getEarnings().get(0).getTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100))));

        GroupEarningsByAccount grouping = new GroupEarningsByAccount(snapshot);
        assertThat(grouping.getItems().size(), is(1));
        assertThat(grouping.getItems().get(0).getAccount(), is(account));
        assertThat(grouping.getItems().get(0).getSum(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(-100))));

        assertThatCalculationWorksOut(snapshot, converter);
    }

    @Test
    public void testFeesRefund()
    {
        Client client = new Client();

        new AccountBuilder() //
                        .fees_refund(LocalDateTime.of(2011, 01, 01, 0, 0), Values.Amount.factorize(100)) //
                        .addTo(client);

        CurrencyConverter converter = new TestCurrencyConverter();
        ClientPerformanceSnapshot snapshot = new ClientPerformanceSnapshot(client, converter, startDate, endDate);

        assertThat(snapshot.getValue(CategoryType.FEES), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(-100))));

        assertThat(snapshot.getFees().size(), is(1));
        assertThat(snapshot.getFees().get(0).getTransaction().getMonetaryAmount(),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(100))));

        assertThatCalculationWorksOut(snapshot, converter);
    }

    private void assertThatCalculationWorksOut(ClientPerformanceSnapshot snapshot, CurrencyConverter converter)
    {
        MutableMoney valueAtEndOfPeriod = MutableMoney.of(converter.getTermCurrency());
        valueAtEndOfPeriod.add(snapshot.getValue(CategoryType.INITIAL_VALUE));
        valueAtEndOfPeriod.add(snapshot.getValue(CategoryType.CAPITAL_GAINS));
        valueAtEndOfPeriod.add(snapshot.getValue(CategoryType.EARNINGS));
        valueAtEndOfPeriod.subtract(snapshot.getValue(CategoryType.FEES));
        valueAtEndOfPeriod.subtract(snapshot.getValue(CategoryType.TAXES));
        valueAtEndOfPeriod.add(snapshot.getValue(CategoryType.CURRENCY_GAINS));
        valueAtEndOfPeriod.add(snapshot.getValue(CategoryType.TRANSFERS));

        assertThat(valueAtEndOfPeriod.toMoney(), is(snapshot.getValue(CategoryType.FINAL_VALUE)));
    }
}
