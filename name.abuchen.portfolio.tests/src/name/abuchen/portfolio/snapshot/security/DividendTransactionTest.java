package name.abuchen.portfolio.snapshot.security;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class DividendTransactionTest
{
    Security security;

    @Before
    public void setup()
    {
        this.security = new Security("ADIDAS ORD", CurrencyUnit.EUR);
    }

    public DividendTransaction createDividendTransaction(long amount, long shares, long tax, LocalDateTime date)
    {
        DividendTransaction dt = new DividendTransaction();
        dt.setAmount(amount);
        dt.setSecurity(security);
        dt.setCurrencyCode(security.getCurrencyCode());
        dt.setShares(shares);
        dt.setDateTime(date);
        dt.setTotalShares(shares);
        if (tax > 0)
        {
            dt.addUnit(new Unit(Unit.Type.TAX, Money.of(CurrencyUnit.EUR, tax * -1)));
        }

        return dt;
    }

    @Test
    public void testCreateFromAccountTransaction()
    {
        AccountTransaction at = new AccountTransaction(
                        LocalDateTime.of(LocalDate.of(2020, 05, 20), LocalTime.of(12, 00)), CurrencyUnit.EUR, 1000L,
                        security, AccountTransaction.Type.DIVIDENDS);

        DividendTransaction dt = DividendTransaction.from(at);

        // some basic assertions that the creation was correct
        assertEquals(dt.getSecurity(), at.getSecurity());
        assertEquals(dt.getShares(), at.getShares());
        assertEquals(dt.getAmount(), at.getAmount());
        assertEquals(dt.getDateTime(), at.getDateTime());
        assertEquals(dt.getCurrencyCode(), at.getCurrencyCode());
        assertEquals(dt.getNote(), at.getNote());

    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateFromAccountTransactionIllegal()
    {
        AccountTransaction at = new AccountTransaction(
                        LocalDateTime.of(LocalDate.of(2020, 05, 20), LocalTime.of(12, 00)), CurrencyUnit.EUR, 1000L,
                        security, AccountTransaction.Type.BUY);

        @SuppressWarnings("unused")
        DividendTransaction dt = DividendTransaction.from(at); // NOSONAR

    }

    @Test
    public void testGetGrossValueNoTax()
    {
        DividendTransaction t = this.createDividendTransaction(100L, 10L, 0L, LocalDateTime.parse("2020-05-20T00:00"));

        Money result = t.getGrossValue();
        Money expected = Money.of(CurrencyUnit.EUR, 100L);

        assertEquals(result, expected);
    }

    @Test
    public void testGetGrossValueWithTax()
    {
        DividendTransaction t = this.createDividendTransaction(100L, 10L, 5L, LocalDateTime.parse("2020-05-20T00:00"));

        Money result = t.getGrossValue();
        Money expected = Money.of(CurrencyUnit.EUR, 95L);

        assertEquals(result, expected);
    }

    @Test
    public void testDividendPerShare()
    {
        DividendTransaction t = this.createDividendTransaction(100, 10L, 5L, LocalDateTime.parse("2020-05-20T00:00"));

        long result = t.getDividendPerShare();

        assertEquals(result, 9500000000l);
    }

    @Test
    public void testDividendPerShareNoShares()
    {
        DividendTransaction t = this.createDividendTransaction(0L, 0L, 0L, LocalDateTime.parse("2020-05-20T00:00"));

        long result = t.getDividendPerShare();

        assertEquals(result, 0L);
    }

    @Test
    public void testGetMovingAverageCost()
    {
        DividendTransaction t1 = createDividendTransaction(100L, 10L, 1L, LocalDateTime.of(2019, 01, 15, 12, 00));
        Money movingAverageCost = Money.of(CurrencyUnit.EUR, 1000L);
        t1.setMovingAverageCost(movingAverageCost);

        Money result = t1.getMovingAverageCost();
        Money expected = Money.of(CurrencyUnit.EUR, 1000L);

        assertEquals(result, expected);
    }

    @Test
    public void testGetFifoCosts()
    {
        DividendTransaction t1 = createDividendTransaction(100L, 10L, 0L, LocalDateTime.of(2019, 01, 15, 12, 00));
        Money fifoCost = Money.of(CurrencyUnit.EUR, 2000L);
        t1.setFifoCost(fifoCost);

        Money result = t1.getFifoCost();
        Money expected = Money.of(CurrencyUnit.EUR, 2000L);

        assertEquals(result, expected);
    }

    @Test
    public void testGetPersonalDiviendYield()
    {
        DividendTransaction t1 = createDividendTransaction(100L, 10L, 0L, LocalDateTime.of(2019, 01, 15, 12, 00));
        Money fifoCost = Money.of(CurrencyUnit.EUR, 2000L);
        t1.setFifoCost(fifoCost);

        //
        double result = t1.getPersonalDividendYield(); // hence 5% yield

        assertEquals(0.05d, result, 0.0d);
    }

    @Test
    public void testGetPersonalDiviendYieldNoSharesNoShares()
    {
        DividendTransaction t1 = createDividendTransaction(100L, 10L, 0L, LocalDateTime.of(2019, 01, 15, 12, 00));
        // Transaction has no movingAverageCosts

        double result = t1.getPersonalDividendYield();

        assertEquals(0.0, result, 0.001d);
    }

    @Test
    public void testGetPersonalDiviendYieldNoShares()
    {
        // although no shares, personal dividend should still be calculated
        // correctly

        DividendTransaction t1 = createDividendTransaction(100L, 0L, 0L, LocalDateTime.of(2019, 01, 15, 12, 00));
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
        DividendTransaction t1 = createDividendTransaction(100L, 10L, 0L, LocalDateTime.of(2019, 01, 15, 12, 00));
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
        DividendTransaction t1 = createDividendTransaction(100L, 10L, 0L, LocalDateTime.of(2019, 01, 15, 12, 00));
        // Transaction has no movingAverageCosts

        double result = t1.getPersonalDividendYieldMovingAverage();

        assertEquals(0.0, result, 0.001d);
    }

    @Test
    public void testGetPersonalDiviendYieldMovingAverageNoShares()
    {
        // although no shares, personal dividend should still be calculated
        // correctly

        DividendTransaction t1 = createDividendTransaction(100L, 0L, 0L, LocalDateTime.of(2019, 01, 15, 12, 00));
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
}
