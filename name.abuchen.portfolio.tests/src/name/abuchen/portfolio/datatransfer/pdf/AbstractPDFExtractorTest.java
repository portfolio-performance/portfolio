package name.abuchen.portfolio.datatransfer.pdf;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import name.abuchen.portfolio.model.Client;

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
}
