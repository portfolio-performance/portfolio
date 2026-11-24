package name.abuchen.portfolio.snapshot.security;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
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
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.security.BaseSecurityPerformanceRecord.Periodicity;

@SuppressWarnings("nls")
public class DividendCalculationTest
{
    Account account;
    Security security;
    CurrencyConverter converter;

    @Before
    public void setup()
    {
        this.account = new Account();
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

    /**
     * If the position is sold before the payment (for example between ex-date
     * and payment date), there is no moving average cost. Then the rate of
     * return is based on the market value of the shares at the payment date.
     */
    @Test
    public void rateOfReturnIfPositionIsSoldBeforePayment()
    {
        security.addPrice(new SecurityPrice(LocalDate.parse("2020-02-03"), Values.Quote.factorize(110)));

        List<CalculationLineItem> transactions = createSaleBeforePayment(security);

        Calculation.perform(CostCalculation.class, converter, security, transactions);
        DividendCalculation dividends = Calculation.perform(DividendCalculation.class, converter, security,
                        transactions);

        // 2019: 50 / 1000, 2020: 50 / (10 * 110)
        assertEquals((0.05 + 50 / 1100d) / 2, dividends.getRateOfReturnPerYear(), 0.000001);
    }

    /**
     * Without moving average cost and without a quote, the rate of return of
     * a payment is unknown. It must not turn the rate of return per year into
     * NaN.
     */
    @Test
    public void rateOfReturnIgnoresPaymentWithoutCostAndQuote()
    {
        List<CalculationLineItem> transactions = createSaleBeforePayment(security);

        Calculation.perform(CostCalculation.class, converter, security, transactions);
        DividendCalculation dividends = Calculation.perform(DividendCalculation.class, converter, security,
                        transactions);

        assertEquals(0.05 / 2, dividends.getRateOfReturnPerYear(), 0.000001);
    }

    /**
     * The rate of return based on the quote compares amounts in the same
     * currency, also if the dividend is booked in another currency than the
     * currency of the security.
     */
    @Test
    public void rateOfReturnBasedOnQuoteWithDividendInOtherCurrency()
    {
        Security usd = new Security("Test Security", "USD");
        usd.addPrice(new SecurityPrice(LocalDate.parse("2015-01-15"), Values.Quote.factorize(110)));

        List<CalculationLineItem> transactions = new ArrayList<>();
        transactions.add(CalculationLineItem.of(new Portfolio(),
                        new PortfolioTransaction(LocalDateTime.of(2015, 1, 2, 0, 0), "USD",
                                        Values.Amount.factorize(1000), usd, Values.Share.factorize(10), Type.BUY,
                                        0L, 0L)));
        transactions.add(CalculationLineItem.of(new Portfolio(),
                        new PortfolioTransaction(LocalDateTime.of(2015, 1, 9, 0, 0), "USD",
                                        Values.Amount.factorize(1100), usd, Values.Share.factorize(10), Type.SELL,
                                        0L, 0L)));

        // dividend of 40 EUR booked on an EUR account
        AccountTransaction dividend = new AccountTransaction();
        dividend.setType(AccountTransaction.Type.DIVIDENDS);
        dividend.setSecurity(usd);
        dividend.setDateTime(LocalDateTime.of(2015, 1, 15, 0, 0));
        dividend.setAmount(Values.Amount.factorize(40));
        dividend.setShares(Values.Share.factorize(10));
        dividend.setCurrencyCode(CurrencyUnit.EUR);
        transactions.add(CalculationLineItem.of(new Account(), dividend));

        Calculation.perform(CostCalculation.class, converter, usd, transactions);
        DividendCalculation dividends = Calculation.perform(DividendCalculation.class, converter, usd,
                        transactions);

        long marketValue = converter.convert(LocalDate.parse("2015-01-15"),
                        Money.of("USD", Values.Amount.factorize(1100))).getAmount();
        assertEquals(Values.Amount.factorize(40) / (double) marketValue, dividends.getRateOfReturnPerYear(),
                        0.000001);
    }

    /**
     * If the position was sold before the payment and the dividend is split
     * across two accounts (security held in two portfolios), the rate of return
     * must be the same as for a single payment.
     */
    @Test
    public void rateOfReturnBasedOnQuoteWithPaymentSplitAcrossAccounts()
    {
        security.addPrice(new SecurityPrice(LocalDate.parse("2020-02-03"), Values.Quote.factorize(100)));

        DividendCalculation single = Calculation.perform(DividendCalculation.class, converter, security,
                        createSoldPositionWithPayments(security, 10));
        DividendCalculation split = Calculation.perform(DividendCalculation.class, converter, security,
                        createSoldPositionWithPayments(security, 5, 5));

        // 62.50 / (10 * 100)
        assertEquals(0.0625, single.getRateOfReturnPerYear(), 0.000001);
        assertEquals(single.getRateOfReturnPerYear(), split.getRateOfReturnPerYear(), 0.000001);
    }

    private List<CalculationLineItem> createSoldPositionWithPayments(Security security, int... shares)
    {
        List<CalculationLineItem> transactions = new ArrayList<>();
        transactions.add(CalculationLineItem.of(new Portfolio(),
                        new PortfolioTransaction(LocalDateTime.of(2020, 1, 2, 0, 0), CurrencyUnit.EUR,
                                        Values.Amount.factorize(1000), security, Values.Share.factorize(10),
                                        Type.BUY, 0L, 0L)));
        transactions.add(CalculationLineItem.of(new Portfolio(),
                        new PortfolioTransaction(LocalDateTime.of(2020, 1, 10, 0, 0), CurrencyUnit.EUR,
                                        Values.Amount.factorize(1100), security, Values.Share.factorize(10),
                                        Type.SELL, 0L, 0L)));
        for (int s : shares)
        {
            AccountTransaction t = dividend(security, LocalDateTime.of(2020, 2, 3, 0, 0),
                            Values.Amount.factorize(6.25 * s));
            t.setShares(Values.Share.factorize(s));
            transactions.add(CalculationLineItem.of(new Account(), t));
        }

        Calculation.perform(CostCalculation.class, converter, security, transactions);
        return transactions;
    }

    private List<CalculationLineItem> createSaleBeforePayment(Security security)
    {
        List<CalculationLineItem> transactions = new ArrayList<>();
        transactions.add(CalculationLineItem.of(new Portfolio(),
                        new PortfolioTransaction(LocalDateTime.of(2019, 1, 10, 0, 0), CurrencyUnit.EUR,
                                        Values.Amount.factorize(1000), security, Values.Share.factorize(10),
                                        Type.BUY, 0L, 0L)));
        transactions.add(CalculationLineItem.of(account,
                        dividend(security, LocalDateTime.of(2019, 6, 3, 0, 0), Values.Amount.factorize(50))));
        transactions.add(CalculationLineItem.of(new Portfolio(),
                        new PortfolioTransaction(LocalDateTime.of(2020, 1, 10, 0, 0), CurrencyUnit.EUR,
                                        Values.Amount.factorize(1100), security, Values.Share.factorize(10),
                                        Type.SELL, 0L, 0L)));
        transactions.add(CalculationLineItem.of(account,
                        dividend(security, LocalDateTime.of(2020, 2, 3, 0, 0), Values.Amount.factorize(50))));
        return transactions;
    }

    private AccountTransaction dividend(Security security, LocalDateTime date, long amount)
    {
        AccountTransaction t = new AccountTransaction();
        t.setType(AccountTransaction.Type.DIVIDENDS);
        t.setSecurity(security);
        t.setDateTime(date);
        t.setAmount(amount);
        t.setShares(Values.Share.factorize(10));
        t.setCurrencyCode(security.getCurrencyCode());
        return t;
    }
}
