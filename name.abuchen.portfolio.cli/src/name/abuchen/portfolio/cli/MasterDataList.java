package name.abuchen.portfolio.cli;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import name.abuchen.portfolio.model.Client;

/**
 * A paged, stably ordered list of master data (instruments or accounts) as a
 * command. A command only says what its entities are and how to render one;
 * the options, ordering, paging, response shape and meta come from here.
 */
final class MasterDataList
{
    /**
     * What a list command contributes.
     *
     * @param entities
     *            all entities of the client, in any order
     * @param uuid
     *            the tie-break of the ordering
     * @param name
     *            the ordering key; null counts as empty
     * @param renderer
     *            prepares, per client, how one entity is rendered
     */
    record Source<T>(Function<Client, List<T>> entities, Function<T, String> uuid, Function<T, String> name,
                    Function<Client, Function<T, Map<String, Object>>> renderer)
    {
        /** Name (case-insensitive, ordinal), then UUID. */
        Comparator<T> ordering()
        {
            return MasterData.byNameThenUuid(name, uuid);
        }
    }

    private MasterDataList()
    {
    }

    /**
     * @param summary
     *            what each item holds; the shared ordering is appended
     * @param item
     *            the schema of one rendered item
     */
    static <T> CommandSpec command(String name, String summary, Schema item, Source<T> source)
    {
        return new CommandSpec(name, summary + ", ordered by name then UUID.", true, Paging.OPTIONS,
                        Paging.pageSchema("A page of the list.", item), ResponseMeta.pagedSchema(),
                        context -> respond(context, source));
    }

    private static <T> Response respond(CommandContext context, Source<T> source) throws CliException
    {
        var paging = Paging.parse(context);
        var client = context.loadClient().client();

        var ordered = source.entities().apply(client).stream().sorted(source.ordering()).toList();
        var page = paging.of(ordered);

        var data = new LinkedHashMap<String, Object>();
        page.putInto(data, source.renderer().apply(client));

        return Response.of(data, ResponseMeta.paged(context, client, paging));
    }
}
