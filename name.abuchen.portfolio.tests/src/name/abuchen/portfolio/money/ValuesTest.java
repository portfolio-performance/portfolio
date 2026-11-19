package name.abuchen.portfolio.money;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class ValuesTest
{
    private static final long PRICE = Values.Quote.factorize(101.5);

    private static Security bond()
    {
        var bond = new Security("Bond", CurrencyUnit.USD);
        bond.setPercentageQuoted(true);
        return bond;
    }

    private static Security share()
    {
        return new Security("Share", CurrencyUnit.USD);
    }

    @Test
    public void testQuoteFormatForPercentageQuotedSecurity()
    {
        var percent = Values.Quote.format(PRICE) + "%";
        var quote = Quote.of(CurrencyUnit.USD, PRICE);

        // the currency is replaced by the percent sign in every variant
        assertThat(Values.Quote.formatFor(bond(), PRICE), is(percent));
        assertThat(Values.Quote.formatFor(bond(), CurrencyUnit.USD, PRICE), is(percent));
        assertThat(Values.Quote.formatFor(bond(), CurrencyUnit.USD, PRICE, CurrencyUnit.EUR), is(percent));
        assertThat(Values.Quote.formatFor(bond(), quote), is(percent));
        assertThat(Values.Quote.formatFor(bond(), quote, CurrencyUnit.EUR), is(percent));
    }

    @Test
    public void testQuoteFormatForRegularSecurity()
    {
        var quote = Quote.of(CurrencyUnit.USD, PRICE);

        // regular securities are formatted exactly as before
        assertThat(Values.Quote.formatFor(share(), PRICE), is(Values.Quote.format(PRICE)));
        assertThat(Values.Quote.formatFor(share(), CurrencyUnit.USD, PRICE),
                        is(Values.Quote.format(CurrencyUnit.USD, PRICE)));
        assertThat(Values.Quote.formatFor(share(), CurrencyUnit.USD, PRICE, CurrencyUnit.EUR),
                        is(Values.Quote.format(CurrencyUnit.USD, PRICE, CurrencyUnit.EUR)));
        assertThat(Values.Quote.formatFor(share(), quote), is(Values.Quote.format(quote)));
        assertThat(Values.Quote.formatFor(share(), quote, CurrencyUnit.EUR),
                        is(Values.Quote.format(quote, CurrencyUnit.EUR)));

        // without security (e.g. no instrument at hand)
        assertThat(Values.Quote.formatFor(null, quote, CurrencyUnit.EUR),
                        is(Values.Quote.format(quote, CurrencyUnit.EUR)));
    }

    @Test
    public void testCalculatedQuoteFormatFor()
    {
        var quote = Quote.of(CurrencyUnit.USD, PRICE);

        assertThat(Values.CalculatedQuote.formatFor(bond(), quote, CurrencyUnit.EUR),
                        is(Values.CalculatedQuote.format(PRICE) + "%"));
        assertThat(Values.CalculatedQuote.formatFor(share(), quote, CurrencyUnit.EUR),
                        is(Values.CalculatedQuote.format(quote, CurrencyUnit.EUR)));
    }

    @Test
    public void testPercentOnlyInCurrencyOfSecurity()
    {
        // a price converted into another currency is not a percentage of the
        // nominal value anymore: it keeps its currency
        var converted = Quote.of(CurrencyUnit.EUR, Values.Quote.factorize(90.9));

        assertThat(Values.Quote.formatFor(bond(), converted, CurrencyUnit.EUR),
                        is(Values.Quote.format(converted, CurrencyUnit.EUR)));
        assertThat(Values.Quote.formatFor(bond(), converted), is(Values.Quote.format(converted)));
        assertThat(Values.Quote.formatFor(bond(), CurrencyUnit.EUR, converted.getAmount(), CurrencyUnit.EUR),
                        is(Values.Quote.format(CurrencyUnit.EUR, converted.getAmount(), CurrencyUnit.EUR)));
        assertThat(Values.CalculatedQuote.formatFor(bond(), converted, CurrencyUnit.EUR),
                        is(Values.CalculatedQuote.format(converted, CurrencyUnit.EUR)));
    }
}
