package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;

import org.junit.Test;

@SuppressWarnings("nls")
public class AmountsTest
{
    @Test
    public void testConversionsToFixedPoint()
    {
        assertThat(Amounts.toAmount(new BigDecimal("12.34")), is(1234L));
        assertThat(Amounts.toAmount(new BigDecimal("0.005")), is(1L)); // half-up
        assertThat(Amounts.toShares(new BigDecimal("1.5")), is(150_000_000L));
        assertThat(Amounts.toQuote(new BigDecimal("101.2")), is(10_120_000_000L));
    }

    @Test
    public void testConversionsFromFixedPoint()
    {
        assertThat(Amounts.amount(1234L), is(new BigDecimal("12.34")));
        assertThat(Amounts.shares(150_000_000L), comparesEqualTo(new BigDecimal("1.5")));
        assertThat(Amounts.quote(10_120_000_000L), comparesEqualTo(new BigDecimal("101.2")));
    }

    @Test
    public void testGrossValueAndConversion()
    {
        assertThat(Amounts.grossValue(Amounts.toShares(new BigDecimal("12.5")), new BigDecimal("101.2")),
                        is(126500L));
        assertThat(Amounts.convert(10000L, new BigDecimal("1.0850")), is(10850L));
        assertThat(Amounts.convert(1L, new BigDecimal("0.5")), is(1L)); // 0.005 rounds half-up
    }

    @Test
    public void testInverseRate()
    {
        assertThat(Amounts.inverseRate(new BigDecimal("2")), comparesEqualTo(new BigDecimal("0.5")));
        assertThat(Amounts.inverseRate(new BigDecimal("3")), is(new BigDecimal("0.3333333333")));
        assertThat(Amounts.inverseRate(BigDecimal.ZERO), is(BigDecimal.ZERO));
    }

    @Test
    public void testGrossValueTolerance()
    {
        var shares = Amounts.toShares(new BigDecimal("10"));
        var quote = new BigDecimal("100");

        // 10 x (100 -/+ 0.01) = 999.90 ... 1000.10
        assertThat(Amounts.isGrossValueWithinQuoteTolerance(shares, quote, 100000L), is(true));
        assertThat(Amounts.isGrossValueWithinQuoteTolerance(shares, quote, 99990L), is(true));
        assertThat(Amounts.isGrossValueWithinQuoteTolerance(shares, quote, 100010L), is(true));
        assertThat(Amounts.isGrossValueWithinQuoteTolerance(shares, quote, 99989L), is(false));
        assertThat(Amounts.isGrossValueWithinQuoteTolerance(shares, quote, 100011L), is(false));
    }

    @Test
    public void testConvertedValueTolerance()
    {
        var rate = new BigDecimal("1.2");

        // 1000.00 x (1.2 -/+ 0.0001) = 1199.90 ... 1200.10
        assertThat(Amounts.isConvertedValueWithinRateTolerance(100000L, rate, 120000L), is(true));
        assertThat(Amounts.isConvertedValueWithinRateTolerance(100000L, rate, 119990L), is(true));
        assertThat(Amounts.isConvertedValueWithinRateTolerance(100000L, rate, 120010L), is(true));
        assertThat(Amounts.isConvertedValueWithinRateTolerance(100000L, rate, 119989L), is(false));
        assertThat(Amounts.isConvertedValueWithinRateTolerance(100000L, rate, 120011L), is(false));
    }

    @Test
    public void testConvertBack()
    {
        // 45.00 EUR at 0.9 EUR per USD is 50.00 USD
        assertThat(Amounts.convertBack(4500L, new BigDecimal("0.9")), is(5000L));
        // 100.00 / 3 = 33.333... → 33.33, and 0.02 / 3 → 0.01 (half-up)
        assertThat(Amounts.convertBack(10000L, new BigDecimal("3")), is(3333L));
        assertThat(Amounts.convertBack(2L, new BigDecimal("3")), is(1L));
    }
}
