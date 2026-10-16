package name.abuchen.portfolio.cli;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Publishes everything declared in {@link Commands}: commands, options,
 * response schemas, write operations and the known limits. Nothing here is
 * written by hand per command, so the output can't drift from the behavior.
 */
final class DescribeCommand
{
    /** What the tool can't do. Agents must not promise these to users. */
    static final List<String> KNOWN_LIMITS = List.of(
                    "Unsaved edits inside a running Portfolio Performance app can't be detected; a file open in the app may be out of date on disk.",
                    "PDF import is not available.",
                    "Prices can't be written: there are no price writes, and quotes are neither fetched nor edited.");

    private static final Schema SCHEMA = Schema.object("Everything the tool can do.")
                    .property("pinnedUpstreamTag",
                                    Schema.string("Upstream release the bundled core is built from."))
                    .property("maxSupportedVersion",
                                    Schema.integer("Newest file version this CLI supports; newer files are refused."))
                    .property("limits", Schema.array("Known limits of the tool.", Schema.string(null)))
                    .property("commands", Schema.array("Every declared command.", Schema.object(null)
                                    .property("name", Schema.string("Command name."))
                                    .property("summary", Schema.string("What the command returns."))
                                    .property("usesClientFile",
                                                    Schema.bool("Whether the command reads a client file."))
                                    .property("options", Schema.array("Options the command accepts.",
                                                    Schema.object(null).property("name", Schema.string(null))
                                                                    .property("valueName", Schema.string(null))
                                                                    .property("description", Schema.string(null))))
                                    .property("response", Schema.object("Schemas of the response envelope parts.")
                                                    .property("data", jsonSchema())
                                                    .property("meta", jsonSchema()))))
                    .property("operations", Schema.array("Every declared write operation.", Schema.object(null)
                                    .property("name", Schema.string("Operation name."))
                                    .property("summary", Schema.string("What the operation does."))
                                    .property("input", jsonSchema())));

    private static final Schema META = Schema.object("Assumptions behind the response.").property("asOf",
                    Schema.string("ISO date the command ran, from the clock."));

    static final CommandSpec SPEC = new CommandSpec("describe",
                    "Commands, options, write operations, response schemas and known limits of this tool.", false,
                    List.of(), SCHEMA, META, DescribeCommand::execute);

    private DescribeCommand()
    {
    }

    private static Schema jsonSchema()
    {
        return Schema.anyObject("A JSON Schema (type, properties, required, items, enum).");
    }

    private static Response execute(CommandContext context)
    {
        var meta = new LinkedHashMap<String, Object>();
        meta.put("asOf", LocalDate.now(context.clock()).toString());

        return Response.of(render(Commands.ALL, Commands.OPERATIONS), meta);
    }

    static Map<String, Object> render(List<CommandSpec> commands, List<OperationSpec> operations)
    {
        var data = new LinkedHashMap<String, Object>();
        data.put("pinnedUpstreamTag", CoreVersion.PINNED_UPSTREAM_TAG);
        data.put("maxSupportedVersion", CoreVersion.MAX_SUPPORTED_FILE_VERSION);
        data.put("limits", KNOWN_LIMITS);
        data.put("commands", commands.stream().map(DescribeCommand::render).toList());
        data.put("operations", operations.stream().map(DescribeCommand::render).toList());
        return data;
    }

    private static Map<String, Object> render(CommandSpec command)
    {
        var json = new LinkedHashMap<String, Object>();
        json.put("name", command.name());
        json.put("summary", command.summary());
        json.put("usesClientFile", command.usesClientFile());
        json.put("options", command.options().stream().map(DescribeCommand::render).toList());

        var response = new LinkedHashMap<String, Object>();
        response.put("data", command.dataSchema().toJson());
        response.put("meta", command.metaSchema().toJson());
        json.put("response", response);
        return json;
    }

    private static Map<String, Object> render(OptionSpec option)
    {
        var json = new LinkedHashMap<String, Object>();
        json.put("name", option.name());
        json.put("valueName", option.valueName());
        json.put("description", option.description());
        return json;
    }

    private static Map<String, Object> render(OperationSpec operation)
    {
        var json = new LinkedHashMap<String, Object>();
        json.put("name", operation.name());
        json.put("summary", operation.summary());
        json.put("input", operation.input().toJson());
        return json;
    }
}
