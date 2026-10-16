package name.abuchen.portfolio.cli;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import name.abuchen.portfolio.model.Client;

/**
 * Paging shared by every list command: <code>--limit</code> and
 * <code>--offset</code> in, <code>{items, total, nextOffset}</code> out.
 */
record Paging(int limit, long offset)
{
    static final int DEFAULT_LIMIT = 200;
    static final int MAX_LIMIT = 1000;

    static final OptionSpec LIMIT = OptionSpec.integer("limit", "n", "Maximum number of items to return.",
                    DEFAULT_LIMIT, 1, (long) MAX_LIMIT);

    static final OptionSpec OFFSET = OptionSpec.integer("offset", "n",
                    "Number of items to skip. Use nextOffset of the previous page to continue.", 0, 0, null);

    /** Both paging options, to declare on a list command. */
    static final List<OptionSpec> OPTIONS = List.of(LIMIT, OFFSET);

    /** The <code>data</code> schema of a list of items. */
    static Schema dataSchema(String description, Schema item)
    {
        return Schema.object(description).property("items", Schema.array("The items of this page.", item))
                        .property("total", Schema.integer("Number of items in the whole list."))
                        .optionalProperty("nextOffset",
                                        Schema.integer("Offset of the next page. Absent on the last page."));
    }

    static Paging parse(CommandContext context) throws CliException
    {
        var limit = parseInteger(context, LIMIT);
        var offset = parseInteger(context, OFFSET);
        return new Paging((int) limit, offset);
    }

    private static long parseInteger(CommandContext context, OptionSpec option) throws CliException
    {
        var raw = context.option(option.name());
        if (raw.isEmpty())
            return (Long) option.defaultValue();

        long value;
        try
        {
            value = Long.parseLong(raw.get());
        }
        catch (NumberFormatException e)
        {
            throw invalid(option, raw.get());
        }

        if (value < option.minimum() || (option.maximum() != null && value > option.maximum()))
            throw invalid(option, raw.get());

        return value;
    }

    private static CliException invalid(OptionSpec option, String raw)
    {
        var range = option.maximum() == null ? "at least " + option.minimum()
                        : "between " + option.minimum() + " and " + option.maximum();
        return CliException.usage("invalid-option-value", "Invalid option value",
                        "Option '--" + option.name() + "' must be an integer " + range + ".")
                        .with("option", option.name()).with("value", raw);
    }

    /**
     * Sorts the entities, renders only the requested page and wraps it in the
     * list response.
     */
    static <T> Response respond(CommandContext context, Client client, Paging paging, List<T> entities,
                    Comparator<T> ordering, Function<T, Map<String, Object>> renderer)
    {
        var sorted = entities.stream().sorted(ordering).toList();
        var total = sorted.size();

        var data = new LinkedHashMap<String, Object>();
        if (paging.offset() >= total)
        {
            data.put("items", List.of());
            data.put("total", total);
        }
        else
        {
            var start = (int) paging.offset();
            var end = Math.min(total, start + paging.limit());
            data.put("items", sorted.subList(start, end).stream().map(renderer).toList());
            data.put("total", total);
            if (end < total)
                data.put("nextOffset", end);
        }

        return Response.of(data, ResponseMeta.paged(context, client, paging));
    }
}
