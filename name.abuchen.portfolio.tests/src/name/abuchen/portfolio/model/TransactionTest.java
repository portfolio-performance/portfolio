package name.abuchen.portfolio.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;

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

}
