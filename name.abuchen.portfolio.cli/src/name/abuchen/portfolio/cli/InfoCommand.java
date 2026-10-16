package name.abuchen.portfolio.cli;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

final class InfoCommand
{
    private static final Schema DATA = Schema.object("Facts about the client file.")
                    .property("fileVersion", Schema.integer("Version of the file as read."))
                    .property("maxSupportedVersion",
                                    Schema.integer("Newest file version this CLI supports; newer files are refused."))
                    .property("format",
                                    Schema.enumOf("Detected file format.",
                                                    Arrays.stream(FileFormat.values()).map(FileFormat::id).toList()))
                    .property("baseCurrency", Schema.string("ISO currency code of the file's base currency."))
                    .property("counts", Schema.object("Number of entities in the file.")
                                    .property("instruments", Schema.integer("Instruments."))
                                    .property("investmentAccounts", Schema.integer("Investment accounts."))
                                    .property("cashAccounts", Schema.integer("Cash accounts."))
                                    .property("transactions", Schema.integer("Transaction records."))
                                    .property("taxonomies", Schema.integer("Taxonomies.")))
                    .property("pinnedUpstreamTag",
                                    Schema.string("Upstream release the bundled core is built from."));

    static final CommandSpec SPEC = new CommandSpec("info",
                    "File version, supported version ceiling, format, base currency and entity counts.", true,
                    List.of(), DATA, ResponseMeta.SCHEMA, InfoCommand::execute);

    private InfoCommand()
    {
    }

    private static Response execute(CommandContext context) throws CliException
    {
        var loaded = context.loadClient();
        var client = loaded.client();

        var counts = new LinkedHashMap<String, Object>();
        counts.put("instruments", client.getSecurities().size());
        counts.put("investmentAccounts", client.getPortfolios().size());
        counts.put("cashAccounts", client.getAccounts().size());
        counts.put("transactions", client.getAllTransactions().size());
        counts.put("taxonomies", client.getTaxonomies().size());

        var data = new LinkedHashMap<String, Object>();
        data.put("fileVersion", client.getFileVersionAfterRead());
        data.put("maxSupportedVersion", CoreVersion.MAX_SUPPORTED_FILE_VERSION);
        data.put("format", loaded.format().id());
        data.put("baseCurrency", client.getBaseCurrency());
        data.put("counts", counts);
        data.put("pinnedUpstreamTag", CoreVersion.PINNED_UPSTREAM_TAG);

        return Response.of(data, ResponseMeta.of(context, client));
    }
}
