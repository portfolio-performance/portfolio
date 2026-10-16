package name.abuchen.portfolio.cli;

import java.util.List;
import java.util.OptionalInt;

/** The page requested by <code>--limit</code> and <code>--offset</code>. */
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

    /** One page of a list, and where the next one starts. */
    record Page<T>(List<T> items, int total, OptionalInt nextOffset)
    {
    }

    static Paging parse(CommandContext context) throws CliException
    {
        var limit = LIMIT.parseInteger(context.option(LIMIT.name()));
        var offset = OFFSET.parseInteger(context.option(OFFSET.name()));
        return new Paging((int) limit, offset);
    }

    /** Cuts this page out of the complete, already ordered list. */
    <T> Page<T> of(List<T> ordered)
    {
        var total = ordered.size();
        if (offset >= total)
            return new Page<>(List.of(), total, OptionalInt.empty());

        var start = (int) offset;
        var end = Math.min(total, start + limit);
        return new Page<>(ordered.subList(start, end), total,
                        end < total ? OptionalInt.of(end) : OptionalInt.empty());
    }
}
