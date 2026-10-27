package name.abuchen.portfolio.rest.internal.mcp;

import java.util.ArrayDeque;
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
     * {@code ttwrorAnnualized} belongs here too - it is the same kind of figure
     * with a different denominator, and leaving it out is how a model reads an
     * annualized 0.0723 as 0.0723 percent.
     */
    private static final Set<String> FRACTION_KEYS = Set.of("ttwror", "ttwrorAnnualized", "irr", "return", "weight",
                    "maxDrawdown", "volatility", "semiVolatility");

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
     * @param arguments
     *            the arguments of the call, needed to tell a patch that changed
     *            something from one that named nothing writable
     * @param droppedMetrics
     *            the metric groups this response is missing because it had to
     *            be narrowed to fit, or empty
     */
    public static String render(McpTool tool, JsonObject arguments, JsonElement payload, List<String> droppedMetrics)
    {
        return render(tool, arguments, payload, droppedMetrics, null, Integer.MAX_VALUE);
    }

    /**
     * Renders a result that has to fit, cutting the payload and keeping the
     * guidance that says the cut happened.
     * <p/>
     * The budget is reserved for the guidance first. Truncating the rendered body
     * whole is how a large response loses exactly the lines telling a model that
     * the figures are fractions, that cash counts as a holding and that an edit
     * is unsaved - an unmarked truncation is a confident wrong answer, and one
     * that also lost its notes is worse.
     *
     * @param advice
     *            what to ask for instead, or null; like the notes, it is cut from
     *            the budget rather than appended past it
     */
    public static String render(McpTool tool, JsonObject arguments, JsonElement payload, List<String> droppedMetrics,
                    String advice, int maxChars)
    {
        var found = scan(payload);
        var guidance = guidanceBefore(found, droppedMetrics);
        var notes = notesFor(tool, arguments, found);
        if (!notes.isEmpty())
            guidance.add(notes.stream().map(note -> "- " + note).reduce((a, b) -> a + "\n" + b).orElseThrow());
        if (advice != null && !advice.isEmpty())
            guidance.add(advice);

        // unrounded: the same portfolio must not read differently through curl
        var body = payload.isJsonNull()
                        ? "Portfolio Performance accepted the call and returned nothing to show for it."
                        : payload.toString();

        var rendered = String.join("\n\n", guidance) + "\n\n" + body;
        if (rendered.length() <= maxChars)
            return rendered;

        // The payload is the only part that can be cut. Everything else in this
        // result is what the cut means.
        var budget = maxChars - String.join("\n\n", guidance).length() - 2;
        if (budget < ITEM_CUT_MARKER.length())
            return String.join("\n\n", guidance) + "\n\n" + NOTHING_FITS;

        return String.join("\n\n", guidance) + "\n\n" + cap(body, budget);
    }

    /** what changes what the payload means: warnings, unconverted money, what was dropped */
    private static List<String> guidanceBefore(Scan found, List<String> droppedMetrics)
    {
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
            // "missing" alone reads as zero. These groups were never asked for in
            // this response, so their figures are unknown, not zero.
            sections.add("**Narrowed to fit.** The full response was too large to return, so it was re-requested with "
                            + "only `" + String.join(",", NARROWED_METRICS) + "`. These groups are **absent, so they "
                            + "are not necessarily zero**: `" + String.join(", ", droppedMetrics) + "`. Ask again with "
                            + "an explicit `metrics` selection if you need one of them.");
        }

        return sections;
    }

    /** guidance that applies to this payload, in the order it should be read */
    private static List<String> notesFor(McpTool tool, JsonObject arguments, Scan found)
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
            if ("PATCH".equals(tool.method()) && !namedWritableField(tool, arguments))
            {
                // an empty merge patch changes nothing and marks nothing dirty, so
                // claiming an edit that never happened is its own kind of lie
                notes.add("Nothing changed: this patch named no field the API can write, so the file was **not** "
                        + "marked dirty and there is nothing for the user to save.");
            }
            else
            {
                // every time: a lost edit is the worst outcome available here
                notes.add("**This change is not saved to disk.** It has changed the file open in Portfolio "
                        + "Performance and marked it dirty, exactly as if the user had typed it. There is no save "
                        + "operation available here — tell the user to save in the application, or to close without "
                        + "saving to discard it.");
            }
        }

        return notes;
    }

    /** whether a patch named at least one field of the body, which is what marks the file dirty */
    private static boolean namedWritableField(McpTool tool, JsonObject arguments)
    {
        return arguments.entrySet().stream().anyMatch(entry -> {
            var binding = tool.binding(entry.getKey());
            return binding != null && binding.where() == McpTool.Where.BODY;
        });
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
     * <p/>
     * A selection counts whether it arrived as a list or as a single value - a
     * client that sent one group some other way still asked for that group, and
     * overwriting it would answer something else. An empty selection names
     * nothing, so narrowing is still on the table.
     */
    public static Optional<Narrowing> planNarrowing(McpTool tool, JsonObject arguments)
    {
        if (tool.bindings().stream().noneMatch(binding -> "metrics".equals(binding.wire())))
            return Optional.empty();

        var chosen = arguments.get("metrics");
        if (chosen != null && !chosen.isJsonNull() && namesSomething(chosen))
            return Optional.empty();

        var dropped = new LinkedHashSet<>(METRIC_GROUPS);
        dropped.removeAll(NARROWED_METRICS);
        return Optional.of(new Narrowing(NARROWED_METRICS, List.copyOf(dropped)));
    }

    /** whether a metrics argument names at least one group */
    private static boolean namesSomething(JsonElement value)
    {
        if (value.isJsonArray())
            return !value.getAsJsonArray().isEmpty();

        return value.isJsonPrimitive() && !value.getAsString().isBlank();
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
                            + "`status: [\"closed\"]` or `[\"open\"]` alone if you did not already, and use "
                            + "pp_list_instrument_trades for one instrument at a time - a whole file of matched trades "
                            + "is never going to fit in one answer.";
            case "pp_list_instruments" -> "This file has more instruments than fit in one answer. Ask about specific "
                            + "instruments with pp_get_instrument, or ask the user which part of the portfolio they "
                            + "mean.";
            case "pp_list_holdings" -> "This portfolio has more positions than fit in one answer. Ask the user which "
                            + "part of it they mean, and use pp_get_instrument for individual positions.";
            case "pp_list_instrument_performance" -> "This report has more instruments than fit in one answer. What "
                            + "dominates the size is the number of instruments, not the metric groups - each instrument "
                            + "carries a handful of figures whatever is asked for: shorten the interval, or use "
                            + "pp_get_instrument_performance for one instrument at a time.";
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
     * Truncates an oversized payload and says so - an unmarked truncation would
     * be read as complete.
     * <p/>
     * The cut lands after the last complete item that fits, where one does, so
     * what survives is still parseable JSON rather than a fragment that ends
     * mid-value. Where no whole item fits, the marker says plainly that the
     * fragment is not parseable, which is the one claim a model cannot recover
     * from on its own. Never larger than {@code maxChars} once a marker fits;
     * below that, the marker alone is what comes back.
     */
    public static String cap(String payload, int maxChars)
    {
        if (payload.length() <= maxChars)
            return payload;

        var budget = Math.max(0, maxChars - ITEM_CUT_MARKER.length());
        var cut = lastCompleteItem(payload, budget);

        if (cut.index() <= 0)
        {
            if (maxChars < PARTIAL_CUT_MARKER.length())
                // too small to say why the fragment is broken; the shortest marker
                // is the only thing that can still be read as a truncation
                return NOTHING_FITS;

            return cutAtCharacterBoundary(payload, budget) + PARTIAL_CUT_MARKER;
        }

        return payload.substring(0, cut.index()) + cut.closings() + ITEM_CUT_MARKER;
    }

    /** where a truncated payload ends: an index, plus the brackets closing what is still open */
    private record Cut(int index, String closings)
    {
    }

    /**
     * The end of the last complete item at or before {@code budget}, with the
     * brackets that close the containers still open there. Scans the compact JSON
     * as text: a comma ends an item, a closing bracket ends a container, and
     * neither counts inside a string.
     * <p/>
     * A boundary counts only where the innermost open container is an array. A
     * comma between two fields of one object, or the closing brace of an object
     * nested inside an item, would produce parseable JSON that ends mid-item - and
     * a marker claiming "after the last complete item" on a half-written item is
     * the one thing this function must not say.
     */
    private static Cut lastCompleteItem(String json, int budget)
    {
        var open = new ArrayDeque<Character>();
        var cut = new Cut(-1, "");
        var inString = false;
        var escaped = false;

        for (var ii = 0; ii < json.length(); ii++)
        {
            var character = json.charAt(ii);

            if (inString)
            {
                if (escaped)
                    escaped = false;
                else if (character == '\\')
                    escaped = true;
                else if (character == '"')
                    inString = false;
                continue;
            }

            if (character == '"')
            {
                inString = true;
            }
            else if (character == '{' || character == '[')
            {
                open.push(character);
            }
            else if (character == '}' || character == ']')
            {
                open.pop();
                if (endsArrayElement(open))
                {
                    var candidate = new Cut(ii + 1, closingsOf(open));
                    if (candidate.index() + candidate.closings().length() <= budget)
                        cut = candidate;
                }
            }
            else if (character == ',' && endsArrayElement(open))
            {
                var candidate = new Cut(ii, closingsOf(open));
                if (candidate.index() + candidate.closings().length() <= budget)
                    cut = candidate;
            }

            if (ii >= budget)
                break;
        }

        return cut;
    }

    /** whether the containers still open end in an array, which is where an item boundary is */
    private static boolean endsArrayElement(ArrayDeque<Character> open)
    {
        return !open.isEmpty() && open.peek() == '[';
    }

    /** the brackets that close the containers still open, innermost first */
    private static String closingsOf(ArrayDeque<Character> open)
    {
        var closing = new StringBuilder();
        for (var character : open)
            closing.append(character == '{' ? '}' : ']');
        return closing.toString();
    }

    /** a cut that splits a surrogate pair would emit a character no one can read */
    private static String cutAtCharacterBoundary(String json, int budget)
    {
        var index = Math.min(budget, json.length());
        if (index > 0 && index < json.length() && Character.isHighSurrogate(json.charAt(index - 1))
                        && Character.isLowSurrogate(json.charAt(index)))
            index--;

        return json.substring(0, index);
    }

    /** the marker for a cut that landed after a whole item - the longer of the two */
    private static final String ITEM_CUT_MARKER = "\n\n[TRUNCATED. The full answer did not fit in one tool result. "
                    + "This is cut after the last complete item that fit; every item after it is absent. Do not "
                    + "summarise what is above as if it were the whole portfolio — say what was cut, and ask a "
                    + "narrower question.]";

    /** the marker for a payload too large for even one item to fit */
    private static final String PARTIAL_CUT_MARKER = "\n\n[TRUNCATED. The full answer did not fit in one tool result, "
                    + "and not even one item fit: this ends partway through a value, so it is not parseable JSON. "
                    + "Nothing above is complete — say so, and ask a narrower question.]";

    /** what the guidance says when the budget left after it is smaller than a marker */
    private static final String NOTHING_FITS = "[TRUNCATED. Not even one figure fit in one tool result — ask a "
                    + "narrower question.]";
}
