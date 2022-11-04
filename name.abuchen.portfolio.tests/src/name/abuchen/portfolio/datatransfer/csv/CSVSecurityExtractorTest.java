package name.abuchen.portfolio.datatransfer.csv;

import static name.abuchen.portfolio.datatransfer.csv.CSVExtractorTestUtil.buildField2Column;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertNull;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
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
    public void testSecurityCreationWithAllSecurityData() throws ParseException
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVSecurityExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList( //
                        new String[] { //
                                        "DE0007164600", // ISIN
                                        "716460", // WKN
                                        "SAP.DE", // TickerSymbol
                                        "SAP SE", // Security name
                                        "EUR", // Currency
                                        "Notiz" // Note
                        }), buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();

        assertThat(security.getIsin(), is("DE0007164600"));
        assertThat(security.getWkn(), is("716460"));
        assertThat(security.getTickerSymbol(), is("SAP.DE"));
        assertThat(security.getName(), is("SAP SE"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(security.getNote(), is("Notiz"));
    }

    @Test
    public void testSecurityIsNotCreatedIfItAlreadyExists()
    {
        Security security = new Security("SAP SE", CurrencyUnit.EUR);
        security.setIsin("DE0007164600");
        security.setWkn("716460");
        security.setTickerSymbol("SAP.DE");
        security.setNote("Notiz");

        Client client = new Client();
        client.addSecurity(security);

        CSVExtractor extractor = new CSVSecurityExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList( //
                        new String[] { //
                                        "DE0007164600", // ISIN
                                        "716460", // WKN
                                        "SAP.DE", // TickerSymbol
                                        "SAP SE", // Security name
                                        "EUR", // Currency
                                        "Notiz" // Note
                        }), buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results, empty());
    }

    @Test
    public void testSecurityCreationOnlyWithISIN()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVSecurityExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList( //
                        new String[] { //
                                        "DE0007164600", // ISIN
                                        "", // WKN
                                        "", // TickerSymbol
                                        "", // Security name
                                        "", // Currency
                                        "" // Note
                        }), buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();

        assertThat(security.getIsin(), is("DE0007164600"));
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is(MessageFormat.format(Messages.CSVImportedSecurityLabel, "DE0007164600")));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertNull(security.getNote());
    }

    @Test
    public void testSecurityCreationOnlyWithWKN()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVSecurityExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList( //
                        new String[] { //
                                        "", // ISIN
                                        "716460", // WKN
                                        "", // TickerSymbol
                                        "", // Security name
                                        "", // Currency
                                        "" // Note
                        }), buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();

        assertNull(security.getIsin());
        assertThat(security.getWkn(), is("716460"));
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is(MessageFormat.format(Messages.CSVImportedSecurityLabel, "716460")));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertNull(security.getNote());
    }

    @Test
    public void testSecurityCreationOnlyWithTickerSymbol()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVSecurityExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList( //
                        new String[] { //
                                        "", // ISIN
                                        "", // WKN
                                        "SAP.DE", // TickerSymbol
                                        "", // Security name
                                        "", // Currency
                                        "" // Note
                        }), buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();

        assertNull(security.getIsin());
        assertNull(security.getWkn());
        assertThat(security.getTickerSymbol(), is("SAP.DE"));
        assertThat(security.getName(), is(MessageFormat.format(Messages.CSVImportedSecurityLabel, "SAP.DE")));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertNull(security.getNote());
    }

    @Test
    public void testSecurityCreationOnlyWithSecurityName()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVSecurityExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList( //
                        new String[] { //
                                        "", // ISIN
                                        "", // WKN
                                        "", // TickerSymbol
                                        "SAP SE", // Security name
                                        "", // Currency
                                        "" // Note
                        }), buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.size(), is(1));
        new AssertImportActions().check(results, CurrencyUnit.EUR);

        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();

        assertNull(security.getIsin());
        assertNull(security.getWkn());
        assertNull(security.getTickerSymbol());
        assertThat(security.getName(), is("SAP SE"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertNull(security.getNote());
    }

    @Test
    public void testSecurityIsCreatedOnlyOnce()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVSecurityExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList( //
                        new String[] { //
                                        "DE0007164600", // ISIN
                                        "716460", // WKN
                                        "SAP.DE", // TickerSymbol
                                        "SAP SE", // Security name
                                        "EUR", // Currency
                                        "Notiz" // Note
                        }, //
                        new String[] { //
                                        "DE0007164600", // ISIN
                                        "716460", // WKN
                                        "SAP.DE", // TickerSymbol
                                        "SAP SE", // Security name
                                        "EUR", // Currency
                                        "Notiz" // Note
                        }), buildField2Column(extractor), errors);

        assertThat(results.size(), is(1));
        assertThat(errors.size(), is(0)); // no warning a/b duplicate imports

        Security security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow(IllegalArgumentException::new).getSecurity();

        assertThat(security.getIsin(), is("DE0007164600"));
        assertThat(security.getWkn(), is("716460"));
        assertThat(security.getTickerSymbol(), is("SAP.DE"));
        assertThat(security.getName(), is("SAP SE"));
        assertThat(security.getCurrencyCode(), is(CurrencyUnit.EUR));
        assertThat(security.getNote(), is("Notiz"));
    }

    @Test
    public void testSecurityCreationWithoutEnoughSecurityData()
    {
        Client client = new Client();

        CSVExtractor extractor = new CSVSecurityExtractor(client);

        List<Exception> errors = new ArrayList<Exception>();
        List<Item> results = extractor.extract(0, Arrays.<String[]>asList( //
                        new String[] { //
                                        "", // ISIN
                                        "", // WKN
                                        "", // TickerSymbol
                                        "", // Security name
                                        "USD", // Currency
                                        "Notiz" // Note
                        }), buildField2Column(extractor), errors);

        assertThat(results, empty());
        assertThat(errors.size(), is(1));

        assertThat(errors.get(0).getMessage(), is(MessageFormat.format(Messages.CSVLineXwithMsgY, "1", //
                        MessageFormat.format(Messages.CSVImportMissingSecurity, //
                                        new StringJoiner(", ") //
                                                        .add(Messages.CSVColumn_ISIN) // ISIN
                                                        .add(Messages.CSVColumn_TickerSymbol) // TickerSymbol
                                                        .add(Messages.CSVColumn_WKN) // WKN
                                                        .toString()), //
                        new ArrayList<>(Arrays.asList( //
                                        "", // ISIN
                                        "", // WKN
                                        "", // TickerSymbol
                                        "", // Security name
                                        "USD", // Currency
                                        "Notiz" // Note
                        )))));
    }
}
