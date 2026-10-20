package name.abuchen.portfolio.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;

/**
 * <code>taxonomies</code>: the taxonomies of the file, in the order the app
 * shows them, each with its classification tree flattened in pre-order and the
 * direct assignments of every classification.
 */
final class TaxonomiesCommand
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
    static final MasterDataList.Source<Taxonomy> SOURCE = new MasterDataList.Source<Taxonomy>(Client::getTaxonomies,
                    Taxonomy::getId, Taxonomy::getName, client -> TaxonomiesCommand::render);

    static final CommandSpec SPEC = new CommandSpec("taxonomies",
                    "Taxonomies in the order the app shows them, each with its classification tree flattened in pre-order: UUID, name, parent, path, target weight and direct assignments.",
                    true, Paging.OPTIONS, Paging.pageSchema("A page of the taxonomies.", ITEM),
                    ResponseMeta.pagedSchema(), TaxonomiesCommand::execute);

    private TaxonomiesCommand()
    {
    }

    private static Response execute(CommandContext context) throws CliException
    {
        var paging = Paging.parse(context);
        var client = context.loadClient().client();

        var page = paging.of(client.getTaxonomies());

        var data = new LinkedHashMap<String, Object>();
        page.putInto(data, TaxonomiesCommand::render);
        return Response.of(data, ResponseMeta.paged(context, client, paging));
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
