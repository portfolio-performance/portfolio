package name.abuchen.portfolio.datatransfer.csv;

import static name.abuchen.portfolio.datatransfer.csv.CSVExtractorTestUtil.buildField2Column;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
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
import name.abuchen.portfolio.datatransfer.Extractor.SecurityUpdateItem;
import name.abuchen.portfolio.datatransfer.SecurityUpdate;
import name.abuchen.portfolio.datatransfer.actions.AssertImportActions;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.AttributeType.ImageConverter;
import name.abuchen.portfolio.model.AttributeType.PercentConverter;
import name.abuchen.portfolio.model.AttributeType.StringConverter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.online.impl.AlphavantageQuoteFeed;
import name.abuchen.portfolio.online.impl.YahooFinanceQuoteFeed;

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
        assertThat(security.getFeed(), is(YahooFinanceQuoteFeed.ID));
    }

    @Test
    public void testSecurityIsNotCreatedIfItAlreadyExists()
    {
        Security security = new Security("SAP SE", CurrencyUnit.EUR);
        security.setIsin("DE0007164600");
        security.setWkn("716460");
        security.setTickerSymbol("SAP.DE");
        security.setNote("Notiz");
        security.setFeed(AlphavantageQuoteFeed.ID);

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
        assertThat(security.getFeed(), is(AlphavantageQuoteFeed.ID));
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
        assertNull(security.getFeed());
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
        assertNull(security.getFeed());
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
        assertThat(security.getFeed(), is(YahooFinanceQuoteFeed.ID));
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
        assertNull(security.getFeed());
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
        assertThat(security.getFeed(), is(YahooFinanceQuoteFeed.ID));
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

    private AttributeType addStringAttribute(Client client, String id, String name)
    {
        var type = new AttributeType(id);
        type.setName(name);
        type.setColumnLabel(name);
        type.setTarget(Security.class);
        type.setType(String.class);
        type.setConverter(StringConverter.class);
        client.getSettings().addAttributeType(type);
        return type;
    }

    @Test
    public void testNewSecurityCarriesAttributeAndNote() throws ParseException
    {
        var client = new Client();
        var rating = addStringAttribute(client, "rating", "Rating");
        var extractor = new CSVSecurityExtractor(client);

        var errors = new ArrayList<Exception>();
        // columns: isin, wkn, ticker, name, currency, note, date, quote, <logo?>, rating...
        // build a row long enough; only isin, name and the rating column are filled
        var row = new String[extractor.getFields().size()];
        java.util.Arrays.fill(row, "");
        var codes = extractor.getFields().stream().map(f -> f.getCode()).toList();
        row[codes.indexOf("isin")] = "DE0007164600";
        row[codes.indexOf("name")] = "SAP SE";
        row[codes.indexOf(SecurityAttributeColumns.fieldCode(rating))] = "A";

        var results = extractor.extract(0, java.util.Arrays.<String[]>asList(row), buildField2Column(extractor), errors);

        assertThat(errors, empty());
        // new security -> exactly one SecurityItem, no update item
        assertThat(results.stream().filter(SecurityUpdateItem.class::isInstance).count(), is(0L));
        var security = results.stream().filter(SecurityItem.class::isInstance).findFirst()
                        .orElseThrow().getSecurity();
        assertThat(security.getAttributes().get(rating), is("A"));
    }

    @Test
    public void testExistingSecurityEmitsUpdateForChangedAttributeOnly() throws ParseException
    {
        var security = new Security("SAP SE", CurrencyUnit.EUR);
        security.setIsin("DE0007164600");
        var client = new Client();
        client.addSecurity(security);
        var rating = addStringAttribute(client, "rating", "Rating");
        var extractor = new CSVSecurityExtractor(client);

        var errors = new ArrayList<Exception>();
        var codes = extractor.getFields().stream().map(f -> f.getCode()).toList();
        var row = new String[extractor.getFields().size()];
        java.util.Arrays.fill(row, "");
        row[codes.indexOf("isin")] = "DE0007164600";
        row[codes.indexOf(SecurityAttributeColumns.fieldCode(rating))] = "A";

        var results = extractor.extract(0, java.util.Arrays.<String[]>asList(row), buildField2Column(extractor), errors);

        assertThat(errors, empty());
        var updates = results.stream().filter(SecurityUpdateItem.class::isInstance)
                        .map(i -> (SecurityUpdateItem) i).toList();
        assertThat(updates.size(), is(1));
        assertThat(updates.get(0).getSecurity(), is(security));
        assertThat(updates.get(0).getUpdates().size(), is(1));
        // nothing applied yet
        assertThat(security.getAttributes().get(rating), is(nullValue()));
    }

    @Test
    public void testUnchangedValueProducesNoItem() throws ParseException
    {
        var security = new Security("SAP SE", CurrencyUnit.EUR);
        security.setIsin("DE0007164600");
        var client = new Client();
        client.addSecurity(security);
        var rating = addStringAttribute(client, "rating", "Rating");
        security.getAttributes().put(rating, "A");
        var extractor = new CSVSecurityExtractor(client);

        var errors = new ArrayList<Exception>();
        var codes = extractor.getFields().stream().map(f -> f.getCode()).toList();
        var row = new String[extractor.getFields().size()];
        java.util.Arrays.fill(row, "");
        row[codes.indexOf("isin")] = "DE0007164600";
        row[codes.indexOf(SecurityAttributeColumns.fieldCode(rating))] = "A"; // identical

        var results = extractor.extract(0, java.util.Arrays.<String[]>asList(row), buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results, empty()); // no update, no SecurityItem for an existing sec
    }

    @Test
    public void testConflictingRowsMarkItemAsFailure() throws ParseException
    {
        var security = new Security("SAP SE", CurrencyUnit.EUR);
        security.setIsin("DE0007164600");
        var client = new Client();
        client.addSecurity(security);
        var rating = addStringAttribute(client, "rating", "Rating");
        var extractor = new CSVSecurityExtractor(client);

        var errors = new ArrayList<Exception>();
        var codes = extractor.getFields().stream().map(f -> f.getCode()).toList();
        java.util.function.Function<String, String[]> mk = value -> {
            var r = new String[extractor.getFields().size()];
            java.util.Arrays.fill(r, "");
            r[codes.indexOf("isin")] = "DE0007164600";
            r[codes.indexOf(SecurityAttributeColumns.fieldCode(rating))] = value;
            return r;
        };

        var results = extractor.extract(0, java.util.Arrays.asList(mk.apply("A"), mk.apply("B")),
                        buildField2Column(extractor), errors);

        var item = results.stream().filter(SecurityUpdateItem.class::isInstance)
                        .map(i -> (SecurityUpdateItem) i).findFirst().orElseThrow();
        assertThat(item.isFailure(), is(true));
    }

    @Test
    public void testSameValueAcrossRowsDoesNotConflict() throws ParseException
    {
        var security = new Security("SAP SE", CurrencyUnit.EUR);
        security.setIsin("DE0007164600");
        var client = new Client();
        client.addSecurity(security);
        var rating = addStringAttribute(client, "rating", "Rating");
        var extractor = new CSVSecurityExtractor(client);

        var errors = new ArrayList<Exception>();
        var codes = extractor.getFields().stream().map(f -> f.getCode()).toList();
        java.util.function.Function<String, String[]> mk = value -> {
            var r = new String[extractor.getFields().size()];
            java.util.Arrays.fill(r, "");
            r[codes.indexOf("isin")] = "DE0007164600";
            r[codes.indexOf(SecurityAttributeColumns.fieldCode(rating))] = value;
            return r;
        };

        var results = extractor.extract(0, java.util.Arrays.asList(mk.apply("A"), mk.apply("A")),
                        buildField2Column(extractor), errors);

        assertThat(errors, empty());
        var updates = results.stream().filter(SecurityUpdateItem.class::isInstance)
                        .map(i -> (SecurityUpdateItem) i).toList();
        assertThat(updates.size(), is(1));
        assertThat(updates.get(0).isFailure(), is(false));
    }

    @Test
    public void testBlankCellOnExistingSecurityPreservesValue() throws ParseException
    {
        var security = new Security("SAP SE", CurrencyUnit.EUR);
        security.setIsin("DE0007164600");
        var client = new Client();
        client.addSecurity(security);
        var rating = addStringAttribute(client, "rating", "Rating");
        security.getAttributes().put(rating, "A");
        var extractor = new CSVSecurityExtractor(client);

        var errors = new ArrayList<Exception>();
        var codes = extractor.getFields().stream().map(f -> f.getCode()).toList();
        var row = new String[extractor.getFields().size()];
        java.util.Arrays.fill(row, "");
        row[codes.indexOf("isin")] = "DE0007164600";
        // rating cell left blank

        var results = extractor.extract(0, java.util.Arrays.<String[]>asList(row), buildField2Column(extractor), errors);

        assertThat(errors, empty());
        assertThat(results.stream().filter(SecurityUpdateItem.class::isInstance).count(), is(0L));
        assertThat(security.getAttributes().get(rating), is("A"));
    }

    @Test
    public void testImageAttributeIsNotOfferedAsColumn()
    {
        var client = new Client();
        var logo = new AttributeType("mylogo");
        logo.setName("MyLogo");
        logo.setColumnLabel("MyLogo");
        logo.setTarget(Security.class);
        logo.setType(String.class);
        logo.setConverter(ImageConverter.class);
        client.getSettings().addAttributeType(logo);

        var extractor = new CSVSecurityExtractor(client);
        var hasLogoColumn = extractor.getFields().stream().anyMatch(f -> f.getCode().equals(SecurityAttributeColumns.fieldCode(logo)));
        assertThat(hasLogoColumn, is(false));
    }

    @Test
    public void testAttributeIdCollidingWithBuiltinFieldCodeDoesNotClash() throws ParseException
    {
        // an attribute whose id equals a built-in field code ("note") must not
        // collide with the built-in note column: the namespaced field code
        // ("attribute:note") keeps them disjoint in the code-keyed map

        var security = new Security("SAP SE", CurrencyUnit.EUR);
        security.setIsin("DE0007164600");
        var client = new Client();
        client.addSecurity(security);
        var collidingAttribute = addStringAttribute(client, "note", "Rating");
        var extractor = new CSVSecurityExtractor(client);

        // the built-in note field and the attribute field have distinct codes
        var codes = extractor.getFields().stream().map(f -> f.getCode()).toList();
        assertThat(codes.contains("note"), is(true));
        assertThat(codes.contains(SecurityAttributeColumns.fieldCode(collidingAttribute)), is(true));

        var errors = new ArrayList<Exception>();
        var row = new String[extractor.getFields().size()];
        java.util.Arrays.fill(row, "");
        row[codes.indexOf("isin")] = "DE0007164600";
        row[codes.indexOf("note")] = "a note";
        row[codes.indexOf(SecurityAttributeColumns.fieldCode(collidingAttribute))] = "A";

        var results = extractor.extract(0, java.util.Arrays.<String[]>asList(row), buildField2Column(extractor),
                        errors);

        assertThat(errors, empty());
        // the note went to the note update, the attribute value to the attribute
        var update = results.stream().filter(SecurityUpdateItem.class::isInstance).map(i -> (SecurityUpdateItem) i)
                        .findFirst().orElseThrow();
        for (var u : update.getUpdates())
        {
            if (u instanceof SecurityUpdate.AttributeUpdate au)
                assertThat(au.value(), is("A"));
            else if (u instanceof SecurityUpdate.NoteUpdate nu)
                assertThat(nu.note(), is("a note"));
        }
        assertThat(update.getUpdates().size(), is(2));
    }

    @Test
    public void testBadValueRejectsWholeRowWithoutLeakingSecurity()
    {
        var client = new Client();
        var percent = new AttributeType("ter2");
        percent.setName("TER2");
        percent.setColumnLabel("TER2");
        percent.setTarget(Security.class);
        percent.setType(Double.class);
        percent.setConverter(PercentConverter.class);
        client.getSettings().addAttributeType(percent);
        var extractor = new CSVSecurityExtractor(client);

        var errors = new ArrayList<Exception>();
        var codes = extractor.getFields().stream().map(f -> f.getCode()).toList();
        var row = new String[extractor.getFields().size()];
        java.util.Arrays.fill(row, "");
        row[codes.indexOf("isin")] = "DE0007164600";
        row[codes.indexOf("name")] = "SAP SE";
        row[codes.indexOf(SecurityAttributeColumns.fieldCode(percent))] = "not-a-number";

        var results = extractor.extract(0, java.util.Arrays.<String[]>asList(row), buildField2Column(extractor), errors);

        assertThat(errors.size(), is(1)); // row rejected
        assertThat(results, empty());     // no SecurityItem leaked
    }
}
