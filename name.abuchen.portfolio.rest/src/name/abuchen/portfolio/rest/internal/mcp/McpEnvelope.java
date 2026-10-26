package name.abuchen.portfolio.rest.internal.mcp;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The result envelope: payload, plus the guidance that response actually needs.
 * <p/>
 * Half the domain rules live here rather than in tool descriptions, which are
 * hashed into a Claude Desktop user's "always allow" grant. Each is conditional
 * on the payload - a response with no rates gets no fractions reminder - so
 * that every line of a result is load-bearing rather than a standing footer
 * that gets skimmed.
 */
@SuppressWarnings("nls")
public final class McpEnvelope
{
    /**
     * Keys whose values are fractions, not percentages. Taken from the fields
     * the specification documents as such: a {@code ttwror} of 0.0723 is 7.23%.
     */
    private static final Set<String> FRACTION_KEYS = Set.of("ttwror", "irr", "return", "weight", "maxDrawdown",
                    "volatility", "semiVolatility");

    /** offset-less local date-times, which must not be read as UTC */
    private static final Set<String> LOCAL_DATETIME_KEYS = Set.of("start", "end");

    /** the holding whose being a holding is not obvious; the value of a holding's {@code type} */
    private static final String CASH_ACCOUNT_TYPE = "cash-account";

    /** the seven groups {@code metrics=} selects from */
    public static final List<String> METRIC_GROUPS = List.of("valuation", "gains", "income", "expenses",
                    "moneyWeighted", "timeWeighted", "risk");

    /**
     * What to keep when a response is too large. Fixed rather than adaptive,
     * and always named in the result: two clients asking the same question have
     * to get the same answer.
     */
    public static final List<String> NARROWED_METRICS = List.of("valuation", "gains");

    /** what a tool can be asked for instead, when the full answer will not fit */
    public record Narrowing(List<String> keep, List<String> dropped)
    {
    }

    /** everything the guidance depends on, collected in one walk of the payload */
    private record Scan(boolean hasFractions, boolean hasNulls, boolean hasLocalDateTimes, boolean hasCashAccounts,
                    JsonArray warnings, JsonArray fxWarnings)
    {
    }

    private McpEnvelope()
    {
    }

    /**
     * Renders a successful payload as the text a model reads.
     *
     * @param droppedMetrics
     *            the metric groups this response is missing because it had to
     *            be narrowed to fit, or empty
     */
    public static String render(McpTool tool, JsonElement payload, List<String> droppedMetrics)
    {
        var found = scan(payload);
        var sections = new ArrayList<String>();

        // warnings first: they change what everything below means
        if (found.warnings().size() > 0)
        {
            sections.add("**Partial result — " + found.warnings().size() + " warning"
                            + (found.warnings().size() == 1 ? "" : "s")
                            + ".** Part of this computation failed, so the figures below are incomplete. Name what was "
                            + "skipped in your answer instead of presenting the totals as if they were whole.\n"
                            + found.warnings());
        }

        // unconverted money is the second outcome that changes what every figure
        // below means, so it is stated before them, not as a footer note
        if (found.fxWarnings().size() > 0)
        {
            sections.add("**Converted at 1:1, not at an exchange rate.** " + found.fxWarnings().size()
                    + " currency pair" + (found.fxWarnings().size() == 1 ? "" : "s")
                    + " in this report have nothing to convert with, so amounts in those currencies were multiplied by "
                    + "1 and are shown at face value. Say that they are unconverted; do not present them as if the "
                    + "exchange rate had been applied. The pairs:\n" + found.fxWarnings());
        }

        if (!droppedMetrics.isEmpty())
        {
            sections.add("**Narrowed to fit.** The full response was too large to return, so it was re-requested with "
                            + "only `" + String.join(",", NARROWED_METRICS) + "`. These groups are **missing and were "
                            + "not zero**: `" + String.join(", ", droppedMetrics) + "`. Ask again with an explicit "
                            + "`metrics` selection if you need one of them.");
        }

        // unrounded: the same portfolio must not read differently through curl
        sections.add(payload.isJsonNull()
                        ? "Portfolio Performance accepted the call and returned nothing to show for it."
                        : payload.toString());

        var notes = notesFor(tool, found);
        if (!notes.isEmpty())
            sections.add(notes.stream().map(note -> "- " + note).reduce((a, b) -> a + "\n" + b).orElseThrow());

        return String.join("\n\n", sections);
    }

    /** guidance that applies to this payload, in the order it should be read */
    private static List<String> notesFor(McpTool tool, Scan found)
    {
        var notes = new ArrayList<String>();

        if (found.hasFractions())
        {
            // the most consequential misreading available
            notes.add("Rates and weights above are **fractions, not percentages** — multiply by 100 before showing "
                            + "one: `0.0723` is 7.23%.");
        }

        if (found.hasNulls())
        {
            // deliberately wider than "undefined for this interval", which is
            // false of an instrument's currencyCode, null when it is an index
            notes.add("A `null` means the value is **unknown or does not apply**, never zero — for a computed figure, "
                            + "that it is undefined over this interval. Say it is unavailable rather than reporting a "
                            + "zero.");
        }

        if (found.hasLocalDateTimes())
        {
            notes.add("The date-times carry **no timezone** and are local to the user. Do not convert them or treat "
                            + "them as UTC.");
        }

        if (found.hasCashAccounts())
        {
            // measured: without this the cash accounts get dropped from a
            // ranking, and in a real file they can be the largest positions in
            // it - which moves every rank below them, not just one line
            notes.add("The `cash-account` lines above **are holdings**. A cash balance is a position like any other "
                            + "and can outrank every security, so include them when ranking, totalling or reporting "
                            + "the largest holdings. If the user asked about securities only, leave them out and say "
                            + "that you did.");
        }

        if (!tool.isReadOnly())
        {
            // every time: a lost edit is the worst outcome available here
            notes.add("**This change is not saved to disk.** It has changed the file open in Portfolio Performance and "
                            + "marked it dirty, exactly as if the user had typed it. There is no save operation "
                            + "available here — tell the user to save in the application, or to close without saving "
                            + "to discard it.");
        }

        return notes;
    }

    private static Scan scan(JsonElement payload)
    {
        var warnings = topLevelArray(payload, "warnings"); //$NON-NLS-1$
        var fxWarnings = topLevelArray(payload, "fxWarnings"); //$NON-NLS-1$

        var flags = new boolean[4];
        visit(payload, flags);
        return new Scan(flags[0], flags[1], flags[2], flags[3], warnings, fxWarnings);
    }

    /** an always-present collection field of a report, empty when it is absent */
    private static JsonArray topLevelArray(JsonElement payload, String key)
    {
        if (!payload.isJsonObject())
            return new JsonArray();

        var value = payload.getAsJsonObject().get(key);
        return value != null && value.isJsonArray() ? value.getAsJsonArray() : new JsonArray();
    }

    /** {@code flags} is fractions, nulls, local date-times, cash accounts - in that order */
    private static void visit(JsonElement node, boolean[] flags)
    {
        if (node.isJsonArray())
        {
            node.getAsJsonArray().forEach(item -> visit(item, flags));
            return;
        }

        if (!node.isJsonObject())
            return;

        for (var entry : node.getAsJsonObject().entrySet())
        {
            var value = entry.getValue();

            if (value.isJsonNull())
            {
                flags[1] = true;
                continue;
            }

            if (value.isJsonPrimitive())
            {
                var primitive = value.getAsJsonPrimitive();
                if (primitive.isNumber() && FRACTION_KEYS.contains(entry.getKey()))
                    flags[0] = true;
                if (primitive.isString() && LOCAL_DATETIME_KEYS.contains(entry.getKey())
                                && primitive.getAsString().contains("T"))
                    flags[2] = true;
                if (primitive.isString() && "type".equals(entry.getKey())
                                && CASH_ACCOUNT_TYPE.equals(primitive.getAsString()))
                    flags[3] = true;
            }

            visit(value, flags);
        }
    }

    /**
     * Whether this tool can be asked for less, and what would be dropped.
     * Empty when the caller already chose a selection: narrowing further would
     * answer a different question than the one asked.
     */
    public static Optional<Narrowing> planNarrowing(McpTool tool, JsonObject arguments)
    {
        if (tool.bindings().stream().noneMatch(binding -> "metrics".equals(binding.wire())))
            return Optional.empty();

        var chosen = arguments.get("metrics");
        if (chosen != null && chosen.isJsonArray() && !chosen.getAsJsonArray().isEmpty())
            return Optional.empty();

        var dropped = new LinkedHashSet<>(METRIC_GROUPS);
        dropped.removeAll(NARROWED_METRICS);
        return Optional.of(new Narrowing(NARROWED_METRICS, List.copyOf(dropped)));
    }

    /**
     * What to suggest when a response will not fit and cannot be narrowed. Each
     * tool has a different honest answer, and "try again" is not one of them.
     */
    public static String oversizeAdvice(McpTool tool)
    {
        return oversizeAdvice(tool, List.of());
    }

    /**
     * The same advice for a report that was already narrowed, naming the groups
     * that were dropped on the way.
     * <p/>
     * Those groups cannot be advice any more. Telling the reader to drop what the
     * answer already dropped asks for the same oversized request again, and the
     * only constraints left are the ones that change the question: a shorter
     * interval, or one instrument instead of all of them.
     */
    public static String oversizeAdvice(McpTool tool, List<String> alreadyDropped)
    {
        if ("pp_list_instrument_performance".equals(tool.name()) && !alreadyDropped.isEmpty())
        {
            return "This report was already re-requested without " + joinedNames(alreadyDropped)
                    + ", and it still did not fit. Dropping those groups again changes nothing: shorten the interval, "
                    + "or use pp_get_instrument_performance for one instrument at a time.";
        }

        return switch (tool.name())
        {
            case "pp_list_trades" -> "This portfolio has more trades than fit in one answer. Ask for "
                            + "`status: [\"closed\"]` or `[\"open\"]` alone, or use pp_list_instrument_trades for one "
                            + "instrument at a time.";
            case "pp_list_instruments" -> "This file has more instruments than fit in one answer. Ask about specific "
                            + "instruments with pp_get_instrument, or ask the user which part of the portfolio they "
                            + "mean.";
            case "pp_list_holdings" -> "This portfolio has more positions than fit in one answer. Ask the user which "
                            + "part of it they mean, and use pp_get_instrument for individual positions.";
            case "pp_list_instrument_performance" -> "This report has more instruments than fit in one answer. The "
                            + "`timeWeighted` and `risk` groups carry a daily series per instrument and dominate the "
                            + "size: ask again without them, or use pp_get_instrument_performance for single "
                            + "instruments. A shorter interval helps too.";
            case "pp_get_overall_performance" -> "This response is larger than one answer can carry, and it cannot be "
                            + "narrowed further. Read the `breakdown` figures you need from what is above, say the "
                            + "rest was cut, and use pp_list_instrument_performance for a per-instrument view.";
            default -> "This response is larger than one answer can carry. Ask a narrower question rather than "
                            + "retrying the same one.";
        };
    }

    /** the dropped groups spelled out, the last one joined with "and" */
    private static String joinedNames(List<String> names)
    {
        var spelled = names.stream().map(name -> "`" + name + "`").toList();
        if (spelled.size() == 1)
            return spelled.get(0);

        return String.join(", ", spelled.subList(0, spelled.size() - 1)) + " and " + spelled.get(spelled.size() - 1);
    }

    /**
     * Truncates an oversized result and says so - an unmarked truncation would
     * be read as complete. A cap smaller than the marker yields the marker
     * alone, rather than a payload with nothing to say it is partial.
     */
    public static String cap(String payload, int maxChars)
    {
        if (payload.length() <= maxChars)
            return payload;

        var marker = "\n\n[TRUNCATED. The full answer did not fit in one tool result. What is above is incomplete and "
                        + "must not be summarised as if it were the whole portfolio — say so, and ask for a narrower "
                        + "question.]";
        return payload.substring(0, Math.max(0, maxChars - marker.length())) + marker;
    }
}
