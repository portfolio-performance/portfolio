package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.rest.internal.mcp.McpEnvelope;
import name.abuchen.portfolio.rest.internal.mcp.McpTools;

/**
 * The two shapes every paged answer of this API owes its reader, held once for all
 * of them instead of once per endpoint: the {@code page} that says where the rest
 * of a list is comes <em>before</em> the list it pages, and what a page leaves out
 * is a whole row at a time.
 * <p/>
 * Both hold today because {@link Paging#apply} writes them that way, which is why
 * no test said so: the first endpoint that pages through a different helper, or a
 * handler that adds its array by hand, breaks the reader rather than the code. A
 * caller who never reaches the array - because an answer was shortened to fit -
 * still has to learn that it was a page and where the next one starts. A page
 * behind the mass is a page nobody can continue.
 */
public class PagingInvariantTest
{
    /**
     * The array names a paged answer pages by. {@code items} for a plain list,
     * {@code classes} beside the taxonomy headers, {@code assignments} beside one
     * class - the shape of {@code page} does not depend on which of them it is.
     */
    private static final List<String> PAGED_KEYS = List.of("items", "classes", "assignments");

    /** One list is enough to hold the invariant for; ten rows ask for three. */
    private static final int ROWS = 10;
    private static final int LIMIT = 3;

    /**
     * Whatever else the response holds and however the paged array is called, the
     * {@code page} is serialized ahead of it - and it says truthfully that the
     * response is a page: fewer rows than there are, and a cursor to go on with.
     */
    @Test
    public void pageComesBeforeTheArrayItPages()
    {
        for (var key : PAGED_KEYS)
        {
            var response = new JsonObject();
            response.addProperty("date", "2024-12-31");
            response.addProperty("totalAssets", 1234.50);
            response.addProperty("reportingCurrency", "EUR");

            var paging = Paging.parse(Paging.Kind.HOLDINGS, "holdings|file=f", null, String.valueOf(LIMIT),
                            new ArrayList<ApiException.FieldError>());
            paging.apply(Paging.Kind.HOLDINGS, response, key, rows(key), row -> row.get("uuid").getAsString(),
                            row -> row);

            var serialized = response.toString();
            assertThat(key, serialized.indexOf("\"page\""),
                            is(lessThan(serialized.indexOf("\"" + key + "\""))));

            var page = response.getAsJsonObject("page");
            assertThat(key, page.get("total").getAsInt(), is(ROWS));
            assertThat(key + " names no cursor to go on with", page.get("nextCursor").isJsonNull(), is(false));
            assertThat(key, response.getAsJsonArray(key).size(), is(LIMIT));
        }
    }

    /**
     * The same, for what a reader of the chat actually receives: the connector
     * turns an oversized list into a page, and in that answer too the guidance and
     * the {@code page} stand ahead of the rows - the rows are a leading piece of
     * the list in its own order, never a scattering of what happened to fit.
     */
    @Test
    public void aFittedPageStaysAheadOfTheRowsItHolds()
    {
        var tool = McpTools.byName("pp_list_holdings").orElseThrow();

        var items = new JsonArray();
        for (int ii = 0; ii < 300; ii++)
        {
            var holding = new JsonObject();
            holding.addProperty("uuid", String.format("00000000-0000-0000-0000-%012d", ii));
            holding.addProperty("name", "ACME " + ii + " " + "x".repeat(600));
            holding.addProperty("weight", 0.001);
            holding.addProperty("cursor", Paging.encode(Paging.Kind.HOLDINGS, "scope", "u" + ii));
            items.add(holding);
        }
        var payload = new JsonObject();
        payload.addProperty("date", "2024-12-31");
        payload.add("items", items);

        // a budget of its own, not the connector's cap: the invariant is about the
        // shape of the answer, and a shape test should not drift with the cap
        var budget = 4_000;
        var fitted = McpEnvelope.fitPage(tool, new JsonObject(), payload, budget, Boolean.FALSE);
        assertThat(fitted, notNullValue());
        assertThat(fitted.length(), is(lessThanOrEqualTo(budget)));
        assertThat(fitted, containsString("\"page\""));
        assertThat(fitted.indexOf("\"page\""), is(lessThan(fitted.indexOf("\"items\""))));

        // guidance first, body after the last blank line
        var body = JsonParser.parseString(fitted.substring(fitted.lastIndexOf("\n\n") + 2)).getAsJsonObject();
        var table = body.getAsJsonObject("items");
        var rows = table.getAsJsonArray("rows");
        assertThat(rows.size(), is(greaterThan(1)));
        assertThat(rows.size(), is(lessThan(300)));
        assertThat(body.getAsJsonObject("page").get("total").getAsInt(), is(300));

        // every row of the page is one of the list's own, in the list's own order
        var columns = new ArrayList<String>();
        table.getAsJsonArray("columns").forEach(column -> columns.add(column.getAsString()));
        var uuid = columns.indexOf("uuid");
        assertThat("the page lost the uuid column", uuid, is(greaterThan(-1)));

        var previous = "";
        for (var row : rows)
        {
            var key = row.getAsJsonArray().get(uuid).getAsString();
            assertThat(key, is(greaterThan(previous)));
            previous = key;
        }
    }

    /**
     * A tool that offers a cursor without a limit is half a page: the reader can
     * go on but cannot ask for a smaller piece, which is the half an agent needs
     * when one page is already too large for its own context. The catalog is where
     * a new endpoint would get this wrong quietly, so the catalog is where it is
     * checked - every tool, not the ones thought of when the invariant was written.
     */
    @Test
    public void everyToolThatPagesDeclaresItsLimitToo()
    {
        for (var tool : McpTools.all())
        {
            var properties = tool.inputSchema().getAsJsonObject("properties");
            if (properties == null || properties.get("cursor") == null)
                continue;

            var limit = properties.get("limit");
            assertThat(tool.name() + " pages without declaring a limit", limit, notNullValue());
            assertThat(tool.name() + " limit is not a number", limit.getAsJsonObject().get("type").getAsString(),
                            is("integer"));
            assertThat(tool.name() + " limit accepts zero", limit.getAsJsonObject().get("minimum").getAsInt(),
                            is(1));
            assertThat(tool.name() + " has no cursor binding", tool.binding("cursor"), notNullValue());
            assertThat(tool.name() + " has no limit binding", tool.binding("limit"), notNullValue());
        }
    }

    private static List<JsonObject> rows(String listKey)
    {
        var rows = new ArrayList<JsonObject>();
        for (int ii = 0; ii < ROWS; ii++)
        {
            var row = new JsonObject();
            row.addProperty("uuid", String.format("00000000-0000-0000-0000-%012d", ii));
            row.addProperty("name", listKey + " " + ii);
            row.addProperty("weight", 0.1);
            rows.add(row);
        }
        return rows;
    }
}
