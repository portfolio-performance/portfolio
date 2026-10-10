package name.abuchen.portfolio.snapshot.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CostMethod;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.TaxesAndFees;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.AccruedInterestScenario;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot.CategoryType;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.SecurityPosition;
import name.abuchen.portfolio.util.Interval;

@SuppressWarnings("nls")
public class SecurityPerformanceSnapshotTest
{

    @Test
    public void testBigPurchases()
    {
        Client client = new Client();

        Security security = new SecurityBuilder().addTo(client);

        new PortfolioBuilder()
                        .buy(security, "2018-05-01", Values.Share.factorize(500000), Values.Amount.factorize(450000))
                        .sell(security, "2018-05-08", Values.Share.factorize(500000), Values.Amount.factorize(494500))
                        .addTo(client);

        final Interval interval = Interval.of(LocalDate.parse("2018-04-01"), LocalDate.parse("2018-06-01"));
        LazySecurityPerformanceSnapshot snapshot = LazySecurityPerformanceSnapshot.create(client,
                        new TestCurrencyConverter(), interval);

        assertThat(snapshot.getRecords(), hasSize(1));

        LazySecurityPerformanceRecord record = snapshot.getRecords().get(0);
        assertThat(record.getSecurity(), is(security));

        assertThat(record.getSharesHeld(), is(0L));

        assertThat(record.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED), is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(record.getCostPerSharesHeld(CostMethod.FIFO, TaxesAndFees.NOT_INCLUDED),
                        is(Quote.of(CurrencyUnit.EUR, 0)));

        assertThat(record.getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED), is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(record.getCostPerSharesHeld(CostMethod.MOVING_AVERAGE, TaxesAndFees.NOT_INCLUDED),
                        is(Quote.of(CurrencyUnit.EUR, 0)));
    }

    @Test
    public void testBigPurchases2()
    {
        Client client = new Client();

        Security security = new SecurityBuilder().addTo(client);

        new PortfolioBuilder()
                        .buy(security, "2018-05-01", Values.Share.factorize(500000), Values.Amount.factorize(450000))
                        .sell(security, "2018-05-08", Values.Share.factorize(1), Values.Amount.factorize(0.989))
                        .addTo(client);

        Interval reportingPeriod = Interval.of(LocalDate.parse("2018-04-01"), LocalDate.parse("2018-06-01"));
        LazySecurityPerformanceSnapshot snapshot = LazySecurityPerformanceSnapshot.create(client,
                        new TestCurrencyConverter(), reportingPeriod);

        assertThat(snapshot.getRecords(), hasSize(1));

        LazySecurityPerformanceRecord record = snapshot.getRecords().get(0);
        assertThat(record.getSecurity(), is(security));

        assertThat(record.getSharesHeld(), is(Values.Share.factorize(499999)));

        assertThat(record.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(450000 - 0.9))));
        assertThat(record.getCostPerSharesHeld(CostMethod.FIFO, TaxesAndFees.NOT_INCLUDED),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(0.9))));

        assertThat(record.getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(450000 - 0.9))));
        assertThat(record.getCostPerSharesHeld(CostMethod.MOVING_AVERAGE, TaxesAndFees.NOT_INCLUDED),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(0.9))));

        SecurityPosition position = ClientSnapshot.create(client, new TestCurrencyConverter(), reportingPeriod.getEnd())
                        .getPositionsByVehicle().get(security).getPosition();

        assertThat(position.getShares(), is(record.getSharesHeld()));
        assertThat(position.calculateValue(), is(record.getMarketValue()));
    }

    @Test
    public void testCostPerSharesHeldOfPercentageQuotedSecurity()
    {
        Client client = new Client();

        Security bond = new SecurityBuilder().addTo(client);
        bond.setPercentageQuoted(true);

        new PortfolioBuilder() //
                        .buy(bond, "2018-05-01", Values.Share.factorize(1000), Values.Amount.factorize(1005)) //
                        .addTo(client);

        LazySecurityPerformanceSnapshot snapshot = LazySecurityPerformanceSnapshot.create(client,
                        new TestCurrencyConverter(),
                        Interval.of(LocalDate.parse("2018-04-01"), LocalDate.parse("2018-06-01")));

        assertThat(snapshot.getRecords(), hasSize(1));
        LazySecurityPerformanceRecord record = snapshot.getRecords().get(0);

        // the purchase price is given in percent like the security prices
        assertThat(record.getCostPerSharesHeld(CostMethod.FIFO, TaxesAndFees.NOT_INCLUDED),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(100.5))));
        assertThat(record.getCostPerSharesHeld(CostMethod.MOVING_AVERAGE, TaxesAndFees.NOT_INCLUDED),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(100.5))));
    }

    @Test
    public void testAccruedInterestIsIncomeAndNotCost()
    {
        var scenario = new AccruedInterestScenario();

        // while holding the bond
        var record = LazySecurityPerformanceSnapshot
                        .create(scenario.client, new TestCurrencyConverter(),
                                        Interval.of(LocalDate.parse("2024-01-01"), LocalDate.parse("2024-06-30")))
                        .getRecords().get(0);

        assertThat(record.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED), is(AccruedInterestScenario.eur(1005)));
        assertThat(record.getCost(CostMethod.FIFO, TaxesAndFees.NOT_INCLUDED), is(AccruedInterestScenario.eur(1000)));
        assertThat(record.getCost(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED),
                        is(AccruedInterestScenario.eur(1005)));
        assertThat(record.getCostPerSharesHeld(CostMethod.FIFO, TaxesAndFees.NOT_INCLUDED),
                        is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(100))));
        assertThat(record.getUnrealizedCapitalGains(CostMethod.FIFO).getCapitalGains(),
                        is(AccruedInterestScenario.eur(0)));

        // coupon 30.00 minus accrued interest paid 10.00
        assertThat(record.getSumOfDividends(), is(AccruedInterestScenario.eur(20)));
        assertThat(record.getDividendEventCount(), is(1));
        assertThat(record.getRateOfReturnPerYear(), is(closeTo(20.0 / 1005.0, 1e-9)));

        // whole year
        record = LazySecurityPerformanceSnapshot
                        .create(scenario.client, new TestCurrencyConverter(),
                                        Interval.of(LocalDate.parse("2024-01-01"), LocalDate.parse("2024-12-31")))
                        .getRecords().get(0);

        // sold at 101 %: the accrued interest received is no capital gain
        assertThat(record.getRealizedCapitalGains(CostMethod.FIFO).getCapitalGains(),
                        is(AccruedInterestScenario.eur(10)));
        assertThat(record.getRealizedCapitalGains(CostMethod.MOVING_AVERAGE).getCapitalGains(),
                        is(AccruedInterestScenario.eur(10)));
        assertThat(record.getRealizedCapitalGains(CostMethod.FIFO, TaxesAndFees.INCLUDED).getCapitalGains(),
                        is(AccruedInterestScenario.eur(0)));
        assertThat(record.getRealizedCapitalGains(CostMethod.MOVING_AVERAGE, TaxesAndFees.INCLUDED)
                        .getCapitalGains(), is(AccruedInterestScenario.eur(0)));

        // coupon 30.00 - paid 10.00 + received 5.00
        assertThat(record.getSumOfDividends(), is(AccruedInterestScenario.eur(25)));
        assertThat(record.getRateOfReturnPerYear(), is(closeTo(25.0 / 1005.0, 1e-9)));
        assertThat(record.getDividendEventCount(), is(1));

        // cash flows (incl. accrued interest) are unchanged: 1,010 + 30 - 1,015
        assertThat(record.getDelta(), is(AccruedInterestScenario.eur(25)));
    }

    @Test
    public void testAccruedInterestInForeignCurrency()
    {
        // EUR bond bought via USD account at 1.10 USD/EUR (the historical rate
        // of the test converter differs: 1.1708)
        Client client = new Client();
        Account account = new Account("USD");
        account.setCurrencyCode(CurrencyUnit.USD);
        client.addAccount(account);
        Portfolio portfolio = new Portfolio("Portfolio");
        portfolio.setReferenceAccount(account);
        client.addPortfolio(portfolio);
        Security bond = new Security("Bond", CurrencyUnit.EUR);
        bond.setPercentageQuoted(true);
        client.addSecurity(bond);

        BigDecimal rate = new BigDecimal("1.1");
        BuySellEntry entry = new BuySellEntry(portfolio, account);
        entry.setType(PortfolioTransaction.Type.BUY);
        entry.setDate(LocalDateTime.parse("2015-01-15T00:00"));
        entry.setSecurity(bond);
        entry.setShares(Values.Share.factorize(1000));
        entry.setMonetaryAmount(Money.of(CurrencyUnit.USD, Values.Amount.factorize(1116.50)));
        PortfolioTransaction t = entry.getPortfolioTransaction();
        t.addUnit(new Unit(Unit.Type.GROSS_VALUE, Money.of(CurrencyUnit.USD, Values.Amount.factorize(1100)),
                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000)), rate));
        t.addUnit(new Unit(Unit.Type.FEE, Money.of(CurrencyUnit.USD, Values.Amount.factorize(5.50))));
        t.addUnit(new Unit(Unit.Type.ACCRUED_INTEREST, Money.of(CurrencyUnit.USD, Values.Amount.factorize(11)),
                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(10)), rate));
        entry.insert();

        LazySecurityPerformanceRecord record = LazySecurityPerformanceSnapshot
                        .create(client, new TestCurrencyConverter(),
                                        Interval.of(LocalDate.parse("2015-01-01"), LocalDate.parse("2015-01-16")))
                        .getRecords().get(0);

        // amount and accrued interest both converted with the transaction rate
        assertThat(record.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1005))));
        assertThat(record.getCost(CostMethod.FIFO, TaxesAndFees.NOT_INCLUDED),
                        is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(1000))));
        assertThat(record.getSumOfDividends(), is(Money.of(CurrencyUnit.EUR, Values.Amount.factorize(-10))));
    }

    @Test
    public void testAccruedInterestWithAllCostMethods()
    {
        var scenario = new AccruedInterestScenario();

        // while holding the bond (price 100 %)
        var record = LazySecurityPerformanceSnapshot
                        .create(scenario.client, new TestCurrencyConverter(),
                                        Interval.of(LocalDate.parse("2024-01-01"), LocalDate.parse("2024-06-30")))
                        .getRecords().get(0);

        for (CostMethod method : CostMethod.values())
        {
            // the accrued interest is neither part of the cost ...
            assertThat(method.name(), record.getCost(method, TaxesAndFees.NOT_INCLUDED),
                            is(AccruedInterestScenario.eur(1000)));
            assertThat(method.name(), record.getCostPerSharesHeld(method, TaxesAndFees.NOT_INCLUDED),
                            is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(100))));
            // ... nor of the cost including fees (5.00)
            assertThat(method.name(), record.getCost(method, TaxesAndFees.INCLUDED),
                            is(AccruedInterestScenario.eur(1005)));
            assertThat(method.name(), record.getCostPerSharesHeld(method, TaxesAndFees.INCLUDED),
                            is(Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(100.5))));

            // ... and therefore not of the unrealized capital gains
            assertThat(method.name(), record.getUnrealizedCapitalGains(method).getCapitalGains(),
                            is(AccruedInterestScenario.eur(0)));
            assertThat(method.name(),
                            record.getUnrealizedCapitalGains(method, TaxesAndFees.INCLUDED).getCapitalGains(),
                            is(AccruedInterestScenario.eur(-5)));
            assertThat(method.name(), record.getCapitalGainsOnHoldings(method), is(AccruedInterestScenario.eur(-5)));

            // total rate of return incl. dividends: (30.00 - 10.00) / 1,005.00
            assertThat(method.name(), record.getTotalRateOfReturnDiv(method), is(20.0 / 1005.0));
        }
    }

    @Test
    public void testAccruedInterestPaidWithoutCouponYet()
    {
        var scenario = new AccruedInterestScenario();

        // bought on 2024-03-01, the first coupon is paid on 2024-06-28
        var interval = Interval.of(LocalDate.parse("2024-01-01"), LocalDate.parse("2024-04-30"));
        var record = LazySecurityPerformanceSnapshot.create(scenario.client, new TestCurrencyConverter(), interval)
                        .getRecords().get(0);

        // the accrued interest paid is a negative income until the coupon
        // is paid
        assertThat(record.getSumOfDividends(), is(AccruedInterestScenario.eur(-10)));
        assertThat(record.getDividendEventCount(), is(0));
        assertThat(record.getLastDividendPayment(), is((LocalDate) null));
        // without a coupon there is no rate of return to offset against
        assertThat(record.getRateOfReturnPerYear(), is(0.0));
        assertThat(record.getTotalRateOfReturnDiv(CostMethod.FIFO), is(-10.0 / 1005.0));
        assertThat(record.getCost(CostMethod.FIFO, TaxesAndFees.INCLUDED), is(AccruedInterestScenario.eur(1005)));

        var snapshot = new ClientPerformanceSnapshot(scenario.client, new TestCurrencyConverter(), interval);
        assertThat(snapshot.getValue(CategoryType.EARNINGS), is(AccruedInterestScenario.eur(-10)));
        assertThat(snapshot.getValue(CategoryType.INITIAL_VALUE, CategoryType.TRANSFERS, CategoryType.CAPITAL_GAINS,
                        CategoryType.REALIZED_CAPITAL_GAINS, CategoryType.EARNINGS, CategoryType.CURRENCY_GAINS)
                        .subtract(snapshot.getValue(CategoryType.FEES, CategoryType.TAXES)),
                        is(snapshot.getValue(CategoryType.FINAL_VALUE)));
    }

    @Test
    public void testAccruedInterestIsOffsetAgainstNextAndPreviousCoupon()
    {
        // bought in December, coupons in January, sold in June
        Client client = new Client();
        Account account = new Account("Account");
        account.setCurrencyCode(CurrencyUnit.EUR);
        client.addAccount(account);
        Portfolio portfolio = new Portfolio("Portfolio");
        portfolio.setReferenceAccount(account);
        client.addPortfolio(portfolio);
        Security bond = new Security("Bond", CurrencyUnit.EUR);
        bond.setPercentageQuoted(true);
        bond.addPrice(new SecurityPrice(LocalDate.parse("2023-12-01"), Values.Quote.factorize(100)));
        client.addSecurity(bond);

        // 1,000.00 + accrued interest 25.00 + fees 5.00
        BuySellEntry buy = new BuySellEntry(portfolio, account);
        buy.setType(PortfolioTransaction.Type.BUY);
        buy.setDate(LocalDateTime.parse("2023-12-01T00:00"));
        buy.setSecurity(bond);
        buy.setShares(Values.Share.factorize(1000));
        buy.setMonetaryAmount(AccruedInterestScenario.eur(1030));
        buy.getPortfolioTransaction().addUnit(new Unit(Unit.Type.ACCRUED_INTEREST, AccruedInterestScenario.eur(25)));
        buy.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, AccruedInterestScenario.eur(5)));
        buy.insert();

        for (String date : new String[] { "2024-01-15T00:00", "2025-01-15T00:00" })
        {
            AccountTransaction coupon = new AccountTransaction();
            coupon.setType(AccountTransaction.Type.DIVIDENDS);
            coupon.setDateTime(LocalDateTime.parse(date));
            coupon.setSecurity(bond);
            coupon.setShares(Values.Share.factorize(1000));
            coupon.setMonetaryAmount(AccruedInterestScenario.eur(30));
            account.addTransaction(coupon);
        }

        // 1,000.00 + accrued interest 15.00 - fees 5.00
        BuySellEntry sell = new BuySellEntry(portfolio, account);
        sell.setType(PortfolioTransaction.Type.SELL);
        sell.setDate(LocalDateTime.parse("2025-06-16T00:00"));
        sell.setSecurity(bond);
        sell.setShares(Values.Share.factorize(1000));
        sell.setMonetaryAmount(AccruedInterestScenario.eur(1010));
        sell.getPortfolioTransaction().addUnit(new Unit(Unit.Type.ACCRUED_INTEREST, AccruedInterestScenario.eur(15)));
        sell.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, AccruedInterestScenario.eur(5)));
        sell.insert();

        var record = LazySecurityPerformanceSnapshot
                        .create(client, new TestCurrencyConverter(),
                                        Interval.of(LocalDate.parse("2023-01-01"), LocalDate.parse("2025-12-31")))
                        .getRecords().get(0);

        // 30 + 30 - 25 + 15
        assertThat(record.getSumOfDividends(), is(AccruedInterestScenario.eur(50)));

        // the accrued interest does not add a year with a (negative) rate:
        // 2024: (30 - 25) / 1,005, 2025: (30 + 15) / 1,005
        assertThat(record.getRateOfReturnPerYear(), is(closeTo((5.0 + 45.0) / 1005.0 / 2, 1e-9)));

        // the payments themselves are unchanged
        assertThat(record.getDividendEventCount(), is(2));
        assertThat(record.getPeriodicity(), is(BaseSecurityPerformanceRecord.Periodicity.ANNUAL));
    }
}
