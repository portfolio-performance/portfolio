package name.abuchen.portfolio.datatransfer.pdf;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Locale;

import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class AbstractPDFExtractorTest
{
    private static class TestExtractor extends AbstractPDFExtractor
    {
        TestExtractor(Client client)
        {
            super(client);
        }

        @Override
        public String getLabel()
        {
            return "Test";
        }

        public String currencyCode(String currency)
        {
            return asCurrencyCode(currency);
        }

        public long bondNominal(String value)
        {
            return asBondNominal(value);
        }

        public long bondNominal(String value, Locale locale)
        {
            return asBondNominal(value, locale);
        }
    }

    @Test
    public void testAsCurrencyCode()
    {
        var extractor = new TestExtractor(new Client());

        assertThat(extractor.currencyCode("EUR"), is("EUR"));
        assertThat(extractor.currencyCode("USD"), is("USD"));
        assertThat(extractor.currencyCode("GBP"), is("GBP"));
        assertThat(extractor.currencyCode("CHF"), is("CHF"));
    }

    @Test
    public void testAsCurrencyCodeTrimsNonBreakingSpace()
    {
        var extractor = new TestExtractor(new Client());

        // U+00A0 non-breaking space: trimmed by TextUtil.trim() but not by String.trim()
        var nbsp = String.valueOf((char) 0x00A0);
        assertThat(extractor.currencyCode("USD" + nbsp), is("USD"));
        assertThat(extractor.currencyCode(nbsp + "EUR"), is("EUR"));
        assertThat(extractor.currencyCode(nbsp + "CHF" + nbsp), is("CHF"));
    }

    @Test
    public void testAsCurrencyCodeTrimsRegularWhitespace()
    {
        var extractor = new TestExtractor(new Client());

        assertThat(extractor.currencyCode(" EUR "), is("EUR"));
        assertThat(extractor.currencyCode("\tUSD\t"), is("USD"));
    }

    @Test
    public void testAsCurrencyCodeHandlesNull()
    {
        var client = new Client();
        var extractor = new TestExtractor(client);

        assertThat(extractor.currencyCode(null), is(client.getBaseCurrency()));
    }

    @Test
    public void testAsCurrencyCodeFallsBackToBaseCurrency()
    {
        var client = new Client();
        var extractor = new TestExtractor(client);

        assertThat(extractor.currencyCode("UNKNOWN"), is(client.getBaseCurrency()));
        assertThat(extractor.currencyCode("XYZ"), is(client.getBaseCurrency()));
    }

    @Test
    public void testAsBondNominal()
    {
        var extractor = new TestExtractor(new Client());

        // the nominal is quoted in percent, i.e. it is divided by 100
        assertThat(extractor.bondNominal("400.000,00000"), is(Values.Share.factorize(4000)));
        assertThat(extractor.bondNominal("1.000.000,00000"), is(Values.Share.factorize(10000)));
        assertThat(extractor.bondNominal("2.100.000"), is(Values.Share.factorize(21000)));
        assertThat(extractor.bondNominal("150"), is(Values.Share.factorize(1.5)));
        assertThat(extractor.bondNominal("50,000"), is(Values.Share.factorize(0.5)));
    }

    @Test
    public void testAsBondNominalWithLocale()
    {
        var extractor = new TestExtractor(new Client());

        assertThat(extractor.bondNominal("400.000,00000", Locale.GERMANY), is(Values.Share.factorize(4000)));
        assertThat(extractor.bondNominal("400,000.00000", Locale.US), is(Values.Share.factorize(4000)));
        assertThat(extractor.bondNominal("50.000", Locale.US), is(Values.Share.factorize(0.5)));
    }

    @Test
    public void testAsBondNominalRoundsHalfUp()
    {
        var extractor = new TestExtractor(new Client());

        // shares have 8 decimal places, i.e. a nominal with 7 decimal places
        // is exactly on the rounding boundary
        assertThat(extractor.bondNominal("0,0000004"), is(0L));
        assertThat(extractor.bondNominal("0,0000005"), is(1L));
        assertThat(extractor.bondNominal("0,0000015"), is(2L));
    }
}
