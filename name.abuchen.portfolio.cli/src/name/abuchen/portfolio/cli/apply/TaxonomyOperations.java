package name.abuchen.portfolio.cli.apply;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import name.abuchen.portfolio.cli.command.Decimals;
import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.cli.query.TaxonomyTree;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;

/**
 * The <code>taxonomy.assign</code> operation. The input mirrors the
 * <code>assignments</code> that the <code>taxonomies</code> output lists per
 * classification, seen from the instrument: the classification and the weight
 * as a fraction.
 */
public final class TaxonomyOperations
{
    private static final String OP = "taxonomy.assign";

    private static final Schema ASSIGNMENT = Schema.object("The share of the instrument that is placed in one classification.")
                    .property("classification", Schema.string("UUID of a leaf classification of the taxonomy."))
                    .property("weight", Schema.number(
                                    "Positive fraction of one, e.g. 0.6 for 60%, with at most four decimal places."));

    static final OperationSpec ASSIGN = new OperationSpec(OP,
                    "Replaces the complete assignment of one instrument within one existing taxonomy. The assignments list names classifications by UUID, each with a weight as a fraction of one, as in the taxonomies output; an empty list removes the instrument from the taxonomy. Weights must be positive and at most 100% in total, a classification may appear only once, and only leaf classifications, those that no other classification names as its parent, can be used. The response is one updated change, with the assignments before and after, whose uuid is the instrument.",
                    schema(), TaxonomyOperations::assign);

    public static final List<OperationSpec> ALL = List.of(ASSIGN);

    private TaxonomyOperations()
    {
    }

    private static Schema schema()
    {
        return Schema.object("The complete assignment of one instrument within one taxonomy.")
                        .property(ApplyBatch.OPERATION_MEMBER, Schema.enumOf("Operation name.", List.of(OP)))
                        .property("taxonomy", Schema.string("UUID of the taxonomy."))
                        .property("instrument", Schema.string("UUID of the instrument."))
                        .property("assignments", Schema.array(
                                        "The classifications to place the instrument in; empty to remove it from the taxonomy.",
                                        ASSIGNMENT));
    }

    static void assign(OperationContext context)
    {
        var taxonomy = parseTaxonomy(context);
        var instrument = parseInstrument(context);
        var placements = parsePlacements(context, taxonomy);

        if (taxonomy == null || instrument == null)
            return;

        // one assignment per taxonomy and instrument in a batch; changing the instrument's master data is separate
        if (!context.claim("instrument", taxonomy.getId() + "/" + instrument.getUUID()))
            return;

        if (placements == null || context.failed())
            return;

        var before = assignmentsOf(taxonomy, instrument);

        taxonomy.getRoot().getTreeElements()
                        .forEach(c -> c.getAssignments().removeIf(a -> a.getInvestmentVehicle() == instrument));
        for (var placement : placements)
        {
            var assignment = new Classification.Assignment(instrument, placement.weight());
            assignment.setRank(Integer.MAX_VALUE);
            placement.classification().addAssignment(assignment);
        }

        var after = assignmentsOf(taxonomy, instrument);
        if (!before.equals(after))
            context.updated(new OperationContext.Change("taxonomy-assignment", instrument.getUUID(),
                            Map.of("assignments", before), Map.of("assignments", after)));
    }

    /** One classification to place the instrument in, with its weight as stored in the file. */
    private record Placement(Classification classification, int weight)
    {
    }

    private static Taxonomy parseTaxonomy(OperationContext context)
    {
        return context.input().existing("taxonomy", EntityKind.TAXONOMY, "This operation needs the taxonomy.");
    }

    private static Security parseInstrument(OperationContext context)
    {
        return context.input().existing("instrument", EntityKind.INSTRUMENT, "This operation needs the instrument.");
    }

    /** The placements, or null if the list itself is unusable; problems of single entries are reported. */
    private static List<Placement> parsePlacements(OperationContext context, Taxonomy taxonomy)
    {
        var input = context.input();
        if (!input.has("assignments"))
        {
            context.fail("assignments", "required", "This operation needs the assignments; an empty list removes the instrument.");
            return null;
        }

        var nodes = taxonomy == null ? null : TaxonomyTree.preOrder(taxonomy);

        var placements = new ArrayList<Placement>();
        var seen = new HashSet<String>();
        var usable = input.eachObject("assignments", "An assignment must be a JSON object.", entry -> {
            entry.rejectUnknownFields(ASSIGNMENT);

            var classification = parseClassification(entry, nodes, seen);
            var weight = parseWeight(entry);
            if (classification != null && weight != null)
                placements.add(new Placement(classification, weight));
        });
        if (!usable)
            return null;

        var total = placements.stream().map(p -> BigDecimal.valueOf(p.weight())).reduce(BigDecimal.ZERO,
                        BigDecimal::add);
        if (total.compareTo(BigDecimal.valueOf(Classification.ONE_HUNDRED_PERCENT)) > 0)
            context.fail("assignments", "weight-total-exceeded", "The weights add up to more than 100%.");
        return placements;
    }

    private static Classification parseClassification(OperationInput entry, List<TaxonomyTree.Node> nodes,
                    Set<String> seen)
    {
        var member = "classification";
        var element = entry.json().get(member);
        if (entry.has(member) && !OperationContext.isText(element))
        {
            entry.fail(member, "invalid-type", "The classification must be a string.");
            return null;
        }

        var uuid = entry.has(member) ? element.getAsString().trim() : "";
        if (uuid.isEmpty())
        {
            entry.fail(member, "required", "An assignment needs a classification.");
            return null;
        }

        // without a taxonomy there is nothing to look a classification up in, and that is reported already
        if (nodes == null)
            return null;

        var found = nodes.stream().filter(n -> uuid.equals(n.uuid())).findFirst();
        if (found.isEmpty())
        {
            entry.fail(member, "reference-not-found", "No classification with UUID " + uuid + " in the taxonomy.");
            return null;
        }

        var classification = found.get().classification();
        if (!seen.add(uuid))
        {
            entry.fail(member, "duplicate-classification", "The classification is already listed above.");
            return null;
        }
        if (!classification.getChildren().isEmpty())
        {
            entry.fail(member, "not-a-leaf", "Only a classification without children can hold an assignment.");
            return null;
        }
        return classification;
    }

    /** The weight as stored in the file, or null after reporting why not. */
    private static Integer parseWeight(OperationInput entry)
    {
        var value = entry.number("weight", "An assignment needs a weight.", true);
        if (value == null)
            return null;

        try
        {
            return value.multiply(BigDecimal.valueOf(Classification.ONE_HUNDRED_PERCENT)).intValueExact();
        }
        catch (ArithmeticException e)
        {
            entry.fail("weight", "invalid-amount",
                            "The weight has more decimal places than are stored; use at most four.");
            return null;
        }
    }

    private static List<Map<String, Object>> assignmentsOf(Taxonomy taxonomy, Security instrument)
    {
        var rendered = new ArrayList<Map<String, Object>>();
        for (var node : TaxonomyTree.preOrder(taxonomy))
        {
            for (var assignment : node.classification().getAssignments())
            {
                if (assignment.getInvestmentVehicle() != instrument)
                    continue;

                var json = new LinkedHashMap<String, Object>();
                json.put("classification", node.uuid());
                json.put("weight", Decimals.of(TaxonomyTree.fraction(assignment.getWeight())));
                rendered.add(json);
            }
        }
        return rendered;
    }
}
