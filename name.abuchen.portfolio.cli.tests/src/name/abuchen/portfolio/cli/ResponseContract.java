package name.abuchen.portfolio.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.cli.apply.ApplyCommand;
import name.abuchen.portfolio.cli.apply.Operations;
import name.abuchen.portfolio.cli.command.CommandSpec;
import name.abuchen.portfolio.cli.command.ErrorCode;
import name.abuchen.portfolio.cli.command.OperationSpec;
import name.abuchen.portfolio.cli.command.OutputFormat;
import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.cli.command.WarningSpec;

/**
 * What the per-command help promises about a successful response: an envelope
 * of <code>data</code>, <code>meta</code> and <code>warnings</code>, whose data
 * and meta conform to the command's declared schemas and whose warnings carry
 * only declared codes; and that a failure's code is one the command's help
 * lists. {@link CliRun} checks every run against it, so each
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
     *         promised no JSON response (help or a table)
     */
    static List<String> violations(List<String> args, int exitCode, String stdout)
    {
        return violations(args, exitCode, stdout, "");
    }

    /**
     * @param stdin
     *            what the run read from stdin, where <code>apply --input -</code>
     *            finds its batch
     * @return one message per broken promise; empty if the run conforms or
     *         promised no JSON response (help or a table)
     */
    static List<String> violations(List<String> args, int exitCode, String stdout, String stdin)
    {
        if (exitCode != 0)
            return failureViolations(args, stdout, stdin);

        if (args.isEmpty() || args.contains(HelpText.OPTION) || isTable(args))
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

    /**
     * A failed command's problem document carries a code that the command's
     * help lists, i.e. one of its {@link CommandSpec#errorCodes()}; each
     * entry of a <code>validation-failed</code> problem's
     * <code>failures</code> carries a code that its operation or the batch
     * declares.
     */
    private static List<String> failureViolations(List<String> args, String stdout, String stdin)
    {
        var command = args.isEmpty() ? Optional.<CommandSpec>empty() : Commands.find(args.get(0));
        if (command.isEmpty())
            return List.of();

        JsonElement json;
        try
        {
            json = JsonParser.parseString(stdout);
        }
        catch (JsonParseException e)
        {
            return List.of();
        }
        if (!json.isJsonObject() || !json.getAsJsonObject().has("code"))
            return List.of();

        var code = json.getAsJsonObject().get("code").getAsString();
        var listed = command.get().errorCodes().stream().map(ErrorCode::code).toList();
        if (!listed.contains(code))
            return List.of("failed with '" + code + "', which the help of '" + command.get().name()
                            + "' does not list: " + listed);
        if (code.equals(ErrorCode.VALIDATION_FAILED.code()))
            return failureCodeViolations(args, json.getAsJsonObject(), stdin);
        return List.of();
    }

    /** Each failure's code is declared by the operation at its index, or by the batch. */
    private static List<String> failureCodeViolations(List<String> args, JsonObject problem, String stdin)
    {
        var operations = batch(args, stdin);
        if (operations.isEmpty())
            return List.of("validation-failed, but the batch its failures belong to could not be read");
        if (!(problem.get("failures") instanceof JsonArray failures))
            return List.of("validation-failed without a failures array");

        var errors = new ArrayList<String>();
        for (var element : failures)
        {
            var failure = element.getAsJsonObject();
            var declared = new LinkedHashSet<>(ApplyCommand.BATCH_FAILURE_CODES);
            operation(operations.get(), failure.get("operationIndex").getAsInt())
                            .ifPresent(spec -> declared.addAll(spec.failureCodes()));

            var codes = declared.stream().map(ErrorCode::code).toList();
            if (!codes.contains(failure.get("code").getAsString()))
                errors.add("failure " + failure + " carries a code that neither its operation nor the batch declares: "
                                + codes);
        }
        return errors;
    }

    /** The operations of the batch that <code>--input</code> gave, if it can be read. */
    private static Optional<JsonArray> batch(List<String> args, String stdin)
    {
        var option = "--input";
        String source = null;
        for (var i = 0; i < args.size(); i++)
        {
            if (args.get(i).equals(option) && i + 1 < args.size())
                source = args.get(i + 1);
            else if (args.get(i).startsWith(option + "="))
                source = args.get(i).substring(option.length() + 1);
        }
        if (source == null)
            return Optional.empty();

        try
        {
            var text = "-".equals(source) ? stdin : Files.readString(Path.of(source), StandardCharsets.UTF_8);
            var document = JsonParser.parseString(text);
            return document.isJsonObject() && document.getAsJsonObject().get("operations") instanceof JsonArray array
                            ? Optional.of(array)
                            : Optional.empty();
        }
        catch (IOException | JsonParseException e)
        {
            return Optional.empty();
        }
    }

    /** The operation the element at the index names, if it is an object that names a known one. */
    private static Optional<OperationSpec<?>> operation(JsonArray operations, int index)
    {
        if (index < 0 || index >= operations.size() || !operations.get(index).isJsonObject())
            return Optional.empty();
        var name = operations.get(index).getAsJsonObject().get(OperationSpec.OPERATION_MEMBER);
        if (name == null || !name.isJsonPrimitive())
            return Optional.empty();
        return Operations.ALL.stream().filter(spec -> spec.name().equals(name.getAsString()))
                        .<OperationSpec<?>> map(spec -> spec).findFirst();
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
