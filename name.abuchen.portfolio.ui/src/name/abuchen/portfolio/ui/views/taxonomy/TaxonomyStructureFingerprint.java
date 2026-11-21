package name.abuchen.portfolio.ui.views.taxonomy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Taxonomy;

/**
 * Calculates a fingerprint of the structure of a taxonomy. The structure
 * consists of all categories (id, parent, rank, weight) and all assignments
 * (category, investment vehicle, weight, rank). Names, colors, notes and
 * valuations are not part of the structure.
 * <p>
 * The {@link TaxonomyModel} builds its node tree only once. If the fingerprint
 * changes because the taxonomy was modified elsewhere (for example by an
 * import or by editing an instrument), the node tree is outdated and the view
 * must be reloaded.
 */
public final class TaxonomyStructureFingerprint
{
    private static final String SEPARATOR = "|"; //$NON-NLS-1$

    private TaxonomyStructureFingerprint()
    {
    }

    public static String of(Taxonomy taxonomy)
    {
        var lines = new ArrayList<String>();

        if (taxonomy.getRoot() != null)
            collect(taxonomy.getRoot(), null, lines);

        Collections.sort(lines);

        return hash(String.join("\n", lines)); //$NON-NLS-1$
    }

    private static void collect(Classification classification, Classification parent, List<String> lines)
    {
        var id = Objects.toString(classification.getId());

        lines.add(String.join(SEPARATOR, "C", //$NON-NLS-1$
                        id, //
                        parent == null ? "" : Objects.toString(parent.getId()), //$NON-NLS-1$
                        String.valueOf(classification.getRank()), //
                        String.valueOf(classification.getWeight())));

        for (Assignment assignment : classification.getAssignments())
        {
            var vehicle = assignment.getInvestmentVehicle();

            lines.add(String.join(SEPARATOR, "A", //$NON-NLS-1$
                            id, //
                            vehicle == null ? "" : Objects.toString(vehicle.getUUID()), //$NON-NLS-1$
                            String.valueOf(assignment.getWeight()), //
                            String.valueOf(assignment.getRank())));
        }

        for (Classification child : classification.getChildren())
            collect(child, classification, lines);
    }

    private static String hash(String value)
    {
        try
        {
            var digest = MessageDigest.getInstance("SHA-256"); //$NON-NLS-1$
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        }
        catch (NoSuchAlgorithmException e)
        {
            // every Java platform must support SHA-256
            throw new IllegalStateException(e);
        }
    }
}
