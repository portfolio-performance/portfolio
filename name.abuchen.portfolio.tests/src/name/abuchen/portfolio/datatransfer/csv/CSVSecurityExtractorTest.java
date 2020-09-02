package name.abuchen.portfolio.datatransfer.csv;

import static name.abuchen.portfolio.datatransfer.csv.CSVExtractorTestUtil.buildField2Column;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class CSVSecurityExtractorTest
{
    @Test
    public void testSecurityCreation() throws ParseException
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVSecurityExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0,
                        Arrays.<String[]>asList(
                                        new String[] { "DE0007164600", "716460", "SAP.DE", "SAP SE", "EUR", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Security security = results.stream().filter(i -> i instanceof SecurityItem).findAny().get().getSecurity();
        assertThat(security.getName(), is("SAP SE"));
        assertThat(security.getIsin(), is("DE0007164600"));
        assertThat(security.getWkn(), is("716460"));
        assertThat(security.getTickerSymbol(), is("SAP.DE"));
        assertThat(security.getCurrencyCode(), is("EUR"));
        assertThat(security.getNote(), is("Notiz"));
    }

    @Test
    public void testSecurityIsNotCreatedIfItAlreadyExists() throws ParseException
    {
        Client client = new Client();
        Security security = new Security();
        security.setIsin("DE0007164600");
        client.addSecurity(security);

        CSVExtractor extractor = new CSVSecurityExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0,
                        Arrays.<String[]>asList(
                                        new String[] { "DE0007164600", "716460", "SAP.DE", "SAP SE", "EUR", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results, empty());
    }

    @Test
    public void testSecurityIsCreatedOnlyOnce() throws ParseException
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVSecurityExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0,
                        Arrays.<String[]>asList(new String[] { "DE0007164600", "716460", "", "SAP SE", "EUR", "Notiz" },
                                        new String[] { "DE0007164600", "716460", "SAP.DE", "SAP SE", "EUR", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(results.size(), is(1));
        assertThat(errors.size(), is(0)); // no warning a/b duplicate imports
    }

    @Test
    public void testErrorMessage() throws ParseException
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVSecurityExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0,
                        Arrays.<String[]>asList(new String[] { "", "", "", "", "EUR", "Notiz" }),
                        buildField2Column(extractor), errors);

        assertThat(errors.size(), is(1));
        assertThat(results, empty());
    }
}
