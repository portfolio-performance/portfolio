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
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class DividendTransactionTest
{
    Account account;
    Security security;

    @Before
    public void setup()
    {
        this.account = new Account();
        this.security = new Security("ADIDAS ORD", CurrencyUnit.EUR);
    }

    public CalculationLineItem.DividendPayment createDividendTransaction(long amount, long shares, long tax,
                    LocalDateTime date)
    {
        AccountTransaction t = new AccountTransaction();
        t.setType(AccountTransaction.Type.DIVIDENDS);
        t.setSecurity(security);
        t.setDateTime(date);
        t.setAmount(amount);
        t.setShares(shares);
        t.setCurrencyCode(security.getCurrencyCode());

        if (tax > 0)
            t.addUnit(new Unit(Unit.Type.TAX, Money.of(CurrencyUnit.EUR, tax * -1)));

        CalculationLineItem.DividendPayment answer = (CalculationLineItem.DividendPayment) CalculationLineItem
                        .of(account, t);
        answer.setTotalShares(shares);
        return answer;
    }

    @Test
    public void testGetGrossValueNoTax()
    {
        CalculationLineItem.DividendPayment t = this.createDividendTransaction(100L, 10L, 0L,
                        LocalDateTime.parse("2020-05-20T00:00"));

        Money result = t.getGrossValue();
        Money expected = Money.of(CurrencyUnit.EUR, 100L);

        assertEquals(expected, result);
    }

    @Test
    public void testGetGrossValueWithTax()
    {
        CalculationLineItem.DividendPayment t = this.createDividendTransaction(100L, 10L, 5L,
                        LocalDateTime.parse("2020-05-20T00:00"));

        Money result = t.getGrossValue();
        Money expected = Money.of(CurrencyUnit.EUR, 95L);

        assertEquals(expected, result);
    }

    @Test
    public void testDividendPerShare()
    {
        CalculationLineItem.DividendPayment t = this.createDividendTransaction(100, 10L, 5L,
                        LocalDateTime.parse("2020-05-20T00:00"));

        long result = t.getDividendPerShare();

        assertEquals(Values.Share.factorize(9500), result);
    }

    @Test
    public void testDividendPerShareNoShares()
    {
        CalculationLineItem.DividendPayment t = this.createDividendTransaction(0L, 0L, 0L,
                        LocalDateTime.parse("2020-05-20T00:00"));

        long result = t.getDividendPerShare();

        assertEquals(0L, result);
    }

    @Test
    public void testGetMovingAverageCost()
    {
        CalculationLineItem.DividendPayment t1 = createDividendTransaction(100L, 10L, 1L,
                        LocalDateTime.of(2019, 01, 15, 12, 00));
        Money movingAverageCost = Money.of(CurrencyUnit.EUR, 1000L);
        t1.setMovingAverageCost(movingAverageCost);

        Money result = t1.getMovingAverageCost();
        Money expected = Money.of(CurrencyUnit.EUR, 1000L);

        assertEquals(expected, result);
    }

    @Test
    public void testGetFifoCosts()
    {
        CalculationLineItem.DividendPayment t1 = createDividendTransaction(100L, 10L, 0L,
                        LocalDateTime.of(2019, 01, 15, 12, 00));
        Money fifoCost = Money.of(CurrencyUnit.EUR, 2000L);
        t1.setFifoCost(fifoCost);

        Money result = t1.getFifoCost();
        Money expected = Money.of(CurrencyUnit.EUR, 2000L);

        assertEquals(expected, result);
    }

    @Test
    public void testGetPersonalDiviendYield()
    {
        CalculationLineItem.DividendPayment t1 = createDividendTransaction(100L, 10L, 0L,
                        LocalDateTime.of(2019, 01, 15, 12, 00));
        Money fifoCost = Money.of(CurrencyUnit.EUR, 2000L);
        t1.setFifoCost(fifoCost);

        //
        double result = t1.getPersonalDividendYield(); // hence 5% yield

        assertEquals(0.05d, result, 0.0d);
    }

    @Test
    public void testGetPersonalDiviendYieldNoSharesNoShares()
    {
        CalculationLineItem.DividendPayment t1 = createDividendTransaction(100L, 10L, 0L,
                        LocalDateTime.of(2019, 01, 15, 12, 00));
        // Transaction has no movingAverageCosts

        double result = t1.getPersonalDividendYield();

        assertEquals(0.0, result, 0.001d);
    }

    @Test
    public void testGetPersonalDiviendYieldNoShares()
    {
        // although no shares, personal dividend should still be calculated
        // correctly

        CalculationLineItem.DividendPayment t1 = createDividendTransaction(100L, 0L, 0L,
                        LocalDateTime.of(2019, 01, 15, 12, 00));
        Money fifoCost = Money.of(CurrencyUnit.EUR, 2000L); // we paid
                                                            // originally 2000$
        t1.setFifoCost(fifoCost);

        //
        double result = t1.getPersonalDividendYield(); // hence 5% yield

        assertEquals(0.05d, result, 0.0d);
    }

    @Test
    public void testGetPersonalDividendYieldMovingAverage()
    {
        CalculationLineItem.DividendPayment t1 = createDividendTransaction(100L, 10L, 0L,
                        LocalDateTime.of(2019, 01, 15, 12, 00));
        Money movingAverageCost = Money.of(CurrencyUnit.EUR, 1800); // we paid
                                                                    // originally
                                                                    // around
                                                                    // 1800
        t1.setMovingAverageCost(movingAverageCost);

        double result = t1.getPersonalDividendYieldMovingAverage(); // hence
                                                                    // 5.5556%
                                                                    // yield

        assertEquals(0.0556d, result, 0.001d);
    }

    @Test
    public void testGetPersonalDividendYieldMovingAverageNoCosts()
    {
        CalculationLineItem.DividendPayment t1 = createDividendTransaction(100L, 10L, 0L,
                        LocalDateTime.of(2019, 01, 15, 12, 00));
        // Transaction has no movingAverageCosts

        double result = t1.getPersonalDividendYieldMovingAverage();

        assertEquals(0.0, result, 0.001d);
    }

    @Test
    public void testGetPersonalDiviendYieldMovingAverageNoShares()
    {
        // although no shares, personal dividend should still be calculated
        // correctly

        CalculationLineItem.DividendPayment t1 = createDividendTransaction(100L, 0L, 0L,
                        LocalDateTime.of(2019, 01, 15, 12, 00));
        Money movingAverageCost = Money.of(CurrencyUnit.EUR, 1500); // we paid
                                                                    // originally
                                                                    // around
                                                                    // 1800
        t1.setMovingAverageCost(movingAverageCost);

        double result = t1.getPersonalDividendYieldMovingAverage(); // hence
                                                                    // 6.66666%
                                                                    // yield

        assertEquals(0.06666, result, 0.001d);
    }

    /**
     * The costs are calculated in term currency. The personal dividend yield
     * must compare the dividend in the same currency, not the amount in the
     * currency of the transaction.
     */
    @Test
    public void testPersonalDividendYieldWithCurrencyOfTransactionOtherThanTermCurrency()
    {
        var converter = new TestCurrencyConverter();
        var usd = new Security("Test Security", "USD");

        List<CalculationLineItem> transactions = new ArrayList<>();
        transactions.add(CalculationLineItem.of(new Portfolio(),
                        new PortfolioTransaction(LocalDateTime.of(2015, 1, 14, 12, 0), "USD",
                                        Values.Amount.factorize(1000), usd, Values.Share.factorize(10),
                                        PortfolioTransaction.Type.BUY, 0L, 0L)));

        AccountTransaction t = new AccountTransaction();
        t.setType(AccountTransaction.Type.DIVIDENDS);
        t.setSecurity(usd);
        t.setDateTime(LocalDateTime.of(2015, 1, 15, 12, 0));
        t.setAmount(Values.Amount.factorize(50));
        t.setShares(Values.Share.factorize(10));
        t.setCurrencyCode("USD");
        var dividend = (CalculationLineItem.DividendPayment) CalculationLineItem.of(account, t);
        transactions.add(dividend);

        Calculation.perform(CostCalculation.class, converter, usd, transactions);

        long dividendInEUR = converter.convert(LocalDate.parse("2015-01-15"),
                        Money.of("USD", Values.Amount.factorize(50))).getAmount();
        long costInEUR = converter.convert(LocalDate.parse("2015-01-14"),
                        Money.of("USD", Values.Amount.factorize(1000))).getAmount();

        assertEquals(dividendInEUR / (double) costInEUR, dividend.getPersonalDividendYield(), 0.000001);
        assertEquals(dividendInEUR / (double) costInEUR, dividend.getPersonalDividendYieldMovingAverage(),
                        0.000001);
    }
}
