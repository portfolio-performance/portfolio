package name.abuchen.portfolio.cli;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import name.abuchen.portfolio.model.Client;

/**
 * The <code>meta</code> of commands that read a client file: the file version
 * and the date the command ran. List commands add the resolved paging and
 * sorting.
 */
final class ResponseMeta
{
    private static final Schema FILE_VERSION = Schema.integer("Version of the file as read.");
    private static final Schema AS_OF = Schema.string("ISO date the command ran, from the clock.");

    /** For commands that don't read a client file. */
    static final Schema AS_OF_SCHEMA = envelope().property("asOf", AS_OF);

    static final Schema SCHEMA = fileEnvelope();

    private ResponseMeta()
    {
    }

    /** A fresh copy each call, so a command can add members of its own. */
    static Schema fileSchema()
    {
        return fileEnvelope();
    }

    /** A fresh copy each call, so a command can add members of its own. */
    static Schema pagedSchema(Sorting.Spec<?> sort)
    {
        return Sorting.declareIn(fileEnvelope().property("limit", Schema.integer("Page size used."))
                        .property("offset", Schema.integer("Index of the first item returned.")), sort);
    }

    private static Schema envelope()
    {
        return Schema.object("Assumptions behind the response.");
    }

    private static Schema fileEnvelope()
    {
        return envelope().property("fileVersion", FILE_VERSION).property("asOf", AS_OF);
    }

    static Map<String, Object> asOf(CommandContext context)
    {
        var meta = new LinkedHashMap<String, Object>();
        meta.put("asOf", LocalDate.now(context.clock()).toString());
        return meta;
    }

    static Map<String, Object> of(CommandContext context, Client client)
    {
        var meta = new LinkedHashMap<String, Object>();
        meta.put("fileVersion", client.getFileVersionAfterRead());
        meta.putAll(asOf(context));
        return meta;
    }

    static Map<String, Object> paged(CommandContext context, Client client, Paging paging, Sorting<?> sorting)
    {
        var meta = of(context, client);
        meta.put("limit", paging.limit());
        meta.put("offset", paging.offset());
        sorting.putInto(meta);
        return meta;
    }

    /** The meta of a write: file versions, backup and whether it was only a preview. */
    static Schema writeSchema()
    {
        return envelope().property("fileVersionBefore", Schema.integer("Version of the file as read."))
                        .property("fileVersionAfter", Schema.integer("Version the file has after the write."))
                        .property("asOf", AS_OF)
                        .property("dryRun", Schema.bool("True if nothing was written, backup included."))
                        .property("backupPath", Schema.string(
                                        "Where the backup of the original file is, or with a dry run would be, written."));
    }

    static Map<String, Object> written(CommandContext context, SafeWrite.Outcome<?> outcome)
    {
        var meta = new LinkedHashMap<String, Object>();
        meta.put("fileVersionBefore", outcome.fileVersionBefore());
        meta.put("fileVersionAfter", outcome.fileVersionAfter());
        meta.putAll(asOf(context));
        meta.put("dryRun", outcome.dryRun());
        meta.put("backupPath", outcome.backup().toString());
        return meta;
    }
}
