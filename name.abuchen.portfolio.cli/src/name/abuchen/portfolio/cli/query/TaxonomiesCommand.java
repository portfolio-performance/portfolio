package name.abuchen.portfolio.cli.query;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.cli.command.ClientFile;
import name.abuchen.portfolio.cli.command.CommandContext;
import name.abuchen.portfolio.cli.command.CommandSpec;
import name.abuchen.portfolio.cli.command.Decimals;
import name.abuchen.portfolio.cli.command.MasterData;
import name.abuchen.portfolio.cli.command.Paging;
import name.abuchen.portfolio.cli.command.Reference;
import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.cli.command.Sorting;
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

    /** What a reference to a taxonomy matches: UUID, then name. Taxonomies are never retired. */
    public static final Reference<Taxonomy> REFERENCE = Reference.of("taxonomy", Client::getTaxonomies,
                    Taxonomy::getId, Taxonomy::getName, List.of(new Reference.Key<>("name", Taxonomy::getName)),
                    Optional.empty(), List.of());

    /** The natural order is the position the app shows; <code>name</code> is an alphabetical alternative. */
    private static final Sorting.Spec<Taxonomy> SORT = Sorting.Spec.of(Taxonomy::getId, "position",
                    new Sorting.Field<Taxonomy>("position", client -> Comparator.comparingInt(client.getTaxonomies()::indexOf)),
                    Sorting.Field.of("name", MasterData.byName(Taxonomy::getName)));

    public static final CommandSpec SPEC = CommandSpec.of("taxonomies",
                    "Taxonomies in the order the app shows them (--sort position, the default) or by name, each with its classification tree flattened in pre-order: UUID, name, parent, path, target weight and direct assignments. --order desc reverses.",
                    Paging.pageSchema("A page of the taxonomies.", ITEM), TaxonomiesCommand::list, ClientFile.READ,
                    Paging.FACET, SORT);

    private TaxonomiesCommand()
    {
    }

    private static Map<String, Object> list(CommandContext run) throws CliException
    {
        var client = run.client();
        var page = run.get(Paging.FACET).of(run.get(SORT).sort(client, client.getTaxonomies()));

        var data = new LinkedHashMap<String, Object>();
        page.putInto(data, TaxonomiesCommand::render);
        return data;
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
