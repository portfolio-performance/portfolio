package name.abuchen.portfolio.cli.apply;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.cli.Commands;
import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.cli.command.ClientFile;
import name.abuchen.portfolio.cli.command.CommandContext;
import name.abuchen.portfolio.cli.command.CommandSpec;
import name.abuchen.portfolio.cli.command.Declaration;
import name.abuchen.portfolio.cli.command.ErrorCode;
import name.abuchen.portfolio.cli.command.Facet;
import name.abuchen.portfolio.cli.command.OptionSpec;
import name.abuchen.portfolio.cli.command.Schema;

/**
 * <code>pp-cli apply</code>: applies a batch of write operations atomically
 * to the client file, or previews it with <code>--dry-run</code>.
 */
public final class ApplyCommand
{
    private static final OptionSpec INPUT = new OptionSpec("input", "file|-",
                    "JSON document {\"operations\": [...]} to apply: a file, or - for stdin. If stdin carries the batch, give the password of an encrypted file through PP_PASSWORD.")
                                    .asRequired();

    /** The batch of <code>--input</code>; validating its operations happens in the write. */
    static final Facet<JsonArray> BATCH = Facet.of(Declaration.empty().options(INPUT).codes(
                    ErrorCode.INPUT_UNREADABLE, ErrorCode.INVALID_INPUT, ErrorCode.VALIDATION_FAILED),
                    ApplyCommand::readOperations);

    private static final Facet<ClientFile.Writer> WRITE = ClientFile.write(
                    "Validate the batch and report the changes and the would-be backup path without writing any file, backup or lock.");

    private static final Schema CHANGE = Schema.object("What an operation did to one entity.")
                    .property("entity", Schema.string("Kind of entity: instrument, cash-account, investment-account or transaction."))
                    .property("uuid", Schema.string("UUID of the entity."))
                    .optionalProperty("before", Schema.anyObject(
                                    "The changed fields as they were, or for a deleted entity all its fields; absent for a created entity. A field that was unset is null."))
                    .property("after", Schema.anyObject(
                                    "A created entity's fields that are set, or an updated entity's changed fields as they are now; a cleared field is null. Empty for a deleted entity."));

    private static final Schema DATA = Schema.object("The outcome of the batch.").property("results", Schema.array(
                    "One result per operation, in order.",
                    Schema.object("The changes one operation made.")
                                    .property("operationIndex", Schema.integer("Position in the batch, from 0."))
                                    .property("operation", Schema.string("Operation name."))
                                    .property("created", Schema.array("Entities the operation created.", CHANGE))
                                    .property("updated", Schema.array("Entities the operation changed.", CHANGE))
                                    .property("deleted", Schema.array("Entities the operation deleted.", CHANGE))));

    public static final CommandSpec SPEC = CommandSpec.of("apply",
                    "Applies a batch of write operations atomically; see the operations list. Writes by default, keeping the file's format, after saving a backup in a backups folder next to it (--backup-dir, --keep). A write is refused, with nothing changed, if it would introduce a consistency issue that core's checker did not already find, if the file changed on disk since it was read, or on macOS if the file is open in another program (unless --force). On any validation failure nothing is written and every failure is reported with operationIndex, field, code and message.",
                    DATA, ApplyCommand::execute, BATCH, WRITE);

    private ApplyCommand()
    {
    }

    private static Object execute(CommandContext run) throws CliException
    {
        var operations = run.get(BATCH);
        var results = run.get(WRITE).write(client -> ApplyBatch.run(client, operations, Commands.OPERATIONS));

        var data = new LinkedHashMap<String, Object>();
        data.put("results", results);
        return data;
    }

    private static JsonArray readOperations(CommandContext context) throws CliException
    {
        var source = context.text(INPUT).orElseThrow();

        String text;
        try
        {
            text = "-".equals(source) ? new String(context.stdin().readAllBytes(), StandardCharsets.UTF_8)
                            : Files.readString(Path.of(source), StandardCharsets.UTF_8);
        }
        catch (IOException | RuntimeException e)
        {
            throw CliException.usage(ErrorCode.INPUT_UNREADABLE, "Input could not be read",
                            "The input " + source + " could not be read: " + e.getMessage())
                                            .with("option", INPUT.name());
        }

        try
        {
            var document = JsonParser.parseString(text);
            if (document.isJsonObject() && document.getAsJsonObject().get("operations") instanceof JsonArray operations
                            && !operations.isEmpty())
                return operations;
        }
        catch (JsonParseException e)
        {
            throw invalidInput("The input is not valid JSON: " + e.getMessage());
        }

        throw invalidInput("The input must be a JSON object {\"operations\": [...]} with at least one operation.");
    }

    private static CliException invalidInput(String detail)
    {
        return CliException.usage(ErrorCode.INVALID_INPUT, "Invalid input", detail).with("option", INPUT.name());
    }
}
