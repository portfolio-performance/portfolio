package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.PdfImportHandlerTest.find;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.assertError;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.body;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.shares;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.PortfolioTransaction;

/** The commit step of an import, on the PDF import of {@link PdfTextExtraction} */
@SuppressWarnings("nls")
public class ImportCommitTest
{
    private TransactionFixture f;
    private PdfTextExtraction extraction;
    private Path pdf;

    @Before
    public void setUp() throws IOException
    {
        f = new TransactionFixture();
        extraction = PdfTextExtraction.install();
        pdf = Files.createTempFile("Kauf02", ".pdf");
    }

    @After
    public void tearDown() throws Exception
    {
        extraction.uninstall();
        f.dispose();
        Files.deleteIfExists(pdf);
    }

    private JsonObject extract() throws Exception
    {
        return body(f.call("POST", "/imports/pdf", Map.of(),
                        "{'paths':['" + pdf.toAbsolutePath().toString().replace('\\', '/') + "']}"));
    }

    private JsonObject commit(String importId, String body) throws Exception
    {
        var response = f.call("POST", "/imports/" + importId + "/commit", Map.of(), body);
        assertThat(new String(response.body()), response.status(), is(200));
        return body(response);
    }

    private ApiException commitFails(String importId, String body)
    {
        try
        {
            f.call("POST", "/imports/" + importId + "/commit", Map.of(), body);
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

    private String targets()
    {
        return "'targets':{'cashAccount':'" + f.cash2.getUUID() + "','investmentAccount':'" + f.broker2.getUUID()
                        + "'}";
    }

    @Test
    public void testCommitImportsIntoTheChosenAccounts() throws Exception
    {
        var importId = extract().get("importId").getAsString();

        var result = commit(importId, "{" + targets() + "}");

        assertThat(result.get("count").getAsInt(), is(2));
        assertThat(result.getAsJsonArray("instruments").size(), is(1));
        var boeing = f.client.getSecurities().stream().filter(s -> "US0970231058".equals(s.getIsin())).findFirst()
                        .orElseThrow();
        assertThat(result.getAsJsonArray("instruments").get(0).getAsJsonObject().get("uuid").getAsString(),
                        is(boeing.getUUID()));

        assertThat(f.broker2.getTransactions().size(), is(1));
        assertThat(f.cash2.getTransactions().size(), is(1));
        assertThat(f.broker.getTransactions().isEmpty(), is(true));
        var buy = f.broker2.getTransactions().get(0);
        assertThat(buy.getType(), is(PortfolioTransaction.Type.BUY));
        assertThat(buy.getShares(), is(shares("160")));
        assertThat(buy.getNote().startsWith("Ord.-Nr."), is(true));

        var transaction = result.getAsJsonArray("transactions").get(0).getAsJsonObject();
        assertThat(transaction.get("uuid").getAsString(), is(buy.getUUID()));
        assertThat(transaction.getAsJsonObject("linked").getAsJsonObject("owner").get("uuid").getAsString(),
                        is(f.cash2.getUUID()));

        assertThat(f.file.isDirty(), is(true));
        assertThat(f.host().hasAccessedOutsideUIThread(), is(false));

        // the session is consumed
        assertThat(commitFails(importId, "{}").getStatus(), is(404));
    }

    @Test
    public void testDuplicatesAreFlaggedAndOnlyImportedWhenSelected() throws Exception
    {
        commit(extract().get("importId").getAsString(), "{" + targets() + "}");

        // the same document again, checked against the targets it was imported into
        var preview = body(f.call("POST", "/imports/pdf", Map.of(), "{'paths':['"
                        + pdf.toAbsolutePath().toString().replace('\\', '/') + "']," + targets() + "}"));
        var items = preview.getAsJsonArray("items");
        assertThat(items.size(), is(1)); // the instrument exists now
        var buy = find(preview, "buy-sell");
        assertThat(buy.get("status").getAsString(), is("warning"));
        var duplicates = 0;
        for (var check : buy.getAsJsonArray("checks"))
        {
            var c = check.getAsJsonObject();
            if (c.get("code").getAsString().equals("duplicates"))
            {
                assertThat(c.get("status").getAsString(), is("warning"));
                duplicates++;
            }
        }
        assertThat(duplicates, is(1));

        var importId = preview.get("importId").getAsString();

        // without select: only the items without warnings
        var dryRun = body(f.call("POST", "/imports/" + importId + "/commit", Map.of("dry_run", "true"),
                        "{" + targets() + "}"));
        assertThat(dryRun.get("count").getAsInt(), is(0));

        // selected explicitly: imported
        var result = commit(importId, "{'select':[0]," + targets() + "}");
        assertThat(result.get("count").getAsInt(), is(1));
        assertThat(f.broker2.getTransactions().size(), is(2));
    }

    @Test
    public void testDryRunChangesNothingAndKeepsTheSession() throws Exception
    {
        var importId = extract().get("importId").getAsString();

        var response = f.call("POST", "/imports/" + importId + "/commit", Map.of("dry_run", "true"), "{}");
        var json = body(response);
        assertThat(json.get("dryRun").getAsBoolean(), is(true));
        assertThat(json.get("count").getAsInt(), is(2));
        assertThat(f.client.getSecurities().size(), is(2));
        assertThat(f.file.isDirty(), is(false));

        assertThat(commit(importId, "{'options':{'importNotes':false}}").get("count").getAsInt(), is(2));
        assertThat(f.broker.getTransactions().get(0).getNote() == null, is(true));
    }

    @Test
    public void testErrorItemsAreNotImportable() throws Exception
    {
        // no EUR cash account: the purchase has nowhere to go
        f.client.removeAccount(f.cash);
        f.client.removeAccount(f.cash2);

        var preview = extract();
        var buy = find(preview, "buy-sell");
        assertThat(buy.get("status").getAsString(), is("error"));
        assertThat(buy.getAsJsonArray("checks").get(0).getAsJsonObject().get("code").getAsString(), is("targets"));
        var buyIndex = buy.get("index").getAsInt();

        var importId = preview.get("importId").getAsString();
        var e = commitFails(importId, "{'options':{'foo':true},'bar':1}");
        assertError(e, "options.foo", "unknown-field");
        assertError(e, "bar", "unknown-field");

        e = commitFails(importId, "{'select':[" + buyIndex + ", 7, 'x']}");
        assertError(e, "select[0]", "item-not-importable");
        assertError(e, "select[1]", "invalid-value");
        assertError(e, "select[2]", "invalid-type");
        assertThat(f.file.isDirty(), is(false));

        // without select, only the instrument is imported
        var result = commit(importId, "{}");
        assertThat(result.get("count").getAsInt(), is(1));
        assertThat(result.getAsJsonArray("transactions").size(), is(0));
    }

    @Test
    public void testCommitIsAWrite() throws Exception
    {
        var importId = extract().get("importId").getAsString();

        f.host().setUserEditing(true);
        assertThat(commitFails(importId, "{}").getStatus(), is(423));
        f.host().setUserEditing(false);

        assertThat(commitFails("unknown", "{}").getStatus(), is(404));
        assertThat(commit(importId, null).get("count").getAsInt(), is(2));
    }
}
