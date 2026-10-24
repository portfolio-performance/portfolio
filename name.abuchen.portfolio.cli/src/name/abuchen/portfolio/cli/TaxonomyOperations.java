package name.abuchen.portfolio.cli;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;

/**
 * The <code>taxonomy.assign</code> operation. The input mirrors the
 * <code>assignments</code> that the <code>taxonomies</code> output lists per
 * classification, seen from the instrument: the classification and the weight
 * as a fraction.
 */
final class TaxonomyOperations
{
    private static final String OP = "taxonomy.assign";

    private static final Set<String> KNOWN = Set.of(ApplyBatch.OPERATION_MEMBER, "taxonomy", "instrument",
                    "assignments");

    static final OperationSpec ASSIGN = new OperationSpec(OP,
                    "Replaces the complete assignment of one instrument within one existing taxonomy. The assignments list names classifications by UUID, each with a weight as a fraction of one, as in the taxonomies output; an empty list removes the instrument from the taxonomy. Weights must be positive and at most 100% in total, a classification may appear only once, and only leaf classifications, those that no other classification names as its parent, can be used. The response is one updated change, with the assignments before and after, whose uuid is the instrument.",
                    schema(), TaxonomyOperations::assign);

    static final List<OperationSpec> ALL = List.of(ASSIGN);

    private TaxonomyOperations()
    {
    }

    private static Schema schema()
    {
        var assignment = Schema.object("The share of the instrument that is placed in one classification.")
                        .property("classification", Schema.string("UUID of a leaf classification of the taxonomy."))
                        .property("weight", Schema.number(
                                        "Positive fraction of one, e.g. 0.6 for 60%, with at most four decimal places."));

        return Schema.object("The complete assignment of one instrument within one taxonomy.")
                        .property(ApplyBatch.OPERATION_MEMBER, Schema.enumOf("Operation name.", List.of(OP)))
                        .property("taxonomy", Schema.string("UUID of the taxonomy."))
                        .property("instrument", Schema.string("UUID of the instrument."))
                        .property("assignments", Schema.array(
                                        "The classifications to place the instrument in; empty to remove it from the taxonomy.",
                                        assignment));
    }

    static void assign(OperationContext context)
    {
        context.rejectUnknownFields(KNOWN);

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
        return TransactionOperations.find(context, context.input(), "taxonomy", "This operation needs the taxonomy.",
                        "taxonomy", context.client().getTaxonomies(), Taxonomy::getId);
    }

    private static Security parseInstrument(OperationContext context)
    {
        return TransactionOperations.find(context, context.input(), "instrument",
                        "This operation needs the instrument.", "instrument", context.client().getSecurities(),
                        Security::getUUID);
    }

    /** The placements, or null if the list itself is unusable; problems of single entries are reported. */
    private static List<Placement> parsePlacements(OperationContext context, Taxonomy taxonomy)
    {
        var element = context.input().get("assignments");
        if (element == null || element.isJsonNull())
        {
            context.fail("assignments", "required", "This operation needs the assignments; an empty list removes the instrument.");
            return null;
        }
        if (!element.isJsonArray())
        {
            context.fail("assignments", "invalid-type", "The assignments must be an array.");
            return null;
        }

        var nodes = taxonomy == null ? null : TaxonomyTree.preOrder(taxonomy);

        var placements = new ArrayList<Placement>();
        var seen = new HashSet<String>();
        var total = BigDecimal.ZERO;
        var array = element.getAsJsonArray();
        for (var index = 0; index < array.size(); index++)
        {
            var field = "assignments[" + index + "]";
            if (!array.get(index).isJsonObject())
            {
                context.fail(field, "invalid-type", "An assignment must be a JSON object.");
                continue;
            }

            var entry = array.get(index).getAsJsonObject();
            context.rejectUnknownFields(entry, field, Set.of("classification", "weight"));

            var classification = parseClassification(context, entry, field, nodes, seen);
            var weight = parseWeight(context, entry, field);
            if (classification != null && weight != null)
            {
                placements.add(new Placement(classification, weight));
                total = total.add(BigDecimal.valueOf(weight));
            }
        }

        if (total.compareTo(BigDecimal.valueOf(Classification.ONE_HUNDRED_PERCENT)) > 0)
            context.fail("assignments", "weight-total-exceeded", "The weights add up to more than 100%.");
        return placements;
    }

    private static Classification parseClassification(OperationContext context, JsonObject entry, String field,
                    List<TaxonomyTree.Node> nodes, Set<String> seen)
    {
        var member = field + ".classification";
        var element = entry.get("classification");
        if (element != null && !element.isJsonNull() && !OperationContext.isText(element))
        {
            context.fail(member, "invalid-type", "The classification must be a string.");
            return null;
        }

        var uuid = element == null || element.isJsonNull() ? "" : element.getAsString().trim();
        if (uuid.isEmpty())
        {
            context.fail(member, "required", "An assignment needs a classification.");
            return null;
        }

        // without a taxonomy there is nothing to look a classification up in, and that is reported already
        if (nodes == null)
            return null;

        var found = nodes.stream().filter(n -> uuid.equals(n.uuid())).findFirst();
        if (found.isEmpty())
        {
            context.fail(member, "reference-not-found", "No classification with UUID " + uuid + " in the taxonomy.");
            return null;
        }

        var classification = found.get().classification();
        if (!seen.add(uuid))
        {
            context.fail(member, "duplicate-classification", "The classification is already listed above.");
            return null;
        }
        if (!classification.getChildren().isEmpty())
        {
            context.fail(member, "not-a-leaf", "Only a classification without children can hold an assignment.");
            return null;
        }
        return classification;
    }

    /** The weight as stored in the file, or null after reporting why not. */
    private static Integer parseWeight(OperationContext context, JsonObject entry, String field)
    {
        var value = TransactionOperations.parseNumber(context, entry, field + ".weight", "weight",
                        "An assignment needs a weight.", true);
        if (value == null)
            return null;

        try
        {
            return value.multiply(BigDecimal.valueOf(Classification.ONE_HUNDRED_PERCENT)).intValueExact();
        }
        catch (ArithmeticException e)
        {
            context.fail(field + ".weight", "invalid-amount",
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
