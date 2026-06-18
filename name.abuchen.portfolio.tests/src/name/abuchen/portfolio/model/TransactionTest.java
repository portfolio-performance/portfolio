package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.Test;

import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

public class TransactionTest
{

    @Test
    public void testUnitValidityCheckWithSmallValues()
    {
        Transaction.Unit unit = new Transaction.Unit(Unit.Type.TAX,
                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.33)),
                        Money.of("JPY", Values.Amount.factorize(0.03)), //$NON-NLS-1$
                        BigDecimal.valueOf(131.53));

        assertThat(unit.getAmount().getAmount(), is(Values.Amount.factorize(4.33)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnitRoundingCheck()
    {
        new Transaction.Unit(Unit.Type.TAX,
                        Money.of(CurrencyUnit.EUR, Values.Amount.factorize(4.33)),
                        Money.of("JPY", Values.Amount.factorize(0.01)), //$NON-NLS-1$
                        BigDecimal.valueOf(131.53));
    }

    @Test
    public void testByDateComparatorThrowsOnNullDateForFirstTransaction()
    {
        var t1 = new AccountTransaction();
        t1.setCurrencyCode(CurrencyUnit.EUR);
        t1.setAmount(100);
        // t1 has no date set

        var t2 = new AccountTransaction();
        t2.setDateTime(LocalDateTime.of(2024, 1, 1, 0, 0));
        t2.setCurrencyCode(CurrencyUnit.EUR);
        t2.setAmount(200);

        var exception = assertThrows(NullPointerException.class, () -> Transaction.BY_DATE.compare(t1, t2));

        assertTrue(exception.getMessage().contains(AccountTransaction.class.getSimpleName()));
        assertTrue(exception.getMessage().contains(t1.getUUID()));
    }

    @Test
    public void testByDateComparatorThrowsOnNullDateForSecondTransaction()
    {
        var t1 = new AccountTransaction();
        t1.setDateTime(LocalDateTime.of(2024, 1, 1, 0, 0));
        t1.setCurrencyCode(CurrencyUnit.EUR);
        t1.setAmount(100);

        var t2 = new AccountTransaction();
        t2.setCurrencyCode(CurrencyUnit.EUR);
        t2.setAmount(200);
        // t2 has no date set

        var exception = assertThrows(NullPointerException.class, () -> Transaction.BY_DATE.compare(t1, t2));
        
        assertTrue(exception.getMessage().contains(AccountTransaction.class.getSimpleName()));
        assertTrue(exception.getMessage().contains(t2.getUUID()));
    }
}
