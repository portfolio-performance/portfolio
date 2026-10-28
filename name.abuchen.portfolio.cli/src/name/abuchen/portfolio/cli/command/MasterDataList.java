package name.abuchen.portfolio.cli.command;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import name.abuchen.portfolio.model.Client;

/**
 * A paged, sortable list of master data (instruments or accounts) as a
 * command. A command only says what its entities are and how to render one;
 * the options, sorting, paging, response shape and meta come from here.
 */
public final class MasterDataList
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
    public record Source<T>(Function<Client, List<T>> entities, Function<T, String> uuid, Function<T, String> name,
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
     *            what each item holds; the sort fields are appended
     * @param item
     *            the schema of one rendered item
     * @param columns
     *            the columns of <code>--format table</code>
     * @param updatedAt
     *            the last modified time of an entity; null if core has none
     */
    public static <T> CommandSpec command(String name, String summary, Schema item, List<TableColumn> columns,
                    Source<T> source, Function<T, Instant> updatedAt)
    {
        var sort = sortSpec(source, updatedAt);

        return CommandSpec.of(name,
                        summary + ", sorted by name (default) or updatedAt, then UUID; --order desc reverses.",
                        Paging.pageSchema("A page of the list.", item), run -> list(run, source, sort),
                        ClientFile.READ, Paging.FACET, sort).withTable(columns);
    }

    private static <T> Sorting.Spec<T> sortSpec(Source<T> source, Function<T, Instant> updatedAt)
    {
        return Sorting.Spec.of(source.uuid(), "name", Sorting.Field.of("name", MasterData.byName(source.name())),
                        Sorting.Field.of("updatedAt",
                                        Sorting.byInstant(entity -> Sorting.lastModified(updatedAt.apply(entity)))));
    }

    private static <T> Map<String, Object> list(CommandContext run, Source<T> source, Sorting.Spec<T> sort)
                    throws CliException
    {
        var client = run.client();
        var ordered = run.get(sort).sort(client, source.entities().apply(client));
        var page = run.get(Paging.FACET).of(ordered);

        var data = new LinkedHashMap<String, Object>();
        page.putInto(data, source.renderer().apply(client));
        return data;
    }
}
