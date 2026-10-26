package name.abuchen.portfolio.cli.performance;

import java.util.Arrays;
import java.util.List;

import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.cli.command.CommandContext;
import name.abuchen.portfolio.cli.command.OptionSpec;
import name.abuchen.portfolio.cli.command.Schema;

/** How capital gains are costed: the value of the <code>--cost-method</code> option. */
enum CostMethod
{
    FIFO("fifo", name.abuchen.portfolio.model.CostMethod.FIFO),
    MOVING_AVERAGE("moving-average", name.abuchen.portfolio.model.CostMethod.MOVING_AVERAGE);

    static final OptionSpec OPTION = new OptionSpec("cost-method", "method",
                    "How capital gains are costed: fifo (the default) or moving-average. Echoed in meta.");

    private final String id;
    private final name.abuchen.portfolio.model.CostMethod core;

    CostMethod(String id, name.abuchen.portfolio.model.CostMethod core)
    {
        this.id = id;
        this.core = core;
    }

    /** The name the option and <code>meta</code> have for it. */
    String id()
    {
        return id;
    }

    /** The same method in core. */
    name.abuchen.portfolio.model.CostMethod core()
    {
        return core;
    }

    static List<String> ids()
    {
        return Arrays.stream(values()).map(CostMethod::id).toList();
    }

    /** The schema of the <code>costMethod</code> member of <code>meta</code>. */
    static Schema schema()
    {
        return Schema.enumOf("The cost method used: as given, otherwise fifo.", ids());
    }

    /**
     * @throws CliException
     *             a usage error if the value is not a cost method
     */
    static CostMethod parse(CommandContext context) throws CliException
    {
        var raw = context.text(OPTION);
        if (raw.isEmpty())
            return FIFO;

        return Arrays.stream(values()).filter(m -> m.id.equals(raw.get())).findFirst()
                        .orElseThrow(() -> OPTION.invalid(raw.get(), "one of: " + String.join(", ", ids())));
    }
}
