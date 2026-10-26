package name.abuchen.portfolio.cli;

import java.util.UUID;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.Taxonomy;

/** Small taxonomies for the tests of <code>taxonomies</code> and <code>allocation</code>. */
public final class TaxonomyFixtures
{
    private TaxonomyFixtures()
    {
    }

    /** An empty taxonomy: just its root. */
    public static Taxonomy taxonomy(Client client, String name)
    {
        var taxonomy = new Taxonomy(name);
        taxonomy.setRootNode(new Classification(UUID.randomUUID().toString(), name));
        client.addTaxonomy(taxonomy);
        return taxonomy;
    }

    /** A classification under the parent; the rank is the position the app shows it at. */
    public static Classification child(Classification parent, String name, int rank)
    {
        var classification = new Classification(parent, UUID.randomUUID().toString(), name);
        classification.setRank(rank);
        parent.addChild(classification);
        return classification;
    }

    public static Classification child(Classification parent, String name)
    {
        return child(parent, name, parent.getChildren().size());
    }

    /** Percentages of the file are stored as fixed-point weights. */
    static int percent(double percent)
    {
        return (int) Math.round(percent * Classification.ONE_HUNDRED_PERCENT / 100);
    }

    public static void assign(Classification classification, InvestmentVehicle vehicle, double percent)
    {
        classification.addAssignment(new Classification.Assignment(vehicle, percent(percent)));
    }

    public static void assign(Classification classification, InvestmentVehicle vehicle)
    {
        assign(classification, vehicle, 100);
    }
}
