package name.abuchen.portfolio.cli.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * Declares a command once: its parsing, dispatch and its help all derive from
 * it. The data and meta schemas describe the two variable parts of the
 * response envelope; the warnings are every code the command can emit. A
 * command that declares table columns also accepts <code>--format
 * table</code>; its data must then be a page of items.
 * <p>
 * A command is built with {@link #of}: its options, meta schema, warnings and
 * error codes derive from its {@link Facet}s. The constructors are
 * transitional, for the commands not yet composed of facets.
 */
public final class CommandSpec
{
    @FunctionalInterface
    public interface CommandHandler
    {
        Response execute(CommandContext context) throws CliException;
    }

    /** The handler of a command composed of facets: it returns the data only. */
    @FunctionalInterface
    public interface Handler
    {
        Object execute(CommandContext run) throws CliException;
    }

    private final String name;
    private final String summary;
    private final boolean usesClientFile;
    private final List<OptionSpec> declaredOptions;
    private final Schema dataSchema;
    private final Schema metaSchema;
    private final List<WarningSpec> warnings;
    private final List<TableColumn> tableColumns;
    private final CommandHandler handler;
    /** Null for a command not composed of facets. */
    private final List<Facet<?>> facets;

    public CommandSpec(String name, String summary, boolean usesClientFile, List<OptionSpec> declaredOptions,
                    Schema dataSchema, Schema metaSchema, List<WarningSpec> warnings, List<TableColumn> tableColumns,
                    CommandHandler handler)
    {
        this(name, summary, usesClientFile, declaredOptions, dataSchema, metaSchema, warnings, tableColumns, handler,
                        null);
    }

    /** A command with warnings that can only answer in JSON. */
    public CommandSpec(String name, String summary, boolean usesClientFile, List<OptionSpec> declaredOptions,
                    Schema dataSchema, Schema metaSchema, List<WarningSpec> warnings, CommandHandler handler)
    {
        this(name, summary, usesClientFile, declaredOptions, dataSchema, metaSchema, warnings, List.of(), handler);
    }

    /** A command that emits no warnings and can only answer in JSON. */
    public CommandSpec(String name, String summary, boolean usesClientFile, List<OptionSpec> declaredOptions,
                    Schema dataSchema, Schema metaSchema, CommandHandler handler)
    {
        this(name, summary, usesClientFile, declaredOptions, dataSchema, metaSchema, List.of(), handler);
    }

    private CommandSpec(String name, String summary, boolean usesClientFile, List<OptionSpec> declaredOptions,
                    Schema dataSchema, Schema metaSchema, List<WarningSpec> warnings, List<TableColumn> tableColumns,
                    CommandHandler handler, List<Facet<?>> facets)
    {
        this.name = name;
        this.summary = summary;
        this.usesClientFile = usesClientFile;
        this.declaredOptions = List.copyOf(declaredOptions);
        this.dataSchema = dataSchema;
        this.metaSchema = metaSchema;
        this.warnings = List.copyOf(warnings);
        this.tableColumns = List.copyOf(tableColumns);
        this.handler = handler;
        this.facets = facets;
    }

    /**
     * A command composed of facets. Its options are the facets' options in
     * facet order; its meta is the envelope, then paging, then sorting, then
     * the other facets' members in facet order.
     *
     * @throws IllegalStateException
     *             if two facets declare the same option or meta member, or
     *             more than one facet declares the client file
     */
    public static CommandSpec of(String name, String summary, Schema dataSchema, Handler handler, Facet<?>... facets)
    {
        var list = List.of(facets);
        rejectConflicts(name, list);

        var options = new ArrayList<OptionSpec>();
        var warnings = new ArrayList<WarningSpec>();
        for (var facet : list)
        {
            facet.declaration().options().stream().filter(o -> !o.name().equals(OptionSpec.FILE.name()))
                            .forEach(options::add);
            warnings.addAll(facet.declaration().warnings());
        }

        var meta = Schema.object("Assumptions behind the response.");
        reportOrder(list).forEach(facet -> facet.declaration().addTo(meta));

        var usesFile = list.stream().anyMatch(f -> f.declaration().declares(OptionSpec.FILE));

        var spec = new CommandSpec[1];
        spec[0] = new CommandSpec(name, summary, usesFile, options, dataSchema, meta, warnings, List.of(),
                        context -> spec[0].run(context, handler), list);
        return spec[0];
    }

    /** The same command, which also writes its page of items as a table with <code>--format table</code>. */
    public CommandSpec withTable(List<TableColumn> columns)
    {
        if (facets == null)
            throw new IllegalStateException("Only a command composed of facets takes table columns this way");
        return new CommandSpec(name, summary, usesClientFile, declaredOptions, dataSchema, metaSchema, warnings,
                        columns, handler, facets);
    }

    private static void rejectConflicts(String name, List<Facet<?>> facets)
    {
        var options = new HashSet<String>();
        var members = new HashSet<String>();
        var envelopes = 0;
        for (var facet : facets)
        {
            var declaration = facet.declaration();
            for (var option : declaration.options())
            {
                if (!options.add(option.name()))
                    throw new IllegalStateException(
                                    "Command '" + name + "': two facets declare the option '--" + option.name() + "'");
            }
            for (var member : declaration.meta())
            {
                if (!members.add(member.name()))
                    throw new IllegalStateException(
                                    "Command '" + name + "': two facets declare the meta member '" + member.name()
                                                    + "'");
            }
            if (declaration.slot() == Declaration.Slot.ENVELOPE && ++envelopes > 1)
                throw new IllegalStateException("Command '" + name + "': more than one client file facet");
        }
    }

    private static List<Facet<?>> reportOrder(List<Facet<?>> facets)
    {
        return facets.stream().sorted(Comparator.comparing((Facet<?> f) -> f.declaration().slot())).toList();
    }

    /** Resolves the facets, runs the handler and builds the response from the data and the facets' reports. */
    private Response run(CommandContext context, Handler body) throws CliException
    {
        context.bind(facets);
        context.checkValues(declaredOptions);

        for (var facet : facets)
        {
            if (!facet.declaration().resolvesWithClient())
                context.resolve(facet);
        }
        for (var facet : facets)
        {
            if (facet.declaration().resolvesWithClient())
                context.resolve(facet);
        }

        var data = body.execute(context);

        var meta = new LinkedHashMap<String, Object>();
        var response = Response.of(data, meta);
        context.handlerWarnings().forEach(response::warn);
        for (var facet : reportOrder(facets))
            report(context, facet, meta, response);
        return response;
    }

    private static <T> void report(CommandContext context, Facet<T> facet, LinkedHashMap<String, Object> meta,
                    Response response)
    {
        var declaration = facet.declaration();
        facet.report(context.get(facet), new Facet.Report()
        {
            @Override
            public void meta(String member, Object value)
            {
                if (!declaration.declaresMember(member))
                    throw new IllegalStateException("A facet emitted the undeclared meta member '" + member + "'");
                meta.put(member, value);
            }

            @Override
            public void warn(Warning warning)
            {
                if (!declaration.declaresWarning(warning.code()))
                    throw new IllegalStateException("A facet emitted the undeclared warning " + warning.code());
                response.warn(warning);
            }
        });

        for (var member : declaration.meta())
        {
            if (member.required() && !meta.containsKey(member.name()))
                throw new IllegalStateException("A facet left out its required meta member '" + member.name() + "'");
        }
    }

    public String name()
    {
        return name;
    }

    public String summary()
    {
        return summary;
    }

    /** Transitional: whether the command reads a client file. */
    public boolean usesClientFile()
    {
        return usesClientFile;
    }

    /** The command's own options, without the shared ones. */
    public List<OptionSpec> declaredOptions()
    {
        return declaredOptions;
    }

    public Schema dataSchema()
    {
        return dataSchema;
    }

    public Schema metaSchema()
    {
        return metaSchema;
    }

    public List<WarningSpec> warnings()
    {
        return warnings;
    }

    public List<TableColumn> tableColumns()
    {
        return tableColumns;
    }

    public CommandHandler handler()
    {
        return handler;
    }

    /**
     * The codes the command can fail with, in declaration order. For a command
     * composed of facets: the codes of every command, those implied by its
     * option types and those its facets declare.
     */
    public List<ErrorCode> errorCodes()
    {
        if (facets == null)
            return Arrays.stream(ErrorCode.values()).filter(e -> e.appliesTo(this)).toList();

        var codes = new HashSet<ErrorCode>();
        facets.forEach(f -> codes.addAll(f.declaration().codes()));
        for (var option : options())
        {
            if (option.type() instanceof OptionSpec.Reference)
                codes.addAll(Set.of(ErrorCode.REFERENCE_NOT_FOUND, ErrorCode.REFERENCE_AMBIGUOUS));
            if (option.required())
                codes.add(ErrorCode.OPTION_REQUIRED);
        }
        return Arrays.stream(ErrorCode.values())
                        .filter(e -> e.scope() == ErrorCode.Scope.EVERY_COMMAND || codes.contains(e)).toList();
    }

    /** The global options this command accepts besides its declared ones. */
    public List<OptionSpec> sharedOptions()
    {
        var options = new ArrayList<OptionSpec>();
        if (usesClientFile)
            options.add(OptionSpec.FILE);
        options.add(OutputFormat.option(!tableColumns.isEmpty()));
        options.add(OptionSpec.VERBOSE);
        return options;
    }

    /** All options the parser accepts, including the shared ones. */
    public List<OptionSpec> options()
    {
        var options = new ArrayList<OptionSpec>(declaredOptions);
        options.addAll(sharedOptions());
        return options;
    }

    public boolean accepts(String optionName)
    {
        return options().stream().anyMatch(o -> o.name().equals(optionName));
    }

    /** The accepted option of that name. */
    public OptionSpec option(String optionName)
    {
        return options().stream().filter(o -> o.name().equals(optionName)).findFirst().orElseThrow();
    }
}
