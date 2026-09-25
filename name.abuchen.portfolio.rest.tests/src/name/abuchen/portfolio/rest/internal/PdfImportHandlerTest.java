package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.TransactionFixture.assertError;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.body;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.Messages;

/**
 * The extraction step of a PDF import. The test fragment has no real PDF
 * documents: the files are empty placeholders and {@link PdfTextExtraction}
 * supplies the text of the comdirect extractor's test document.
 */
@SuppressWarnings("nls")
public class PdfImportHandlerTest
{
    private TransactionFixture f;
    private PdfTextExtraction extraction;
    private Path directory;

    @Before
    public void setUp() throws IOException
    {
        f = new TransactionFixture();
        extraction = PdfTextExtraction.install();
        directory = Files.createTempDirectory("pdf-import");
    }

    @After
    public void tearDown() throws Exception
    {
        extraction.uninstall();
        f.dispose();
        try (var files = Files.list(directory))
        {
            for (var file : files.toList())
                Files.delete(file);
        }
        Files.delete(directory);
    }

    /** an existing file with the given name */
    private String file(String name) throws IOException
    {
        var file = directory.resolve(name);
        Files.writeString(file, "%PDF-1.4 placeholder");
        return file.toAbsolutePath().toString().replace('\\', '/');
    }

    private JsonObject extract(String body) throws Exception
    {
        var response = f.call("POST", "/imports/pdf", Map.of(), body);
        assertThat(new String(response.body()), response.status(), is(200));
        return body(response);
    }

    private ApiException fails(String body)
    {
        try
        {
            f.call("POST", "/imports/pdf", Map.of(), body);
            Assert.fail("expected ApiException");
            return null;
        }
        catch (ApiException e)
        {
            return e;
        }
        catch (Exception e)
        {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testPreviewOfAPurchase() throws Exception
    {
        var json = extract("{'paths':['" + file("Kauf02.pdf") + "']}");

        assertThat(json.get("importId").getAsString().isEmpty(), is(false));
        assertThat(json.get("kind").getAsString(), is("pdf"));
        assertThat(json.has("expiresAt"), is(true));
        assertThat(json.getAsJsonArray("errors").size(), is(0));

        var items = json.getAsJsonArray("items");
        assertThat(items.size(), is(2));
        assertThat(json.getAsJsonObject("counts").get("ok").getAsInt(), is(2));

        var instrument = find(json, "instrument");
        assertThat(instrument.get("status").getAsString(), is("ok"));
        assertThat(instrument.getAsJsonObject("instrument").get("isin").getAsString(), is("US0970231058"));
        assertThat(instrument.getAsJsonObject("instrument").get("new").getAsBoolean(), is(true));

        var buy = find(json, "buy-sell");
        assertThat(buy.get("status").getAsString(), is("ok"));
        assertThat(buy.getAsJsonArray("checks").size(), is(6));
        var check = buy.getAsJsonArray("checks").get(0).getAsJsonObject();
        assertThat(check.get("code").getAsString(), is("transaction-date"));
        assertThat(check.get("status").getAsString(), is("ok"));

        var transaction = buy.getAsJsonObject("transaction");
        assertThat(transaction.get("type").getAsString(), is("buy"));
        assertThat(transaction.get("date").getAsString(), is("2016-07-18T17:02"));
        assertThat(transaction.get("shares").getAsBigDecimal().compareTo(new BigDecimal("160")), is(0));
        assertThat(transaction.getAsJsonObject("value").get("value").getAsBigDecimal()
                        .compareTo(new BigDecimal("19359.18")), is(0));
        assertThat(transaction.get("source").getAsString(), is("Kauf02.pdf"));

        // the wizard's defaults: the first EUR cash account by name, the first investment account
        assertThat(buy.getAsJsonObject("cashAccount").get("uuid").getAsString(), is(f.cash.getUUID()));
        assertThat(buy.getAsJsonObject("investmentAccount").get("uuid").getAsString(), is(f.broker.getUUID()));

        // a preview changes nothing
        assertThat(f.client.getSecurities().size(), is(2));
        assertThat(f.file.isDirty(), is(false));
        assertThat(f.host().hasAccessedOutsideUIThread(), is(false));
    }

    @Test
    public void testTargetsSteerThePreview() throws Exception
    {
        var json = extract("{'paths':['" + file("Kauf02.pdf") + "'],'targets':{'cashAccountsByCurrency':{'EUR':'"
                        + f.cash2.getUUID() + "'},'investmentAccount':'" + f.broker2.getUUID() + "'}}");

        var buy = find(json, "buy-sell");
        assertThat(buy.getAsJsonObject("cashAccount").get("uuid").getAsString(), is(f.cash2.getUUID()));
        assertThat(buy.getAsJsonObject("investmentAccount").get("uuid").getAsString(), is(f.broker2.getUUID()));
    }

    @Test
    public void testValidation() throws Exception
    {
        assertError(fails("{}"), "paths", "required");
        assertError(fails("{'paths':[]}"), "paths", "invalid-value");

        var e = fails("{'paths':['relative.pdf','" + file("statement.txt") + "','" + directory.toString()
                        .replace('\\', '/') + "/missing.pdf',42],'foo':1}");
        assertError(e, "paths[0]", "invalid-value");
        assertError(e, "paths[1]", "invalid-value");
        assertError(e, "paths[2]", "file-not-found");
        assertError(e, "paths[3]", "invalid-type");
        assertError(e, "foo", "unknown-field");

        e = fails("{'paths':['" + file("Kauf02.pdf") + "'],'targets':{'cashAccount':'nope','cashAccountsByCurrency':{'EUR':'"
                        + f.usdCash.getUUID() + "'},'bar':1}}");
        assertError(e, "targets.cashAccount", "unknown-reference");
        assertError(e, "targets.cashAccountsByCurrency.EUR", "currency-mismatch");
        assertError(e, "targets.bar", "unknown-field");
    }

    @Test
    public void testUnreadableDocumentIsReportedAsError() throws Exception
    {
        // the real extraction: PDFBox cannot parse the placeholder
        extraction.uninstall();

        var json = extract("{'paths':['" + file("broken.pdf") + "']}");
        assertThat(json.getAsJsonArray("items").size(), is(0));
        var error = json.getAsJsonArray("errors").get(0).getAsJsonObject();
        assertThat(error.get("path").getAsString().endsWith("broken.pdf"), is(true));
        assertThat(error.has("message"), is(true));
    }

    @Test
    public void testErrorsAreCondensedToOneEntryPerFile()
    {
        var doc = new File("statement.pdf");
        var other = new File("letter.pdf");
        var notSupported = (Function<String, Exception>) bank -> new UnsupportedOperationException(
                        MessageFormat.format(Messages.PDFMsgFileNotSupported, doc.getName(), bank));
        var unknownType = new UnsupportedOperationException(
                        MessageFormat.format(Messages.PDFdbMsgCannotDetermineFileType, "Bank A", doc.getName()));

        // every extractor reports the file, and PDFImportAssistant runs twice (PDFBox 3 and 1)
        var errors = new LinkedHashMap<File, List<Exception>>();
        errors.put(doc, List.of(notSupported.apply("Bank X"), unknownType, notSupported.apply("Bank Y"),
                        notSupported.apply("Bank X"), unknownType));
        errors.put(other, List.of(new UnsupportedOperationException(
                        MessageFormat.format(Messages.PDFMsgFileNotSupported, other.getName(), "Bank X"))));

        var array = PdfImportHandler.errors(errors);

        assertThat(array.size(), is(2));
        var letter = array.get(0).getAsJsonObject();
        assertThat(letter.get("message").getAsString(), is("no extractor recognized the document"));
        assertThat(letter.has("details"), is(false));
        var statement = array.get(1).getAsJsonObject();
        assertThat(statement.get("message").getAsString(), is(unknownType.getMessage()));
        assertThat(statement.has("details"), is(false));
    }

    @Test
    public void testDistinctSpecificErrorsAreListedAsDetails()
    {
        var doc = new File("statement.pdf");
        var errors = Map.<File, List<Exception>>of(doc, List.of(
                        new UnsupportedOperationException(MessageFormat
                                        .format(Messages.PDFdbMsgCannotDetermineFileType, "Bank A", doc.getName())),
                        new UnsupportedOperationException(MessageFormat
                                        .format(Messages.PDFdbMsgCannotDetermineFileType, "Bank B", doc.getName()))));

        var entry = PdfImportHandler.errors(errors).get(0).getAsJsonObject();

        assertThat(entry.get("message").getAsString(), is("no extractor recognized the document"));
        assertThat(entry.getAsJsonArray("details").size(), is(2));
    }

    /* package */ static JsonObject find(JsonObject preview, String kind)
    {
        for (var item : preview.getAsJsonArray("items"))
        {
            if (item.getAsJsonObject().get("kind").getAsString().equals(kind))
                return item.getAsJsonObject();
        }
        throw new AssertionError("no item of kind " + kind + " in " + preview);
    }
}
