package name.abuchen.portfolio.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.Messages;

@SuppressWarnings("nls")
public class PartialExemptionTest
{
    private Locale defaultLocale;

    @Before
    public void setupLocale()
    {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);
    }

    @After
    public void resetLocale()
    {
        Locale.setDefault(defaultLocale);
    }

    @Test
    public void testStatutoryRates()
    {
        assertThat(PartialExemption.of(0.0).orElseThrow(), is(PartialExemption.NONE));
        assertThat(PartialExemption.of(0.15).orElseThrow(), is(PartialExemption.MIXED_FUND));
        assertThat(PartialExemption.of(0.3).orElseThrow(), is(PartialExemption.EQUITY_FUND));
        assertThat(PartialExemption.of(0.6).orElseThrow(), is(PartialExemption.REAL_ESTATE_FUND));
        assertThat(PartialExemption.of(0.8).orElseThrow(), is(PartialExemption.FOREIGN_REAL_ESTATE_FUND));

        assertThat(PartialExemption.of(0.25).isEmpty(), is(true));
        assertThat(PartialExemption.of(null).isEmpty(), is(true));
    }

    @Test
    public void testLabelsContainPercentageAndFundTypes()
    {
        assertThat(PartialExemption.NONE.getLabel(), is("0,00% - " + Messages.PartialExemptionNone));
        assertThat(PartialExemption.MIXED_FUND.getLabel(), is("15,00% - " + Messages.PartialExemptionMixedFund));
        assertThat(PartialExemption.EQUITY_FUND.getLabel(), is("30,00% - " + Messages.PartialExemptionEquityFund));
        assertThat(PartialExemption.getLabels().length, is(PartialExemption.values().length));
    }

    @Test
    public void testSelectionIndexAndValue()
    {
        assertThat(PartialExemption.indexOf(0.8), is(PartialExemption.FOREIGN_REAL_ESTATE_FUND.ordinal()));
        assertThat(PartialExemption.valueAt(PartialExemption.indexOf(0.15)),
                        is(Double.valueOf(PartialExemption.MIXED_FUND.getValue())));
    }

    @Test
    public void testValueOutsideTheStatutoryRatesHasNoSelection()
    {
        assertThat(PartialExemption.indexOf(0.25), is(-1));
        assertThat(PartialExemption.indexOf(null), is(-1));
        assertThat(PartialExemption.valueAt(-1), is(nullValue()));
        assertThat(PartialExemption.valueAt(PartialExemption.values().length), is(nullValue()));
    }
}
