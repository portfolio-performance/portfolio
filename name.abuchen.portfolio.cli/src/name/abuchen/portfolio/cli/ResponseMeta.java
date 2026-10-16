package name.abuchen.portfolio.cli;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import name.abuchen.portfolio.model.Client;

/**
 * The <code>meta</code> of commands that read a client file: the file version
 * and the date the command ran. List commands add the resolved paging.
 */
final class ResponseMeta
{
    /** For commands that don't read a client file. */
    static final Schema AS_OF_SCHEMA = Schema.object("Assumptions behind the response.").property("asOf",
                    Schema.string("ISO date the command ran, from the clock."));

    static final Schema SCHEMA = Schema.object("Assumptions behind the response.")
                    .property("fileVersion", Schema.integer("Version of the file as read."))
                    .property("asOf", Schema.string("ISO date the command ran, from the clock."));

    static final Schema PAGED_SCHEMA = Schema.object("Assumptions behind the response.")
                    .property("fileVersion", Schema.integer("Version of the file as read."))
                    .property("asOf", Schema.string("ISO date the command ran, from the clock."))
                    .property("limit", Schema.integer("Page size used."))
                    .property("offset", Schema.integer("Index of the first item returned."));

    private ResponseMeta()
    {
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

    static Map<String, Object> paged(CommandContext context, Client client, Paging paging)
    {
        var meta = of(context, client);
        meta.put("limit", paging.limit());
        meta.put("offset", paging.offset());
        return meta;
    }
}
