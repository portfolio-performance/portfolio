package name.abuchen.portfolio.datatransfer.csv;

import static name.abuchen.portfolio.datatransfer.csv.CSVExtractorTestUtil.buildField2Column;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class CSVSecurityPriceExtractorTest
{
    @Test
    public void testSecurityCreation() throws ParseException
    {
        CSVExtractor extractor = new CSVSecurityPriceExtractor();

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0,
                        Arrays.<String[]>asList( //
                                        new String[] { "2015-01-01", "14,20" }, new String[] { "2015-01-02", "15,20" }),
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findAny().get().getSecurity();

        List<SecurityPrice> prices = security.getPrices();
        assertThat(prices.size(), is(2));
        assertThat(prices.get(0), is(new SecurityPrice(LocalDate.parse("2015-01-01"), Values.Quote.factorize(14.20))));
        assertThat(prices.get(1), is(new SecurityPrice(LocalDate.parse("2015-01-02"), Values.Quote.factorize(15.20))));
    }

    @Test
    public void testErrorIfDateIsMissing() throws ParseException
    {
        CSVExtractor extractor = new CSVSecurityPriceExtractor();

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList( //
                        new String[] { "", "14,20" }), buildField2Column(extractor), errors);

        assertThat(errors.size(), is(1));
        assertThat(results, empty());
    }

    @Test
    public void testErrorIfAmountIsMissing() throws ParseException
    {
        CSVExtractor extractor = new CSVSecurityPriceExtractor();

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList( //
                        new String[] { "2015-01-01", "" }), buildField2Column(extractor), errors);

        assertThat(errors.size(), is(1));
        assertThat(results, empty());
    }
}
