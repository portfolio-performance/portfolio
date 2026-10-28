package name.abuchen.portfolio.rest.internal.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The compact shape a model reads: fewer tokens, and not one value lost or
 * changed - above all no amount left without the currency it is in.
 */
@SuppressWarnings("nls")
public class McpCompactTest
{
    private static final McpTool HOLDINGS = McpTools.byName("pp_list_holdings").orElseThrow();
    private static final McpTool FILES = McpTools.byName("pp_list_files").orElseThrow();
    private static final McpTool PERFORMANCE = McpTools.byName("pp_get_overall_performance").orElseThrow();
    private static final McpTool INSTRUMENTS = McpTools.byName("pp_list_instruments").orElseThrow();
    private static final McpTool TRADES = McpTools.byName("pp_list_trades").orElseThrow();

    /** a holding in the reporting currency, one in USD, and a cash account */
    private static final String HOLDINGS_PAYLOAD = """
                    {"date":"2026-10-28","reportingCurrency":"EUR","totalAssets":{"value":1000.5,"currency":"EUR"},
                     "items":[
                      {"type":"instrument","uuid":"a","name":"ACME","shares":10,
                       "price":{"value":50,"currency":"EUR","date":"2026-10-27"},
                       "priceStale":false,"priceAgeDays":1,"priceMissedTradingDays":0,
                       "valuation":{"value":500,"currency":"EUR"},"weight":0.5},
                      {"type":"instrument","uuid":"b","name":"USA 2051","shares":80,
                       "price":{"value":51.95,"currency":"USD","date":"2026-08-21"},
                       "priceStale":true,"priceAgeDays":68,"priceMissedTradingDays":47,
                       "valuation":{"value":300,"currency":"EUR"},"weight":0.3,
                       "localValuation":{"value":4156,"currency":"USD"}},
                      {"type":"cash-account","uuid":"c","name":"Giro",
                       "valuation":{"value":200.5,"currency":"EUR"},"weight":0.2}
                     ],"fxWarnings":[]}""";

    @Test
    public void testItemsBecomeATable()
    {
        var table = compact(HOLDINGS, HOLDINGS_PAYLOAD).getAsJsonObject("items");

        assertThat(columns(table), is(List.of("type", "uuid", "name", "shares", "price", "price.currency",
                        "price.date", "priceNote", "valuation", "weight", "localValuation", "localValuation.currency")));
        assertThat(table.getAsJsonArray("rows").size(), is(3));
        // every row as long as the columns, missing fields as null
        for (var row : table.getAsJsonArray("rows"))
            assertThat(row.getAsJsonArray().size(), is(12));
    }

    /** a value in the reporting currency loses its currency - and only such a value */
    @Test
    public void testAnAmountInTheReportingCurrencyIsABareNumber()
    {
        var report = compact(HOLDINGS, HOLDINGS_PAYLOAD);
        var table = report.getAsJsonObject("items");

        assertThat(report.get("totalAssets").getAsDouble(), is(1000.5));
        assertThat(cell(table, 2, "valuation").getAsDouble(), is(200.5));
        assertThat(columns(table).contains("valuation.currency"), is(false));
    }

    /** the amount in USD keeps its currency, in a column beside it */
    @Test
    public void testAnAmountInAnotherCurrencyKeepsItsCurrency()
    {
        var table = compact(HOLDINGS, HOLDINGS_PAYLOAD).getAsJsonObject("items");

        assertThat(cell(table, 1, "localValuation").getAsDouble(), is(4156d));
        assertThat(cell(table, 1, "localValuation.currency").getAsString(), is("USD"));
        assertThat(cell(table, 0, "localValuation").isJsonNull(), is(true));
    }

    /** a price is a quote in the instrument's currency, not an amount of the report */
    @Test
    public void testAPriceKeepsItsCurrencyAlways()
    {
        var table = compact(HOLDINGS, HOLDINGS_PAYLOAD).getAsJsonObject("items");

        assertThat(cell(table, 0, "price").getAsDouble(), is(50d));
        assertThat(cell(table, 0, "price.currency").getAsString(), is("EUR"));
        assertThat(cell(table, 1, "price.currency").getAsString(), is("USD"));
        assertThat(cell(table, 1, "price.date").getAsString(), is("2026-08-21"));
    }

    /** one column with even one amount in another currency keeps the currency for all of them */
    @Test
    public void testAColumnWithOneForeignAmountKeepsItsCurrencyColumn()
    {
        var table = compact(PERFORMANCE, """
                        {"reportingCurrency":"EUR","items":[
                          {"name":"a","fees":{"value":1,"currency":"EUR"}},
                          {"name":"b","fees":{"value":2,"currency":"CHF"}}]}""").getAsJsonObject("items");

        assertThat(columns(table), is(List.of("name", "fees", "fees.currency")));
        assertThat(cell(table, 0, "fees.currency").getAsString(), is("EUR"));
        assertThat(cell(table, 1, "fees.currency").getAsString(), is("CHF"));
    }

    /** without a reporting currency there is nothing to drop a currency in favour of */
    @Test
    public void testWithoutAReportingCurrencyEveryAmountKeepsItsCurrency()
    {
        var table = compact(PERFORMANCE, """
                        {"items":[{"name":"a","fees":{"value":1,"currency":"EUR"}}]}""").getAsJsonObject("items");

        assertThat(columns(table), is(List.of("name", "fees", "fees.currency")));
    }

    @Test
    public void testAGroupBecomesColumnsNamedWithADot()
    {
        var table = compact(PERFORMANCE, """
                        {"reportingCurrency":"EUR","items":[{"name":"a",
                          "gains":{"realizedCapitalGains":{"value":5,"currency":"EUR"}},
                          "risk":{"volatility":0.2,"longestDrawdownDays":3}}]}""").getAsJsonObject("items");

        assertThat(columns(table),
                        is(List.of("name", "gains.realizedCapitalGains", "risk.volatility", "risk.longestDrawdownDays")));
        assertThat(cell(table, 0, "gains.realizedCapitalGains").getAsDouble(), is(5d));
    }

    /** the four age fields become one note, and only where there is something to say */
    @Test
    public void testThePriceAgeIsANoteOnlyWhenItMatters()
    {
        var table = compact(HOLDINGS, HOLDINGS_PAYLOAD).getAsJsonObject("items");

        assertThat(cell(table, 0, "priceNote").isJsonNull(), is(true));
        assertThat(cell(table, 1, "priceNote").getAsString(), is("stale: 47 trading days without a quote"));
        assertThat(columns(table).contains("priceStale"), is(false));
        assertThat(columns(table).contains("priceAgeDays"), is(false));

        var manual = compact(HOLDINGS, """
                        {"reportingCurrency":"EUR","items":[{"name":"ZF",
                          "priceStale":false,"priceAgeDays":68,"priceMissedTradingDays":47,"priceManual":true}]}""")
                        .getAsJsonObject("items");
        assertThat(cell(manual, 0, "priceNote").getAsString(), is("manual price, 68 days old"));
    }

    @Test
    public void testTheFileListKeepsOnlyWhatIsSent()
    {
        var table = compact(FILES, """
                        {"items":[{"id":"u-1","use":"u-1","label":"Richard.portfolio","path":"C:\\\\x.portfolio"}]}""")
                        .getAsJsonObject("items");

        assertThat(columns(table), is(List.of("use", "label")));
    }

    /** no value is rounded: the same portfolio reads the same through curl */
    @Test
    public void testNoValueIsChanged()
    {
        var table = compact(HOLDINGS, """
                        {"reportingCurrency":"EUR","items":[{"name":"dust","weight":0.0000025924239650152385}]}""")
                        .getAsJsonObject("items");

        assertThat(cell(table, 0, "weight").toString(), is("0.0000025924239650152385"));
    }

    /** an empty list stays an empty list, not a table without columns */
    @Test
    public void testAnEmptyListStaysAList()
    {
        var report = compact(HOLDINGS, "{\"reportingCurrency\":\"EUR\",\"items\":[]}");

        assertThat(report.get("items").isJsonArray(), is(true));
    }

    /** the envelope says what the shape means, and which currency the bare amounts are in */
    @Test
    public void testTheEnvelopeExplainsTheShape()
    {
        var rendered = McpEnvelope.render(HOLDINGS, new JsonObject(), JsonParser.parseString(HOLDINGS_PAYLOAD),
                        List.of());

        assertThat(rendered, containsString("`items` is a table"));
        assertThat(rendered, containsString("Amounts without a currency are in **EUR**"));
        assertThat(rendered, not(containsString("\"priceStale\"")));
        // the table has price.currency and localValuation.currency
        assertThat(rendered, containsString("**, the `reportingCurrency`. A column `<name>.currency` gives the "
                        + "currency of the column before it."));
    }

    /**
     * A report without a table names the currency of its bare amounts, but
     * not a {@code <name>.currency} column it does not have.
     */
    @Test
    public void testTheCurrencyNoteNamesColumnsOnlyWhereThereAreSome()
    {
        var payload = """
                        {"openingDate":"2025-12-31","closingDate":"2026-10-28","reportingCurrency":"EUR",
                         "costMethod":"fifo","ttwror":0.1,"irr":0.1,
                         "breakdown":{"openingValue":{"value":100,"currency":"EUR"},
                          "unrealizedCapitalGains":{"value":10,"currency":"EUR"},
                          "realizedCapitalGains":{"value":0,"currency":"EUR"},
                          "income":{"value":0,"currency":"EUR"},"fees":{"value":0,"currency":"EUR"},
                          "taxes":{"value":0,"currency":"EUR"},"currencyGains":{"value":0,"currency":"EUR"},
                          "netDeposits":{"value":0,"currency":"EUR"},"closingValue":{"value":110,"currency":"EUR"}},
                         "fxWarnings":[]}""";

        var rendered = McpEnvelope.render(PERFORMANCE, new JsonObject(), JsonParser.parseString(payload), List.of());

        assertThat(rendered, containsString("Amounts without a currency are in **EUR**, the `reportingCurrency`."));
        assertThat(rendered, not(containsString("<name>.currency")));
    }

    /** a table whose only currency column is a price's still explains that column */
    @Test
    public void testAPriceCurrencyColumnIsExplainedWithoutBareAmounts()
    {
        var payload = """
                        {"items":[{"name":"ACME","price":{"value":50,"currency":"USD","date":"2026-10-27"}}]}""";

        var compacted = McpCompact.compact(HOLDINGS, JsonParser.parseString(payload));
        assertThat(compacted.currencyDropped() == null, is(true));
        assertThat(compacted.currencyColumn(), is(true));

        var rendered = McpEnvelope.render(HOLDINGS, new JsonObject(), JsonParser.parseString(payload), List.of());
        assertThat(rendered, containsString("A column `<name>.currency` gives the currency of the column before it."));
        assertThat(rendered, not(containsString("Amounts without a currency")));
    }

    /**
     * In a long table, the fields only a few rows have go into `more`: per
     * row an object of the ones it has, or null - and every value is still
     * there, under its column name.
     */
    @Test
    public void testSparseColumnsOfALongTableGoIntoMore()
    {
        var items = new StringBuilder();
        for (int ii = 0; ii < 40; ii++)
        {
            if (ii > 0)
                items.append(',');
            items.append("{\"uuid\":\"u").append(ii).append("\",\"name\":\"I ").append(ii).append('"');
            if (ii == 3)
                items.append(",\"note\":\"kept\",\"attributes\":{\"ter\":0.002}");
            if (ii == 7)
                items.append(",\"attributes\":{\"aum\":12}");
            // set in every row: stays a column
            items.append(",\"isin\":\"X").append(ii).append("\"}");
        }
        var result = McpCompact.compact(INSTRUMENTS, JsonParser.parseString("{\"items\":[" + items + "]}"));
        var table = result.payload().getAsJsonObject().getAsJsonObject("items");

        assertThat(columns(table), is(List.of("uuid", "name", "isin", "more")));
        assertThat(result.moreColumn(), is(true));
        assertThat(cell(table, 3, "more").getAsJsonObject().get("note").getAsString(), is("kept"));
        assertThat(cell(table, 3, "more").getAsJsonObject().get("attributes.ter").getAsDouble(), is(0.002));
        assertThat(cell(table, 7, "more").toString(), is("{\"attributes.aum\":12}"));
        assertThat(cell(table, 0, "more").isJsonNull(), is(true));

        var rendered = McpEnvelope.render(INSTRUMENTS, new JsonObject(),
                        JsonParser.parseString("{\"items\":[" + items + "]}"), List.of());
        assertThat(rendered, containsString("`more` holds the fields only a few rows have"));
    }

    /** a short table, or one with a single sparse column, keeps its columns */
    @Test
    public void testAShortTableOrASingleSparseColumnStaysAsItIs()
    {
        var shortTable = McpCompact.compact(INSTRUMENTS, JsonParser.parseString(
                        "{\"items\":[{\"name\":\"A\",\"note\":\"n\"},{\"name\":\"B\"},{\"name\":\"C\",\"wkn\":\"1\"}]}"));
        assertThat(shortTable.moreColumn(), is(false));

        var items = new StringBuilder();
        for (int ii = 0; ii < 40; ii++)
            items.append(ii > 0 ? "," : "").append("{\"name\":\"I").append(ii).append('"')
                            .append(ii == 5 ? ",\"note\":\"n\"}" : "}");
        var oneSparse = McpCompact.compact(INSTRUMENTS, JsonParser.parseString("{\"items\":[" + items + "]}"));
        assertThat(oneSparse.moreColumn(), is(false));
        assertThat(columns(oneSparse.payload().getAsJsonObject().getAsJsonObject("items")),
                        is(List.of("name", "note")));
    }

    /** the null line only where a null is sent: a table without one does not get it */
    @Test
    public void testTheNullNoteOnlyWhereANullIsSent()
    {
        var files = McpEnvelope.render(FILES, new JsonObject(), JsonParser.parseString(
                        "{\"items\":[{\"id\":\"a\",\"use\":\"a\",\"label\":\"A\",\"path\":\"/a\"}]}"), List.of());
        assertThat(files, not(containsString("`null` means")));

        var withNull = McpEnvelope.render(FILES, new JsonObject(), JsonParser.parseString(
                        "{\"items\":[{\"use\":\"a\",\"label\":\"A\",\"alias\":\"x\"},{\"use\":\"b\",\"label\":\"B\"}]}"),
                        List.of());
        assertThat(withNull, containsString("`null` means"));
    }

    /** an irr annualized from a few days is named as such, with the largest of them */
    @Test
    public void testAnExtremeIrrIsNamed()
    {
        var payload = JsonParser.parseString("""
                        {"reportingCurrency":"EUR","items":[
                         {"instrument":{"uuid":"a","name":"A"},"holdingPeriodDays":1,"irr":25586198.51754072,"return":0.0478},
                         {"instrument":{"uuid":"b","name":"B"},"holdingPeriodDays":400,"irr":0.08,"return":0.09},
                         {"instrument":{"uuid":"c","name":"C"},"holdingPeriodDays":6,"irr":-150.5,"return":-0.2}]}""");

        var rendered = McpEnvelope.render(TRADES, new JsonObject(), payload, List.of());

        assertThat(rendered, containsString("2 `irr` values are above 10,000% a year in size (up to 25586199)"));
        assertThat(rendered, containsString("Report `return` for those rows instead."));

        var ordinary = McpEnvelope.render(TRADES, new JsonObject(), JsonParser.parseString(
                        "{\"items\":[{\"irr\":0.5,\"return\":0.1}]}"), List.of());
        assertThat(ordinary, not(containsString("10,000%")));
    }

    /** a trade counting transactions booked ahead is named before the figures */
    @Test
    public void testATradeWithLaterTransactionsIsNamedBeforeTheFigures()
    {
        var payload = JsonParser.parseString("""
                        {"valuationDate":"2026-10-28","items":[
                         {"instrument":{"uuid":"a","name":"ZF"},"shares":12.4073,"transactionsAfterValuationDate":5},
                         {"instrument":{"uuid":"b","name":"B"},"shares":1}]}""");

        var rendered = McpEnvelope.render(TRADES, new JsonObject(), payload, List.of());

        assertThat(rendered, containsString("**1 trade includes transactions dated after `valuationDate`**"));
        assertThat(rendered.indexOf("1 trade includes"), is(lessThan(rendered.indexOf("\"rows\""))));
    }

    /** a stale exchange rate is named before the figures, with the pair */
    @Test
    public void testAStaleExchangeRateIsNamedBeforeTheFigures()
    {
        var payload = JsonParser.parseString("""
                        {"reportingCurrency":"RUB","ttwror":0.12,"fxWarnings":[],
                         "fxStaleRates":[{"currency":"EUR","reportingCurrency":"RUB","rateDate":"2022-03-01","daysOld":1702}]}""");

        var rendered = McpEnvelope.render(PERFORMANCE, new JsonObject(), payload, List.of());

        assertThat(rendered, containsString("**Exchange rates out of date.** 1 currency pair was converted"));
        assertThat(rendered, containsString("\"daysOld\":1702"));
        assertThat(McpEnvelope.render(PERFORMANCE, new JsonObject(), JsonParser.parseString(
                        "{\"fxWarnings\":[],\"fxStaleRates\":[]}"), List.of()), not(containsString("out of date")));
    }

    /** valued at a future date, the price note says that its age is today's */
    @Test
    public void testThePriceNoteOfAFutureDateSaysAsOfToday()
    {
        var future = LocalDate.now().plusYears(2).toString();
        var payload = HOLDINGS_PAYLOAD.replace("\"date\":\"2026-10-28\"", "\"date\":\"" + future + "\"");

        var table = compact(HOLDINGS, payload).getAsJsonObject("items");
        assertThat(cell(table, 1, "priceNote").getAsString(),
                        is("stale: 47 trading days without a quote as of today"));

        var today = compact(HOLDINGS, HOLDINGS_PAYLOAD).getAsJsonObject("items");
        assertThat(cell(today, 1, "priceNote").getAsString(), is("stale: 47 trading days without a quote"));
    }

    /**
     * A table cut to fit ends after a whole row: a cut between two cells would
     * leave a row short of its columns, which reads as a complete row with the
     * wrong values in it.
     */
    @Test
    public void testATruncatedTableEndsAfterAWholeRow()
    {
        var rows = new StringBuilder();
        for (int ii = 0; ii < 200; ii++)
        {
            if (ii > 0)
                rows.append(',');
            rows.append("[\"instrument ").append(ii).append("\",").append(ii).append(",0.5]");
        }
        var payload = "{\"items\":{\"columns\":[\"name\",\"shares\",\"weight\"],\"rows\":[" + rows + "]}}";

        var cut = McpEnvelope.cap(payload, 1_000);
        var json = cut.substring(0, cut.indexOf("\n\n[TRUNCATED"));
        var parsed = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("items");

        for (var row : parsed.getAsJsonArray("rows"))
            assertThat(row.getAsJsonArray().size(), is(3));
    }

    private static JsonObject compact(McpTool tool, String payload)
    {
        return McpCompact.compact(tool, JsonParser.parseString(payload)).payload().getAsJsonObject();
    }

    private static List<String> columns(JsonObject table)
    {
        var columns = new ArrayList<String>();
        table.getAsJsonArray("columns").forEach(column -> columns.add(column.getAsString()));
        return columns;
    }

    private static JsonElement cell(JsonObject table, int row, String column)
    {
        JsonArray cells = table.getAsJsonArray("rows").get(row).getAsJsonArray();
        return cells.get(columns(table).indexOf(column));
    }
}
