package name.abuchen.portfolio.cli;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;

import name.abuchen.portfolio.model.Client;

final class InfoCommand
{
    /**
     * The upstream release the bundled core is built from. Bump it together
     * with the core when updating the CLI.
     */
    static final String PINNED_UPSTREAM_TAG = "0.88.0";

    static final CommandSpec SPEC = new CommandSpec("info",
                    "File version, supported version ceiling, format, base currency and entity counts.", true,
                    List.of(), InfoCommand::execute);

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
        data.put("maxSupportedVersion", Client.CURRENT_VERSION);
        data.put("format", loaded.format().id());
        data.put("baseCurrency", client.getBaseCurrency());
        data.put("counts", counts);
        data.put("pinnedUpstreamTag", PINNED_UPSTREAM_TAG);

        var meta = new LinkedHashMap<String, Object>();
        meta.put("fileVersion", client.getFileVersionAfterRead());
        meta.put("asOf", LocalDate.now(context.clock()).toString());

        return Response.of(data, meta);
    }
}
