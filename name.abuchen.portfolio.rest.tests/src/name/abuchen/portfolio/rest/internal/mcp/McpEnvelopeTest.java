package name.abuchen.portfolio.rest.internal.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
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

    @Test
    public void testAWriteAlwaysSaysItIsNotSavedToDisk()
    {
        assertThat(render(UPDATE, "{\"uuid\":\"x\"}"), containsString("not saved to disk"));
        assertThat(render(HOLDINGS, "{\"items\":[]}"), not(containsString("not saved to disk")));
    }

    @Test
    public void testANoContentResponseSaysSoRatherThanPrintingNull()
    {
        var rendered = McpEnvelope.render(UPDATE, JsonNull.INSTANCE, List.of());

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

    @Test
    public void testAToolWithoutMetricsCannotBeNarrowed()
    {
        assertThat(McpEnvelope.planNarrowing(HOLDINGS, new JsonObject()).isPresent(), is(false));
    }

    @Test
    public void testWhatWasDroppedIsNamedAsMissingRatherThanZero()
    {
        var rendered = McpEnvelope.render(PERFORMANCE, JsonParser.parseString("{\"items\":[]}"), List.of("risk"));

        assertThat(rendered, containsString("Narrowed to fit"));
        assertThat(rendered, containsString("were not zero"));
        assertThat(rendered, containsString("risk"));
    }

    // ── the cap ─────────────────────────────────────────────────────────────

    @Test
    public void testTruncationIsNeverSilent()
    {
        var capped = McpEnvelope.cap("x".repeat(5000), 400);

        assertThat(capped.length(), is(400));
        assertThat(capped, containsString("TRUNCATED"));
        assertThat(capped, containsString("must not be summarised as if it were the whole portfolio"));
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
        assertThat(McpEnvelope.oversizeAdvice(PERFORMANCE), containsString("narrower question"));
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
        return McpEnvelope.render(tool, JsonParser.parseString(payload), List.of());
    }
}
