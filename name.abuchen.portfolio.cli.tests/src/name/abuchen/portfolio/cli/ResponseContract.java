package name.abuchen.portfolio.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.cli.command.OutputFormat;
import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.cli.command.WarningSpec;

/**
 * What the per-command help promises about a successful response: an envelope
 * of <code>data</code>, <code>meta</code> and <code>warnings</code>, whose data
 * and meta conform to the command's declared schemas and whose warnings carry
 * only declared codes. {@link CliRun} checks every run against it, so each
 * test of the suite also proves that the help tells the truth.
 */
final class ResponseContract
{
    private static final Set<String> ENVELOPE = Set.of("data", "meta", "warnings");

    private ResponseContract()
    {
    }

    /**
     * @return one message per broken promise; empty if the run conforms or
     *         promised no JSON response (a failure, help or a table)
     */
    static List<String> violations(List<String> args, int exitCode, String stdout)
    {
        if (exitCode != 0 || args.isEmpty() || args.contains(HelpText.OPTION) || isTable(args))
            return List.of();

        var command = Commands.find(args.get(0));
        if (command.isEmpty())
            return List.of();

        JsonElement json;
        try
        {
            json = JsonParser.parseString(stdout);
        }
        catch (JsonParseException e)
        {
            return List.of("stdout is not a JSON document: " + e.getMessage());
        }

        if (!json.isJsonObject() || !json.getAsJsonObject().keySet().equals(ENVELOPE))
            return List.of("envelope must have exactly " + ENVELOPE + " but was " + json);

        var envelope = json.getAsJsonObject();
        var spec = command.get();
        var errors = new ArrayList<String>();
        SchemaValidator.validate(envelope.get("data"), json(spec.dataSchema())).forEach(e -> errors.add("data " + e));
        SchemaValidator.validate(envelope.get("meta"), json(spec.metaSchema())).forEach(e -> errors.add("meta " + e));

        var warnings = envelope.get("warnings");
        if (!warnings.isJsonArray())
        {
            errors.add("warnings must be an array but was " + warnings);
            return errors;
        }

        var declared = spec.warnings().stream().map(WarningSpec::code).toList();
        for (var warning : warnings.getAsJsonArray())
        {
            if (!warning.isJsonObject() || !warning.getAsJsonObject().has("code")
                            || !warning.getAsJsonObject().has("detail"))
                errors.add("warning without code and detail: " + warning);
            else if (!declared.contains(warning.getAsJsonObject().get("code").getAsString()))
                errors.add("undeclared warning code: " + warning);
        }
        return errors;
    }

    static JsonObject json(Schema schema)
    {
        return new Gson().toJsonTree(schema.toJson()).getAsJsonObject();
    }

    /** Accepts both spellings the parser does: <code>--format table</code> and <code>--format=table</code>. */
    private static boolean isTable(List<String> args)
    {
        var option = "--" + OutputFormat.OPTION_NAME;
        var table = OutputFormat.TABLE.id();
        var format = args.indexOf(option);
        return args.contains(option + "=" + table)
                        || format >= 0 && format + 1 < args.size() && args.get(format + 1).equals(table);
    }
}
