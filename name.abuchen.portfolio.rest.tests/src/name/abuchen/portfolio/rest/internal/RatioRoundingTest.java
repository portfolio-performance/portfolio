package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.google.gson.JsonNull;

import org.junit.Test;

/**
 * A rate is credible to about five decimals - IRR is solved by an iteration
 * that stops at 1e-5 - so the raw double claims precision the computation does
 * not have, and an iteration halting near zero leaves residue a client can
 * faithfully report as a return.
 */
@SuppressWarnings("nls")
public class RatioRoundingTest
{
    private static String wire(double value)
    {
        return EntityJson.ratio(value).toString();
    }

    /** the reported defect: an IRR of exactly zero, served as float dust */
    @Test
    public void testIterationResidueBecomesZero()
    {
        assertThat(wire(-0.000000000002438937940496544), is("0"));
        assertThat(wire(2.438937940496544e-12), is("0"));
    }

    /** and it is plain zero, not "-0" or "0E-8" */
    @Test
    public void testZeroIsWrittenPlainly()
    {
        assertThat(wire(0d), is("0"));
        assertThat(wire(-0d), is("0"));
    }

    @Test
    public void testOrdinaryRatesKeepEightDecimals()
    {
        assertThat(wire(-0.0252575376736347), is("-0.02525754"));
        assertThat(wire(0.0723), is("0.0723"));
        assertThat(wire(-0.48056933449413863), is("-0.48056933"));
    }

    /** a rate can exceed 1 - an annualised return over a short period, say */
    @Test
    public void testLargeRatesSurvive()
    {
        assertThat(wire(2.376028023198908), is("2.37602802"));
    }

    /** a rate this small is noise; the threshold is deliberate, not incidental */
    @Test
    public void testRatesBelowTheScaleCollapse()
    {
        assertThat(wire(1e-9), is("0"));
        assertThat(wire(0.00000001), is("0.00000001"));
    }

    /** unchanged behaviour: undefined stays null rather than becoming zero */
    @Test
    public void testUndefinedStaysNull()
    {
        assertThat(EntityJson.ratio(Double.NaN), is(JsonNull.INSTANCE));
        assertThat(EntityJson.ratio(Double.POSITIVE_INFINITY), is(JsonNull.INSTANCE));
        assertThat(EntityJson.ratio(Double.NEGATIVE_INFINITY), is(JsonNull.INSTANCE));
    }
}
