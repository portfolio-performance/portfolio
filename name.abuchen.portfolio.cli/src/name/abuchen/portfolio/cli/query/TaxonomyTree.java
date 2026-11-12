package name.abuchen.portfolio.cli.query;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import name.abuchen.portfolio.cli.command.MasterData;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Taxonomy;

/**
 * The classifications of one taxonomy as the commands show them: the root is
 * left out, and the rest is flattened in pre-order, siblings by the rank the
 * app shows them in, then name (case-insensitive, ordinal), then UUID.
 */
public final class TaxonomyTree
{
    /**
     * A classification and where it sits.
     *
     * @param parent
     *            empty at the top level
     * @param path
     *            names of the ancestors, top level first, excluding the
     *            classification itself
     */
    public record Node(Classification classification, Optional<Classification> parent, List<String> path)
    {
        public String uuid()
        {
            return classification.getId();
        }

        /** The members both commands show: <code>uuid</code>, <code>name</code>, <code>parent</code> and <code>path</code>. */
        Map<String, Object> identity()
        {
            var json = new LinkedHashMap<String, Object>();
            MasterData.putIdentity(json, uuid(), classification.getName());
            parent.ifPresent(p -> json.put("parent", p.getId()));
            json.put("path", path);
            return json;
        }
    }

    private static final Comparator<Classification> SIBLINGS = Comparator
                    .comparingInt(Classification::getRank)
                    .thenComparing(MasterData.byNameThenUuid(Classification::getName, Classification::getId));

    private TaxonomyTree()
    {
    }

    public static List<Node> preOrder(Taxonomy taxonomy)
    {
        var nodes = new ArrayList<Node>();
        collect(taxonomy.getRoot(), false, List.of(), nodes);
        return nodes;
    }

    private static void collect(Classification parent, boolean parentIsShown, List<String> path, List<Node> nodes)
    {
        var children = parent.getChildren().stream().sorted(SIBLINGS).toList();
        for (var child : children)
        {
            nodes.add(new Node(child, parentIsShown ? Optional.of(parent) : Optional.empty(), path));

            var childPath = new ArrayList<>(path);
            childPath.add(MasterData.orEmpty(child.getName()));
            collect(child, true, List.copyOf(childPath), nodes);
        }
    }

    /** A weight of the file as a fraction of one, unrounded. */
    public static BigDecimal fraction(int weight)
    {
        return BigDecimal.valueOf(weight).divide(BigDecimal.valueOf(Classification.ONE_HUNDRED_PERCENT))
                        .stripTrailingZeros();
    }
}
