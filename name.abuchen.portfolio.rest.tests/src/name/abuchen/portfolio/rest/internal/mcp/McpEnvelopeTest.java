package name.abuchen.portfolio.rest.internal.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;

import java.util.List;

import org.junit.Test;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The guidance riding with a result is conditional on the payload: a response
 * with no rates gets no fractions reminder, which is what keeps every line of a
 * result load-bearing rather than skimmed.
 */
@SuppressWarnings("nls")
public class McpEnvelopeTest
{
    private static final McpTool HOLDINGS = McpTools.byName("pp_list_holdings").orElseThrow();
    private static final McpTool TRADES = McpTools.byName("pp_list_trades").orElseThrow();
    private static final McpTool INSTRUMENTS = McpTools.byName("pp_list_instruments").orElseThrow();
    private static final McpTool UPDATE = McpTools.byName("pp_update_instrument").orElseThrow();
    private static final McpTool PERFORMANCE = McpTools.byName("pp_list_instrument_performance").orElseThrow();

    /** the single most consequential misreading available */
    @Test
    public void testARateBearingPayloadCarriesTheFractionsRule()
    {
        var rendered = render(HOLDINGS, "{\"items\":[{\"weight\":0.6875}]}");

        assertThat(rendered, containsString("fractions, not percentages"));
        assertThat(rendered, containsString("0.6875"));
    }

    @Test
    public void testAPayloadWithoutRatesCarriesNoFractionsRule()
    {
        assertThat(render(INSTRUMENTS, "{\"items\":[{\"name\":\"ACME\"}]}"), not(containsString("fractions")));
    }

    /** both kinds of null the API produces: an undefined figure, and an index's currencyCode */
    @Test
    public void testANullIsNeverZero()
    {
        assertThat(render(PERFORMANCE, "{\"items\":[{\"irr\":null}]}"), containsString("never zero"));
        assertThat(render(INSTRUMENTS, "{\"items\":[{\"currencyCode\":null}]}"),
                        containsString("unknown or does not apply"));
    }

    @Test
    public void testOffsetLessDateTimesAreFlagged()
    {
        assertThat(render(TRADES, "{\"items\":[{\"start\":\"2024-03-04T00:00:00\"}]}"),
                        containsString("no timezone"));
        // a plain date is not a date-time and needs no warning
        assertThat(render(TRADES, "{\"items\":[{\"start\":\"2024-03-04\"}]}"), not(containsString("no timezone")));
    }

    /** warnings come first: they change what everything below them means */
    @Test
    public void testWarningsLeadTheResultAndAreNotAnError()
    {
        var rendered = render(PERFORMANCE, "{\"items\":[],\"warnings\":[{\"code\":\"no-rate\"}]}");

        assertThat(rendered.startsWith("**Partial result — 1 warning.**"), is(true));
        assertThat(rendered, containsString("no-rate"));
    }

    /**
     * Money that was never converted is the other outcome that changes what
     * every figure below means, so it is stated before the payload rather than
     * as a footer note.
     */
    @Test
    public void testUnconvertedMoneyIsStatedBeforeThePayload()
    {
        var rendered = render(HOLDINGS,
                        "{\"items\":[],\"fxWarnings\":[{\"currency\":\"USD\",\"reportingCurrency\":\"EUR\",\"reason\":\"no-series\"}]}");

        assertThat(rendered.startsWith("**Converted at 1:1, not at an exchange rate.**"), is(true));
        assertThat(rendered, containsString("no-series"));
        assertThat(rendered, containsString("shown at face value"));
        assertThat(rendered.indexOf("Converted at 1:1"), is(lessThan(rendered.indexOf("\"items\""))));
    }

    /**
     * The guidance quotes a bounded number of pairs and refers to the payload
     * for the rest: the full list is there anyway, and a guidance section
     * without a bound is the one part of a result the cap cannot cut.
     */
    @Test
    public void testALongWarningListIsQuotedOnlyInPart()
    {
        var pairs = new StringBuilder();
        for (int ii = 0; ii < 25; ii++)
            pairs.append(ii == 0 ? "" : ",").append("{\"currency\":\"X").append(ii)
                            .append("\",\"reportingCurrency\":\"EUR\",\"reason\":\"no-series\"}");

        var rendered = render(HOLDINGS, "{\"items\":[],\"fxWarnings\":[" + pairs + "]}");
        var guidance = rendered.substring(0, rendered.indexOf("{\"items\""));

        assertThat(guidance, containsString("25 currency pairs"));
        assertThat(guidance, containsString("\"X9\""));
        assertThat(guidance, not(containsString("\"X10\"")));
        assertThat(guidance, containsString("(15 more in `fxWarnings` below)"));
    }

    /** every report carries the array, empty or not; an empty one adds no text */
    @Test
    public void testAnEmptyFxWarningsArrayAddsNothing()
    {
        assertThat(render(HOLDINGS, "{\"items\":[],\"fxWarnings\":[]}"), not(containsString("1:1")));
    }

    /** a failed computation still leads; the unconverted pairs follow it */
    @Test
    public void testWarningsStillLeadWhenBothArePresent()
    {
        var rendered = render(TRADES,
                        "{\"items\":[],\"warnings\":[{\"code\":\"incomplete\"}],\"fxWarnings\":[{\"currency\":\"USD\",\"reportingCurrency\":\"EUR\",\"reason\":\"no-rate\"}]}");

        assertThat(rendered.startsWith("**Partial result"), is(true));
        assertThat(rendered.indexOf("Converted at 1:1"), is(greaterThan(rendered.indexOf("Partial result"))));
    }

    /** a patch that named a field is an edit the user has to save or discard */
    @Test
    public void testAWriteThatNamedAFieldSaysItIsNotSavedToDisk()
    {
        assertThat(renderWith(UPDATE, "{\"uuid\":\"0a9af43d-f91a-408d-b432-a119446bce43\",\"note\":\"x\"}", //$NON-NLS-1$
                        "{\"uuid\":\"x\"}"), containsString("not saved to disk"));
        assertThat(render(HOLDINGS, "{\"items\":[]}"), not(containsString("not saved to disk")));
    }

    /** an empty merge patch changes nothing and marks nothing dirty, so saying it did is a lie */
    @Test
    public void testAnEmptyPatchDoesNotClaimAnEditHappened()
    {
        var rendered = renderWith(UPDATE, "{}", "{\"uuid\":\"x\"}"); //$NON-NLS-1$ //$NON-NLS-2$

        assertThat(rendered, containsString("Nothing changed"));
        assertThat(rendered, containsString("not** marked dirty"));
        assertThat(rendered, not(containsString("not saved to disk")));
    }

    /** a delete is not a patch: it always changed something */
    @Test
    public void testADeleteStillSaysItIsNotSavedToDisk()
    {
        assertThat(renderWith(McpTools.byName("pp_delete_instrument").orElseThrow(), "{}", "{}"), //$NON-NLS-1$ //$NON-NLS-2$
                        containsString("not saved to disk"));
    }

    @Test
    public void testANoContentResponseSaysSoRatherThanPrintingNull()
    {
        var rendered = McpEnvelope.render(UPDATE, new JsonObject(), JsonNull.INSTANCE, List.of()); //$NON-NLS-1$

        assertThat(rendered, containsString("returned nothing to show"));
        assertThat(rendered, not(containsString("null")));
    }

    /** numbers pass through unrounded: the same portfolio must read the same through curl */
    @Test
    public void testNumbersAreNotRounded()
    {
        assertThat(render(PERFORMANCE, "{\"items\":[{\"irr\":0.07234567891234}]}"),
                        containsString("0.07234567891234"));
    }

    // ── narrowing ───────────────────────────────────────────────────────────

    @Test
    public void testATooLargeMetricsResponseCanBeAskedForLess()
    {
        var plan = McpEnvelope.planNarrowing(PERFORMANCE, new JsonObject()).orElseThrow();

        assertThat(plan.keep(), is(List.of("valuation", "gains")));
        assertThat(plan.dropped(), is(List.of("income", "expenses", "moneyWeighted", "timeWeighted", "risk")));
    }

    /** narrowing a selection the caller made would answer a different question */
    @Test
    public void testAChosenMetricsSelectionIsNeverNarrowed()
    {
        var arguments = JsonParser.parseString("{\"metrics\":[\"risk\"]}").getAsJsonObject();

        assertThat(McpEnvelope.planNarrowing(PERFORMANCE, arguments).isPresent(), is(false));
    }

    /** one group sent as a single value is still a choice the caller made */
    @Test
    public void testASingleMetricsValueIsStillAChosenSelection()
    {
        var arguments = JsonParser.parseString("{\"metrics\":\"risk\"}").getAsJsonObject();

        assertThat(McpEnvelope.planNarrowing(PERFORMANCE, arguments).isPresent(), is(false));
    }

    /** an empty selection names nothing, so narrowing is still the way out */
    @Test
    public void testAnEmptyMetricsSelectionLeavesNarrowingOnTheTable()
    {
        var arguments = JsonParser.parseString("{\"metrics\":[]}").getAsJsonObject();

        assertThat(McpEnvelope.planNarrowing(PERFORMANCE, arguments).isPresent(), is(true));
    }

    @Test
    public void testAToolWithoutMetricsCannotBeNarrowed()
    {
        assertThat(McpEnvelope.planNarrowing(HOLDINGS, new JsonObject()).isPresent(), is(false));
    }

    @Test
    public void testWhatWasDroppedIsNamedAsMissingRatherThanZero()
    {
        var rendered = McpEnvelope.render(PERFORMANCE, new JsonObject(), //$NON-NLS-1$
                        JsonParser.parseString("{\"items\":[]}"), List.of("risk"));

        assertThat(rendered, containsString("Narrowed to fit"));
        assertThat(rendered, containsString("absent, so they are not necessarily zero"));
        assertThat(rendered, containsString("risk"));
    }

    /** an annualized rate is a fraction too; reading it as a percentage is off by 100 */
    @Test
    public void testAnAnnualizedRateIsTreatedAsAFraction()
    {
        assertThat(render(PERFORMANCE, "{\"timeWeighted\":{\"ttwrorAnnualized\":0.0723}}"),
                        containsString("fractions, not percentages"));
    }

    // ── the cap ─────────────────────────────────────────────────────────────

    @Test
    public void testTruncationIsNeverSilent()
    {
        var capped = McpEnvelope.cap("x".repeat(5000), 400);

        assertThat(capped.length(), is(lessThanOrEqualTo(400)));
        assertThat(capped, containsString("TRUNCATED"));
        // no whole item fit, so the fragment says so rather than looking valid
        assertThat(capped, containsString("not parseable JSON"));
    }

    /** the cut lands after a whole item and closes what was open, so what survives still parses */
    @Test
    public void testTruncationEndsAtTheLastCompleteItem()
    {
        var items = new StringBuilder();
        for (int ii = 0; ii < 40; ii++)
        {
            if (ii > 0)
                items.append(",");
            items.append("{\"name\":\"item ").append(ii).append("\"}");
        }
        var payload = "{\"items\":[" + items + "]}";

        var capped = McpEnvelope.cap(payload, 500);
        var kept = capped.substring(0, capped.indexOf("\n\n[TRUNCATED"));

        assertThat(capped.length(), is(lessThanOrEqualTo(500)));
        assertThat(capped, containsString("last complete item"));
        assertThat(JsonParser.parseString(kept).isJsonObject(), is(true));
        assertThat(JsonParser.parseString(kept).getAsJsonObject().getAsJsonArray("items").size(), is(lessThan(40)));
    }

    /** a cut that splits a surrogate pair would emit a character no one can read */
    @Test
    public void testTruncationNeverSplitsASurrogatePair()
    {
        var payload = "x".repeat(107) + "\uD83D\uDE00" + "y".repeat(300);
        var capped = McpEnvelope.cap(payload, 400);

        assertThat(capped.length(), is(lessThanOrEqualTo(400)));

        for (var ii = 0; ii < capped.length(); ii++)
        {
            if (Character.isHighSurrogate(capped.charAt(ii)))
                assertThat("lone high surrogate at " + ii, Character.isLowSurrogate(capped.charAt(ii + 1)), is(true));
        }
    }

    /**
     * A cut lands between items, never between the fields of one item. A JSON
     * object closed early is still parseable, so a cut that ends mid-item produces
     * figures missing a field while the marker claims every item above is complete.
     */
    @Test
    public void testACutNeverEndsMidItem()
    {
        var items = new StringBuilder();
        for (int ii = 0; ii < 60; ii++)
        {
            if (ii > 0)
                items.append(",");
            items.append("{\"uuid\":\"00000000-0000-0000-0000-000000000000\",\"name\":\"instrument ").append(ii)
                    .append("\",\"ttwror\":0.1177,\"currencyGains\":0,\"shares\":12.5}");
        }

        var capped = McpEnvelope.cap("[" + items + "]", 900);

        assertThat(capped.length(), is(lessThanOrEqualTo(900)));
        assertThat(capped, containsString("cut after the last complete item"));

        var kept = capped.substring(0, capped.indexOf("\n\n[TRUNCATED"));
        var parsed = JsonParser.parseString(kept).getAsJsonArray();
        assertThat(parsed.size(), is(greaterThan(0)));

        for (var item : parsed)
            assertThat("item cut mid-field", item.getAsJsonObject().size(), is(5));
    }

    /**
     * A report that opens with an array and continues with fields: a cut may end
     * after a whole item of the array, which closes the array and the object above
     * it, but it must never end between two fields of one object - there the
     * fragment would parse while sitting half-written.
     */
    @Test
    public void testACutNeverEndsBetweenTheFieldsOfAnObject()
    {
        var payload = "{\"items\":[{\"uuid\":\"00000000-0000-0000-0000-000000000000\",\"name\":\"ACME\"}],"
                        + "\"totals\":{\"openingValue\":394352.02,\"unrealizedGains\":40130.96,"
                        + "\"realizedGains\":6006.82,\"income\":2324.95,\"fees\":151.53,\"taxes\":731.94,"
                        + "\"currencyGains\":120.10,\"netDeposits\":25000.00,\"dividends\":1990.00,"
                        + "\"interest\":334.95,\"transferCreditor\":1200.00,\"transferDebtor\":800.00,"
                        + "\"rewards\":45.00,\"closingValue\":426864.67}}";

        assertThat(payload.length(), is(greaterThan(340)));

        var capped = McpEnvelope.cap(payload, 340);
        assertThat(capped.length(), is(lessThanOrEqualTo(340)));
        assertThat(capped, containsString("TRUNCATED"));

        if (capped.contains("not parseable JSON"))
            return; // a fragment that says it is broken is honest about being broken

        var kept = capped.substring(0, capped.indexOf("\n\n[TRUNCATED"));
        var parsed = JsonParser.parseString(kept).getAsJsonObject();

        assertThat(parsed.getAsJsonArray("items").size(), is(greaterThan(0)));
        assertThat("totals cut mid-fields", parsed.has("totals"), is(false));
    }

    /**
     * A report that is one object has no item boundary to cut at. It gets the
     * marker that says the fragment is not parseable, rather than one that claims
     * whole items - the claim is the thing a model cannot recover from.
     */
    @Test
    public void testAnObjectPayloadIsMarkedUnparseable()
    {
        var payload = "{\"breakdown\":{\"openingValue\":394352.02,\"unrealizedGains\":40130.96,"
                        + "\"realizedGains\":6006.82,\"income\":2324.95,\"fees\":151.53,\"taxes\":731.94,"
                        + "\"currencyGains\":120.10,\"netDeposits\":25000.00,\"dividends\":1990.00,"
                        + "\"interest\":334.95,\"closingValue\":426864.67}}";

        assertThat(payload.length(), is(greaterThan(230)));

        var capped = McpEnvelope.cap(payload, 230);

        assertThat(capped.length(), is(lessThanOrEqualTo(230)));
        assertThat(capped, containsString("not parseable JSON"));
    }

    /**
     * The guidance is reserved from the budget before the payload is cut. Losing
     * it is the failure mode a truncation must not have: the notes are what tell
     * a model that the figures are fractions and that cash counts as a holding.
     */
    @Test
    public void testTruncationKeepsTheNotesAndTheAdvice()
    {
        var items = new StringBuilder();
        for (int ii = 0; ii < 200; ii++)
        {
            if (ii > 0)
                items.append(",");
            items.append("{\"name\":\"item ").append(ii).append("\",\"weight\":0.005}");
        }

        var rendered = McpEnvelope.render(PERFORMANCE, new JsonObject(), //$NON-NLS-1$
                        JsonParser.parseString("{\"items\":[" + items + "]}"), List.of("risk"),
                        "Ask a narrower question.", 600);

        assertThat(rendered.length(), is(lessThanOrEqualTo(600)));
        assertThat(rendered, containsString("fractions, not percentages"));
        assertThat(rendered, containsString("not necessarily zero"));
        assertThat(rendered, containsString("Ask a narrower question."));
        assertThat(rendered, containsString("TRUNCATED"));
    }

    /** when even the marker would not fit, the guidance still says nothing fit */
    @Test
    public void testACapTooSmallForThePayloadStillSaysSo()
    {
        var rendered = McpEnvelope.render(PERFORMANCE, new JsonObject(), //$NON-NLS-1$
                        JsonParser.parseString("{\"items\":[]}"), List.of(), "Ask a narrower question.", 20);

        assertThat(rendered, containsString("Not even one figure fit"));
        assertThat(rendered, containsString("Ask a narrower question."));
    }

    @Test
    public void testAFittingPayloadIsUntouched()
    {
        assertThat(McpEnvelope.cap("short", 400), is("short"));
    }

    /** each tool has a different honest answer, and "try again" is not one */
    @Test
    public void testOversizeAdviceIsSpecificWhereItCanBe()
    {
        assertThat(McpEnvelope.oversizeAdvice(TRADES), containsString("pp_list_instrument_trades"));
        assertThat(McpEnvelope.oversizeAdvice(INSTRUMENTS), containsString("pp_get_instrument"));
        // the two performance reports, which the metrics narrowing does not reach
        assertThat(McpEnvelope.oversizeAdvice(PERFORMANCE), containsString("pp_get_instrument_performance"));
        assertThat(McpEnvelope.oversizeAdvice(PERFORMANCE), containsString("number of instruments"));
        assertThat(McpEnvelope.oversizeAdvice(McpTools.byName("pp_get_overall_performance").orElseThrow()),
                        containsString("cannot be narrowed further"));
        // a tool with nothing specific to say still gets the honest default
        assertThat(McpEnvelope.oversizeAdvice(McpTools.byName("pp_get_instrument").orElseThrow()),
                        containsString("narrower question"));
    }

    /**
     * Advice for a report that was already narrowed cannot name the groups it
     * already dropped: following that advice re-sends the same oversized request.
     * Only what still changes the question is left to suggest.
     */
    @Test
    public void testAdviceAfterANarrowingDoesNotRepeatTheNarrowing()
    {
        var after = McpEnvelope.oversizeAdvice(PERFORMANCE, List.of("timeWeighted", "risk"));

        assertThat(after, containsString("already re-requested without `timeWeighted` and `risk`"));
        assertThat(after, not(containsString("ask again without them")));
        assertThat(after, containsString("shorten the interval"));
        assertThat(after, containsString("pp_get_instrument_performance"));

        // without a narrowing there is nothing already dropped to name
        assertThat(McpEnvelope.oversizeAdvice(PERFORMANCE), not(containsString("already re-requested")));
    }

    /**
     * Measured: without this the cash accounts are dropped from a ranking, and
     * in a real file they can be the largest positions in it.
     */
    @Test
    public void testACashAccountIsSaidToBeAHolding()
    {
        var rendered = render(HOLDINGS,
                        "{\"items\":[{\"type\":\"instrument\",\"name\":\"ACME\",\"weight\":0.4}," //
                                        + "{\"type\":\"cash-account\",\"name\":\"Giro\",\"weight\":0.6}]}");

        assertThat(rendered, containsString("**are holdings**"));
        assertThat(rendered, containsString("can outrank every security"));
        // and the way out is naming the exclusion, not making it silently
        assertThat(rendered, containsString("leave them out and say that you did"));
    }

    @Test
    public void testAPayloadWithoutCashAccountsCarriesNoSuchRule()
    {
        assertThat(render(HOLDINGS, "{\"items\":[{\"type\":\"instrument\",\"name\":\"ACME\",\"weight\":1}]}"),
                        not(containsString("are holdings")));
        // the word is not a type here, so nothing fires
        assertThat(render(INSTRUMENTS, "{\"items\":[{\"name\":\"cash-account\"}]}"),
                        not(containsString("are holdings")));
    }

    private static String render(McpTool tool, String payload)
    {
        return renderWith(tool, "{}", payload);
    }

    private static String renderWith(McpTool tool, String arguments, String payload)
    {
        return McpEnvelope.render(tool, JsonParser.parseString(arguments).getAsJsonObject(), //$NON-NLS-1$
                        JsonParser.parseString(payload), List.of());
    }
}
