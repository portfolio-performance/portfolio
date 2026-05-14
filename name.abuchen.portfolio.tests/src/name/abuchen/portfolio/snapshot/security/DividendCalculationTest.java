package name.abuchen.portfolio.snapshot.security;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.junit.TestCurrencyConverter;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.security.BaseSecurityPerformanceRecord.Periodicity;

@SuppressWarnings("nls")
public class DividendCalculationTest
{
    Account account;
    Portfolio portfolio;
    Security security;
    CurrencyConverter converter;

    @Before
    public void setup()
    {
        this.account = new Account();
        this.portfolio = new Portfolio();
        this.security = new Security("ADIDAS ORD", "EUR");
        this.converter = new TestCurrencyConverter();
    }

    /**
     * Creates a dividend payment of 100$ with 10Shares -> 10$/Share
     * 
     * @param date
     * @return
     */
    public CalculationLineItem createDividendTransaction(LocalDateTime date)
    {
        AccountTransaction t = new AccountTransaction();
        t.setType(AccountTransaction.Type.DIVIDENDS);
        t.setSecurity(security);
        t.setDateTime(date);
        t.setAmount(100L);
        t.setShares(10);
        t.setCurrencyCode(security.getCurrencyCode());

        return CalculationLineItem.of(account, t);
    }

    @Test
    public void noTransactionTest()
    {
        List<CalculationLineItem> transactions = new ArrayList<>();

        DividendCalculation dc = Calculation.perform(DividendCalculation.class, converter, security, transactions);

        assertEquals(0, dc.getNumOfEvents());
        assertEquals(Periodicity.NONE, dc.getPeriodicity());
        assertEquals(0.0, dc.getRateOfReturnPerYear(), 0.0);
    }

    @Test
    public void oneTransactionTest()
    {

        List<CalculationLineItem> transactions = new ArrayList<>();
        transactions.add(createDividendTransaction(LocalDateTime.of(2019, 01, 15, 12, 00)));

        DividendCalculation dividends = Calculation.perform(DividendCalculation.class, converter, security,
                        transactions);

        assertEquals(1, dividends.getNumOfEvents());
        assertEquals(Periodicity.UNKNOWN, dividends.getPeriodicity());
        assertEquals(transactions.get(0).getDateTime().toLocalDate(), dividends.getLastDividendPayment());

    }

    @Test
    public void periodicityAnualTest()
    {

        List<CalculationLineItem> transactions = new ArrayList<>();
        transactions.add(createDividendTransaction(LocalDateTime.of(2019, 01, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2020, 01, 15, 12, 00)));

        DividendCalculation dividends = Calculation.perform(DividendCalculation.class, converter, security,
                        transactions);

        assertEquals(Periodicity.ANNUAL, dividends.getPeriodicity());
    }

    @Test
    public void periodicityAnnualWithGap()
    {

        List<CalculationLineItem> transactions = new ArrayList<>();
        transactions.add(createDividendTransaction(LocalDateTime.of(2015, 01, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2016, 01, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2017, 01, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2018, 01, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2020, 01, 15, 12, 00)));

        DividendCalculation dividends = Calculation.perform(DividendCalculation.class, converter, security,
                        transactions);

        assertEquals(Periodicity.ANNUAL, dividends.getPeriodicity());
    }

    @Test
    public void periodicityTransitionToQuaterly()
    {

        List<CalculationLineItem> transactions = new ArrayList<>();
        transactions.add(createDividendTransaction(LocalDateTime.of(2016, 01, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2017, 01, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2017, 04, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2017, 07, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2017, 10, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2018, 01, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2018, 04, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2018, 07, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2018, 10, 15, 12, 00)));

        DividendCalculation dividends = Calculation.perform(DividendCalculation.class, converter, security,
                        transactions);

        assertEquals(Periodicity.QUARTERLY, dividends.getPeriodicity());
    }

    @Test
    public void periodicitySemiAnnualTest()
    {

        List<CalculationLineItem> transactions = new ArrayList<>();
        transactions.add(createDividendTransaction(LocalDateTime.of(2019, 01, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2019, 07, 15, 12, 00)));

        DividendCalculation dividends = Calculation.perform(DividendCalculation.class, converter, security,
                        transactions);

        assertEquals(Periodicity.SEMIANNUAL, dividends.getPeriodicity());
    }

    @Test
    public void periodicityQuarterlyTest()
    {

        List<CalculationLineItem> transactions = new ArrayList<>();
        transactions.add(createDividendTransaction(LocalDateTime.of(2019, 01, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2019, 04, 15, 12, 00)));

        DividendCalculation dividends = Calculation.perform(DividendCalculation.class, converter, security,
                        transactions);

        assertEquals(Periodicity.QUARTERLY, dividends.getPeriodicity());
    }

    @Test
    public void periodicityMonthlyTest()
    {

        List<CalculationLineItem> transactions = new ArrayList<>();
        transactions.add(createDividendTransaction(LocalDateTime.of(2019, 01, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2019, 02, 15, 12, 00)));

        DividendCalculation dividends = Calculation.perform(DividendCalculation.class, converter, security,
                        transactions);

        assertEquals(Periodicity.MONTHLY, dividends.getPeriodicity());
    }

    @Test
    public void rateOfReturnCalculationTest()
    {

        List<CalculationLineItem> transactions = new ArrayList<>();
        // We buy some shares, 1000$, 10 Shares -> 100/Share

        transactions.add(CalculationLineItem.of(new Portfolio(),
                        new PortfolioTransaction(LocalDateTime.of(2019, 01, 14, 12, 00), security.getCurrencyCode(),
                                        1000L, security, 10L, Type.BUY, 0L, 0L)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2019, 01, 15, 12, 00)));
        transactions.add(createDividendTransaction(LocalDateTime.of(2020, 01, 15, 12, 00)));

        // We need to calculate the costs, in order to get the average return
        @SuppressWarnings("unused")
        CostCalculation cost = Calculation.perform(CostCalculation.class, converter, security, transactions);
        DividendCalculation dividends = Calculation.perform(DividendCalculation.class, converter, security,
                        transactions);

        assertEquals(0.1, dividends.getRateOfReturnPerYear(), 0.0);
    }

    @Test
    public void rateOfReturnWithCurrencyMismatchBugTest()
    {
        // Test case that catches the currency mismatch bug:
        // When transaction currency (USD) differs from term currency (EUR),
        // the bug divides raw USD amount by converted EUR cost without proper conversion.
        // 
        // Purchase: 10 shares at 100 USD per share = 1000 USD total
        // Dividend: 50 USD (5 USD per share) on 2015-01-15
        // 
        // Using TestCurrencyConverter with EUR term currency:
        // Exchange rate on 2015-01-15: ~1.1708 EUR per USD (from test data)
        // - Purchase cost in EUR: 1000 USD * 1.1708 = 1170.8 EUR
        // - Dividend in EUR: 50 USD * 1.1708 = 58.54 EUR
        // - Correct rate: 58.54 / 1170.8 = 0.05 (5%)
        //
        // BUG: Code uses raw dividend amount (5000 in stored format, USD) divided by
        //      converted cost (in EUR), which gives wrong result
        //      With buggy code: ~0.0588 (wrong - dividing USD by EUR)
        //      With fixed code: ~0.05 (correct - both in EUR)

        Security testSecurity = new Security("Test Security", "USD");
        testSecurity.addPrice(new SecurityPrice(LocalDateTime.of(2015, 1, 15, 0, 0).toLocalDate(),
                        Values.Quote.factorize(100)));

        List<CalculationLineItem> transactions = new ArrayList<>();

        // Purchase: 10 shares at 100 USD = 1000 USD
        transactions.add(CalculationLineItem.of(new Portfolio(), new PortfolioTransaction(
                        LocalDateTime.of(2015, 1, 14, 12, 0), testSecurity.getCurrencyCode(),
                        Values.Amount.factorize(1000), testSecurity, Values.Share.factorize(10), Type.BUY, 0L, 0L)));

        // Dividend: 50 USD (5 USD per share)
        AccountTransaction dividend = new AccountTransaction();
        dividend.setType(AccountTransaction.Type.DIVIDENDS);
        dividend.setSecurity(testSecurity);
        dividend.setDateTime(LocalDateTime.of(2015, 1, 15, 12, 0));
        dividend.setAmount(Values.Amount.factorize(50));
        dividend.setShares(Values.Share.factorize(10));
        dividend.setCurrencyCode(testSecurity.getCurrencyCode());

        transactions.add(CalculationLineItem.of(new Account(), dividend));

        @SuppressWarnings("unused")
        CostCalculation cost = Calculation.perform(CostCalculation.class, converter, testSecurity, transactions);
        DividendCalculation dividends = Calculation.perform(DividendCalculation.class, converter, testSecurity,
                        transactions);

        // Expected: 50 USD / 1000 USD = 0.05 (5%)
        // After currency conversion to EUR: ~58.54 EUR / ~1170.8 EUR = 0.05
        // With bug: Uses raw USD amount / converted EUR cost = ~0.0588 (wrong!)
        // With fix: Uses converted dividend / converted cost = ~0.05 (correct)
        // This test will FAIL with the buggy code (~0.0588) and PASS with the fix (~0.05)
        // Tolerance is tight (0.005) to catch the bug: 0.0588 - 0.05 = 0.0088 > 0.005
        assertEquals(0.05, dividends.getRateOfReturnPerYear(), 0.005);
    }

    private CalculationLineItem buy(LocalDateTime date, double amount, double shares)
    {
        return CalculationLineItem.of(portfolio, new PortfolioTransaction(date, security.getCurrencyCode(),
                        Values.Amount.factorize(amount), security, Values.Share.factorize(shares), Type.BUY, 0L, 0L));
    }

    @SuppressWarnings("unused")
    private CalculationLineItem buy(LocalDateTime date, double amount, double shares, double fees)
    {
        return CalculationLineItem.of(portfolio,
                        new PortfolioTransaction(date, security.getCurrencyCode(), Values.Amount.factorize(amount),
                                        security, Values.Share.factorize(shares), Type.BUY,
                                        Values.Amount.factorize(fees), 0L));
    }

    private CalculationLineItem sell(LocalDateTime date, double amount, double shares)
    {
        return CalculationLineItem.of(portfolio, new PortfolioTransaction(date, security.getCurrencyCode(),
                        Values.Amount.factorize(amount), security, Values.Share.factorize(shares), Type.SELL, 0L, 0L));
    }

    private CalculationLineItem dividend(LocalDateTime date, double amount, double shares)
    {
        return dividend(account, date, amount, shares);
    }

    private CalculationLineItem dividend(Account dividendAccount, LocalDateTime date, double amount, double shares)
    {
        AccountTransaction t = new AccountTransaction();
        t.setType(AccountTransaction.Type.DIVIDENDS);
        t.setSecurity(security);
        t.setDateTime(date);
        t.setAmount(Values.Amount.factorize(amount));
        t.setShares(Values.Share.factorize(shares));
        t.setCurrencyCode(security.getCurrencyCode());

        return CalculationLineItem.of(dividendAccount, t);
    }

    private DividendCalculation calculate(List<CalculationLineItem> transactions)
    {
        Calculation.perform(CostCalculation.class, converter, security, transactions);
        return Calculation.perform(DividendCalculation.class, converter, security, transactions);
    }

    @Test
    public void rateOfReturnThreeYearsWithConstantPosition()
    {
        List<CalculationLineItem> transactions = new ArrayList<>();

        transactions.add(buy(LocalDateTime.of(2019, 1, 7, 12, 0), 1330.25, 61));

        transactions.add(dividend(LocalDateTime.of(2019, 6, 15, 12, 0), 17.49, 61));
        transactions.add(dividend(LocalDateTime.of(2020, 6, 15, 12, 0), 17.49, 61));
        transactions.add(dividend(LocalDateTime.of(2021, 6, 15, 12, 0), 17.49, 61));

        DividendCalculation dividends = calculate(transactions);

        assertEquals(52.47 / 1330.25 / 3, dividends.getRateOfReturnPerYear(), 0.00001);
    }

    @Test
    public void rateOfReturnThreeYearsWithSavingsPlan()
    {
        List<CalculationLineItem> transactions = new ArrayList<>();

        transactions.add(buy(LocalDateTime.of(2019, 1, 1, 12, 0), 1000, 10));
        transactions.add(dividend(LocalDateTime.of(2019, 6, 15, 12, 0), 10, 10));

        transactions.add(buy(LocalDateTime.of(2020, 1, 1, 12, 0), 1000, 10));
        transactions.add(dividend(LocalDateTime.of(2020, 6, 15, 12, 0), 20, 20));

        transactions.add(buy(LocalDateTime.of(2021, 1, 1, 12, 0), 1000, 10));
        transactions.add(dividend(LocalDateTime.of(2021, 6, 15, 12, 0), 30, 30));

        DividendCalculation dividends = calculate(transactions);

        assertEquals(0.01, dividends.getRateOfReturnPerYear(), 0.00001);
    }

    @Test
    public void rateOfReturnThreeYearsWithAdditionalPurchaseDuringPeriod()
    {
        List<CalculationLineItem> transactions = new ArrayList<>();

        transactions.add(buy(LocalDateTime.of(2019, 1, 1, 12, 0), 1000, 10));
        transactions.add(dividend(LocalDateTime.of(2019, 6, 15, 12, 0), 100, 10));

        transactions.add(buy(LocalDateTime.of(2020, 1, 1, 12, 0), 1000, 10));
        transactions.add(dividend(LocalDateTime.of(2020, 6, 15, 12, 0), 100, 20));
        transactions.add(dividend(LocalDateTime.of(2021, 6, 15, 12, 0), 100, 20));

        DividendCalculation dividends = calculate(transactions);

        assertEquals((0.10 + 0.05 + 0.05) / 3, dividends.getRateOfReturnPerYear(), 0.00001);
    }

    @Test
    public void rateOfReturnThreeYearsWithPartialSaleDuringPeriod()
    {
        List<CalculationLineItem> transactions = new ArrayList<>();

        transactions.add(buy(LocalDateTime.of(2019, 1, 1, 12, 0), 1000, 100));
        transactions.add(dividend(LocalDateTime.of(2019, 6, 15, 12, 0), 100, 100));

        transactions.add(sell(LocalDateTime.of(2020, 1, 1, 12, 0), 500, 50));
        transactions.add(dividend(LocalDateTime.of(2020, 6, 15, 12, 0), 50, 50));
        transactions.add(dividend(LocalDateTime.of(2021, 6, 15, 12, 0), 50, 50));

        DividendCalculation dividends = calculate(transactions);

        assertEquals(0.10, dividends.getRateOfReturnPerYear(), 0.00001);
    }

    @Test
    public void rateOfReturnThreeYearsWithFullSaleAfterLastDividend()
    {
        List<CalculationLineItem> transactions = new ArrayList<>();

        transactions.add(buy(LocalDateTime.of(2019, 1, 1, 12, 0), 1000, 100));
        transactions.add(dividend(LocalDateTime.of(2019, 6, 15, 12, 0), 100, 100));
        transactions.add(dividend(LocalDateTime.of(2020, 6, 15, 12, 0), 100, 100));
        transactions.add(dividend(LocalDateTime.of(2021, 6, 15, 12, 0), 100, 100));
        transactions.add(sell(LocalDateTime.of(2021, 12, 31, 12, 0), 1000, 100));

        DividendCalculation dividends = calculate(transactions);

        assertEquals(0.10, dividends.getRateOfReturnPerYear(), 0.00001);
    }

    @Test
    public void rateOfReturnThreeYearsWithSaleAndRepurchase()
    {
        List<CalculationLineItem> transactions = new ArrayList<>();

        transactions.add(buy(LocalDateTime.of(2019, 1, 1, 12, 0), 1000, 100));
        transactions.add(dividend(LocalDateTime.of(2019, 6, 15, 12, 0), 100, 100));

        transactions.add(sell(LocalDateTime.of(2019, 12, 31, 12, 0), 1000, 100));

        transactions.add(buy(LocalDateTime.of(2020, 1, 1, 12, 0), 2000, 100));
        transactions.add(dividend(LocalDateTime.of(2020, 6, 15, 12, 0), 100, 100));
        transactions.add(dividend(LocalDateTime.of(2021, 6, 15, 12, 0), 100, 100));

        DividendCalculation dividends = calculate(transactions);

        assertEquals((0.10 + 0.05 + 0.05) / 3, dividends.getRateOfReturnPerYear(), 0.00001);
    }

    @Test
    public void rateOfReturnWithMultipleAccountsOnSameDividendDate()
    {
        List<CalculationLineItem> transactions = new ArrayList<>();

        Account secondAccount = new Account();

        transactions.add(buy(LocalDateTime.of(2019, 1, 1, 12, 0), 1000, 100));

        transactions.add(dividend(account, LocalDateTime.of(2019, 6, 15, 12, 0), 40, 40));
        transactions.add(dividend(secondAccount, LocalDateTime.of(2019, 6, 15, 12, 0), 60, 60));

        DividendCalculation dividends = calculate(transactions);

        assertEquals(0.10, dividends.getRateOfReturnPerYear(), 0.00001);
    }

    @Test
    public void rateOfReturnWithDividendForPartialPosition()
    {
        List<CalculationLineItem> transactions = new ArrayList<>();

        transactions.add(buy(LocalDateTime.of(2019, 1, 1, 12, 0), 1000, 100));
        transactions.add(dividend(LocalDateTime.of(2019, 6, 15, 12, 0), 40, 40));

        DividendCalculation dividends = calculate(transactions);

        assertEquals(0.10, dividends.getRateOfReturnPerYear(), 0.00001);
    }
}
