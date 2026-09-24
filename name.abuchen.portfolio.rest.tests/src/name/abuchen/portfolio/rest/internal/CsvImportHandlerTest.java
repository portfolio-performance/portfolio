package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.TransactionFixture.amount;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.assertError;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.body;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class CsvImportHandlerTest
{
    private static final String CASH_CONFIG = "'config':{'target':'account-transaction','delimiter':';',"
                    + "'skipLines':0,'isFirstLineHeader':true,'columns':["
                    + "{'label':'Date','field':'date','format':'yyyy-MM-dd'},"
                    + "{'label':'Value','field':'value','format':'0,000.00'},{'label':'Note','field':'note'}]}";

    private static final String PRICE_CONFIG = "'config':{'target':'investment-vehicle-price','delimiter':',',"
                    + "'skipLines':0,'isFirstLineHeader':true,'columns':["
                    + "{'label':'Date','field':'date','format':'yyyy-MM-dd'},"
                    + "{'label':'Close','field':'quote','format':'0,000.00'}]}";

    private TransactionFixture f;
    private Path csv;

    @Before
    public void setUp() throws IOException
    {
        f = new TransactionFixture();
        csv = Files.createTempFile("import", ".csv");
    }

    @After
    public void tearDown() throws Exception
    {
        f.dispose();
        Files.deleteIfExists(csv);
    }

    private String path()
    {
        return csv.toAbsolutePath().toString().replace('\\', '/');
    }

    private void write(String content) throws IOException
    {
        Files.writeString(csv, content, StandardCharsets.UTF_8);
    }

    private JsonObject extract(String config) throws Exception
    {
        var response = f.call("POST", "/imports/csv", Map.of(), "{'path':'" + path() + "'," + config + "}");
        assertThat(new String(response.body()), response.status(), is(200));
        return body(response);
    }

    private ApiException fails(String body)
    {
        try
        {
            f.call("POST", "/imports/csv", Map.of(), body);
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
    public void testAccountTransactions() throws Exception
    {
        write("Date;Value;Note\n2026-01-05;1,000.00;first\n2026-02-05;-250.50;second\n");

        var preview = extract(CASH_CONFIG);
        assertThat(preview.get("kind").getAsString(), is("csv"));

        var items = preview.getAsJsonArray("items");
        assertThat(items.size(), is(2));
        var first = items.get(0).getAsJsonObject();
        assertThat(first.get("kind").getAsString(), is("transaction"));
        assertThat(first.get("status").getAsString(), is("ok"));
        assertThat(first.getAsJsonObject("transaction").get("type").getAsString(), is("deposit"));
        assertThat(items.get(1).getAsJsonObject().getAsJsonObject("transaction").get("type").getAsString(),
                        is("removal"));

        var result = body(f.call("POST", "/imports/" + preview.get("importId").getAsString() + "/commit", Map.of(),
                        "{'targets':{'cashAccount':'" + f.cash2.getUUID() + "'}}"));
        assertThat(result.get("count").getAsInt(), is(2));

        var transactions = f.cash2.getTransactions();
        assertThat(transactions.size(), is(2));
        var deposit = transactions.stream().filter(t -> t.getType() == AccountTransaction.Type.DEPOSIT).findFirst()
                        .orElseThrow();
        assertThat(deposit.getAmount(), is(amount("1000")));
        assertThat(deposit.getNote(), is("first"));
        assertThat(f.file.isDirty(), is(true));
        assertThat(f.host().hasAccessedOutsideUIThread(), is(false));
    }

    @Test
    public void testHistoricalPricesGoToTheNamedInstrument() throws Exception
    {
        write("Date,Close\n2026-01-05,10.50\n2026-01-06,11.25\n");

        var preview = extract(PRICE_CONFIG);
        var items = preview.getAsJsonArray("items");
        assertThat(items.size(), is(2));
        var first = items.get(0).getAsJsonObject();
        assertThat(first.get("kind").getAsString(), is("price"));
        assertThat(first.getAsJsonObject("price").get("date").getAsString(), is("2026-01-05"));
        assertThat(first.getAsJsonObject("price").get("value").getAsBigDecimal().compareTo(new BigDecimal("10.5")),
                        is(0));

        var importId = preview.get("importId").getAsString();
        try
        {
            f.call("POST", "/imports/" + importId + "/commit", Map.of(), "{}");
            Assert.fail();
        }
        catch (ApiException e)
        {
            assertError(e, "targets.instrument", "required");
        }

        var result = body(f.call("POST", "/imports/" + importId + "/commit", Map.of(),
                        "{'targets':{'instrument':'" + f.usdSecurity.getUUID() + "'}}"));
        assertThat(result.get("count").getAsInt(), is(2));
        var prices = f.usdSecurity.getPrices();
        assertThat(prices.size(), is(2));
        assertThat(prices.get(1).getDate(), is(LocalDate.parse("2026-01-06")));
        assertThat(prices.get(1).getValue(), is(Values.Quote.factorize(11.25)));
    }

    @Test
    public void testUnreadableLinesAreReportedWithTheirLineNumber() throws Exception
    {
        write("Date;Value;Note\n2026-01-05;1,000.00;first\nyesterday;lots;second\n");

        var e = fails("{'path':'" + path() + "'," + CASH_CONFIG + "}");
        assertThat(e.getStatus(), is(422));
        assertThat(e.getErrors().size(), is(1));
        assertError(e, "line[3]", "csv-parse-error");
    }

    @Test
    public void testConfigurationIsValidated() throws Exception
    {
        write("Date;Value\n");

        var e = fails("{'path':'" + path() + "','config':{'target':'bananas','delimiter':'|','skipLines':-1,"
                        + "'isFirstLineHeader':'yes','columns':[{'field':'date'}]},'x':1}");
        assertError(e, "config.target", "invalid-value");
        assertError(e, "config.delimiter", "invalid-value");
        assertError(e, "config.skipLines", "invalid-value");
        assertError(e, "config.isFirstLineHeader", "invalid-type");
        assertError(e, "config.columns[0]", "invalid-value");
        assertError(e, "x", "unknown-field");

        e = fails("{'path':'" + path() + "','config':{'target':'account-transaction','delimiter':';','skipLines':0,"
                        + "'isFirstLineHeader':true,'columns':[{'label':'Date','field':'nope'}]}}");
        assertError(e, "config.columns[0].field", "invalid-value");

        e = fails("{'config':{}}");
        assertError(e, "path", "required");
    }
}
