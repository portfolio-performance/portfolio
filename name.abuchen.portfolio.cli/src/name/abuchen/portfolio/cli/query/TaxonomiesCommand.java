package name.abuchen.portfolio.cli.query;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import name.abuchen.portfolio.cli.CliException;
import name.abuchen.portfolio.cli.CommandContext;
import name.abuchen.portfolio.cli.CommandSpec;
import name.abuchen.portfolio.cli.Decimals;
import name.abuchen.portfolio.cli.MasterData;
import name.abuchen.portfolio.cli.MasterDataList;
import name.abuchen.portfolio.cli.Paging;
import name.abuchen.portfolio.cli.Response;
import name.abuchen.portfolio.cli.ResponseMeta;
import name.abuchen.portfolio.cli.Schema;
import name.abuchen.portfolio.cli.Sorting;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;

/**
 * <code>taxonomies</code>: the taxonomies of the file, in the order the app
 * shows them, each with its classification tree flattened in pre-order and the
 * direct assignments of every classification.
 */
public final class TaxonomiesCommand
{
    private static final Schema ASSIGNMENT = Schema.object("One instrument or cash account placed in a classification.")
                    .optionalProperty("instrument", Schema.string("UUID of the instrument. Present for an instrument."))
                    .optionalProperty("cashAccount", Schema.string("UUID of the cash account. Present for a cash account."))
                    .property("name", Schema.string("Name of the instrument or account; empty if the file has none."))
                    .property("weight", Schema.number(
                                    "The share of the instrument or account placed here, as a fraction of one. Unrounded."));

    private static final Schema CLASSIFICATION = Schema.object("One classification of a taxonomy.")
                    .property("uuid", Schema.string("Canonical UUID."))
                    .property("name", Schema.string("Classification name; empty if the file has none."))
                    .optionalProperty("parent", Schema.string("UUID of the parent classification. Absent at the top level."))
                    .property("path", Schema.array("Names of the ancestors, top level first, excluding this classification.",
                                    Schema.string("A classification name.")))
                    .property("targetWeight", Schema.number(
                                    "The rebalancing target of the classification as an unrounded fraction of its parent. 0 if not set."))
                    .optionalProperty("key", Schema.string("The key of the classification, if set."))
                    .optionalProperty("note", Schema.string("Note, if set."))
                    .property("assignments", Schema.array(
                                    "The assignments made directly to this classification, not to its subtree, ordered by name then UUID. Retired instruments and accounts are included.",
                                    ASSIGNMENT));

    private static final Schema ITEM = Schema.object("A taxonomy.")
                    .property("uuid", Schema.string("Canonical UUID."))
                    .property("name", Schema.string("Taxonomy name; empty if the file has none."))
                    .property("classifications", Schema.array(
                                    "All classifications at every depth, flattened in tree pre-order: siblings by the rank the app shows, then name, then UUID. Not paged.",
                                    CLASSIFICATION));

    /** What a reference to a taxonomy matches; taxonomies are listed in file order, not through {@link MasterDataList}. */
    public static final MasterDataList.Source<Taxonomy> SOURCE = new MasterDataList.Source<Taxonomy>(
                    Client::getTaxonomies, Taxonomy::getId, Taxonomy::getName, client -> TaxonomiesCommand::render);

    /** The natural order is the position the app shows; <code>name</code> is an alphabetical alternative. */
    private static final Sorting.Spec<Taxonomy> SORT = Sorting.Spec.of(Taxonomy::getId, "position",
                    new Sorting.Field<Taxonomy>("position", client -> Comparator.comparingInt(client.getTaxonomies()::indexOf)),
                    Sorting.Field.of("name", MasterData.byName(Taxonomy::getName)));

    public static final CommandSpec SPEC = new CommandSpec("taxonomies",
                    "Taxonomies in the order the app shows them (--sort position, the default) or by name, each with its classification tree flattened in pre-order: UUID, name, parent, path, target weight and direct assignments. --order desc reverses.",
                    true, Stream.concat(Paging.OPTIONS.stream(), SORT.options().stream()).toList(),
                    Paging.pageSchema("A page of the taxonomies.", ITEM), ResponseMeta.pagedSchema(SORT),
                    TaxonomiesCommand::execute);

    private TaxonomiesCommand()
    {
    }

    private static Response execute(CommandContext context) throws CliException
    {
        var paging = Paging.parse(context);
        var sorting = SORT.parse(context);
        var client = context.loadClient().client();

        var page = paging.of(sorting.sort(client, client.getTaxonomies()));

        var data = new LinkedHashMap<String, Object>();
        page.putInto(data, TaxonomiesCommand::render);
        return Response.of(data, ResponseMeta.paged(context, client, paging, sorting));
    }

    private static Map<String, Object> render(Taxonomy taxonomy)
    {
        var json = new LinkedHashMap<String, Object>();
        MasterData.putIdentity(json, taxonomy.getId(), taxonomy.getName());
        json.put("classifications", TaxonomyTree.preOrder(taxonomy).stream().map(TaxonomiesCommand::render).toList());
        return json;
    }

    private static Map<String, Object> render(TaxonomyTree.Node node)
    {
        var classification = node.classification();

        var json = node.identity();
        json.put("targetWeight", Decimals.of(TaxonomyTree.fraction(classification.getWeight())));
        MasterData.putIfSet(json, "key", classification.getKey());
        MasterData.putIfSet(json, "note", classification.getNote());
        json.put("assignments", assignments(classification));
        return json;
    }

    private static List<Map<String, Object>> assignments(Classification classification)
    {
        var rendered = new ArrayList<Map<String, Object>>();
        classification.getAssignments().stream()
                        .sorted(MasterData.byNameThenUuid(a -> a.getInvestmentVehicle().getName(),
                                        a -> a.getInvestmentVehicle().getUUID()))
                        .forEach(assignment -> {
                            var vehicle = assignment.getInvestmentVehicle();

                            var json = new LinkedHashMap<String, Object>();
                            json.put(vehicle instanceof Security ? "instrument" : "cashAccount", vehicle.getUUID());
                            json.put("name", MasterData.orEmpty(vehicle.getName()));
                            json.put("weight", Decimals.of(TaxonomyTree.fraction(assignment.getWeight())));
                            rendered.add(json);
                        });
        return rendered;
    }
}
