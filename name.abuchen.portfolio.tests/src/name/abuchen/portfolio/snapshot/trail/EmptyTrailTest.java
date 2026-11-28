package name.abuchen.portfolio.snapshot.trail;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class EmptyTrailTest
{
    private TrailRecord feeTrail(long amount)
    {
        return TrailRecord.ofTransaction(new AccountTransaction(LocalDate.parse("2020-07-01").atStartOfDay(),
                        CurrencyUnit.EUR, Values.Amount.factorize(amount), null, AccountTransaction.Type.FEES));
    }

    @Test
    public void testSubtractFromEmptyTrailRendersZeroMinusTrail()
    {
        TrailRecord fee = feeTrail(10);

        TrailRecord answer = TrailRecord.empty().subtract(fee);

        assertThat(answer.isEmpty(), is(false));
        assertThat(answer.getValue(), is(Money.of(CurrencyUnit.EUR, -Values.Amount.factorize(10))));

        // 0 - fee: the first input is a zero in the currency of the subtrahend
        assertThat(answer.getInputs().size(), is(2));
        assertThat(answer.getInputs().get(0).getValue(), is(Money.of(CurrencyUnit.EUR, 0)));
        assertThat(answer.getInputs().get(1), is(sameInstance(fee)));
    }

    @Test
    public void testSubtractEmptyFromEmptyTrailStaysEmpty()
    {
        TrailRecord answer = TrailRecord.empty().subtract(TrailRecord.empty());

        assertThat(answer.isEmpty(), is(true));
        assertThat(answer.getValue(), is(nullValue()));
    }

    @Test
    public void testSubtractEmptyTrailIsNoOp()
    {
        TrailRecord fee = feeTrail(10);

        assertThat(fee.subtract(TrailRecord.empty()), is(sameInstance(fee)));
        assertThat(fee.subtract(TrailRecord.empty()), is(not(TrailRecord.empty())));
    }
}
