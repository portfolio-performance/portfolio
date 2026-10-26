package name.abuchen.portfolio.cli.apply;

import static name.abuchen.portfolio.cli.TaxonomyFixtures.assign;
import static name.abuchen.portfolio.cli.TaxonomyFixtures.child;
import static name.abuchen.portfolio.cli.TaxonomyFixtures.taxonomy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonElement;

import name.abuchen.portfolio.cli.CliFixture;
import name.abuchen.portfolio.cli.CliRun;
import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Taxonomy;

/**
 * <code>taxonomy.assign</code>: replaces one instrument's whole assignment
 * within one taxonomy.
 * <p>
 * The fixture is worth 1,200 on 2026-03-01: Alpha (10 shares at 120) and Beta
 * (no shares). The Classes taxonomy has Equity (with Europe and America) and
 * Bonds; Regions has North and South. Alpha sits in Europe.
 */
public class TaxonomyAssignTest
{
    private static final String DATE = "2026-03-01";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final CliFixture cli = new CliFixture(folder);

    private Security alpha;
    private Security beta;
    private Taxonomy classes;
    private Classification equity;
    private Classification europe;
    private Classification america;
    private Classification bonds;
    private Taxonomy regions;
    private Classification north;

    @Before
    public void setUp() throws IOException
    {
        var client = new Client();

        alpha = new SecurityBuilder().addTo(client);
        alpha.setName("Alpha");
        alpha.addPrice(new SecurityPrice(LocalDate.parse(DATE), PortfolioBuilder.quoteOf(120)));

        beta = new SecurityBuilder().addTo(client);
        beta.setName("Beta");

        var cash = new AccountBuilder().deposit_("2026-01-01", PortfolioBuilder.amountOf(100_000)).addTo(client);
        new PortfolioBuilder(cash).buyPrice(alpha, "2026-01-02", 10, 100).addTo(client);

        classes = taxonomy(client, "Classes");
        equity = child(classes.getRoot(), "Equity");
        europe = child(equity, "Europe");
        america = child(equity, "America");
        bonds = child(classes.getRoot(), "Bonds");

        regions = taxonomy(client, "Regions");
        north = child(regions.getRoot(), "North");
        child(regions.getRoot(), "South");

        assign(europe, alpha);

        cli.save(client);
    }

    private static String assignOp(Taxonomy taxonomy, Security instrument, String... assignments)
    {
        return assignOp(taxonomy.getId(), instrument.getUUID(), assignments);
    }

    private static String assignOp(String taxonomy, String instrument, String... assignments)
    {
        return "{\"operation\":\"taxonomy.assign\",\"taxonomy\":\"" + taxonomy + "\",\"instrument\":\"" + instrument
                        + "\",\"assignments\":[" + String.join(",", assignments) + "]}";
    }

    private static String to(Classification classification, String weight)
    {
        return "{\"classification\":\"" + classification.getId() + "\",\"weight\":" + weight + "}";
    }

    /** What the file now says about the instrument: classification name to weight in percent. */
    private List<String> placements(Taxonomy taxonomy, Security instrument) throws IOException
    {
        var client = cli.reload();
        var reloaded = client.getTaxonomies().stream().filter(t -> t.getId().equals(taxonomy.getId())).findFirst()
                        .orElseThrow();
        var placements = new ArrayList<String>();
        reloaded.getRoot().getTreeElements().forEach(c -> c.getAssignments().stream()
                        .filter(a -> a.getInvestmentVehicle().getUUID().equals(instrument.getUUID()))
                        .forEach(a -> placements.add(c.getName() + "=" + BigDecimal.valueOf(a.getWeight(), 2)
                                        .stripTrailingZeros().toPlainString())));
        return placements;
    }

    private CliRun allocation(Taxonomy taxonomy)
    {
        return cli.run("allocation", "--taxonomy", taxonomy.getId(), "--date", DATE,
                        "--currency", "EUR");
    }

    private static BigDecimal valueOf(CliRun allocation, String name)
    {
        var items = allocation.items();
        return StreamSupport.stream(items.spliterator(), false).map(JsonElement::getAsJsonObject)
                        .filter(r -> r.get("name").getAsString().equals(name)).findFirst().orElseThrow().get("value")
                        .getAsBigDecimal();
    }

    @Test
    public void assignsAnInstrumentToSeveralClassificationsAndAllocationReflectsIt() throws IOException
    {
        var run = cli.apply(assignOp(regions, alpha, to(north, "0.25"), "{\"classification\":\"" + regions.getRoot()
                        .getChildren().get(1).getId() + "\",\"weight\":0.75}"));

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(placements(regions, alpha), containsInAnyOrder("North=25", "South=75"));

        var allocation = allocation(regions);
        assertThat(valueOf(allocation, "North").compareTo(new BigDecimal("300")), is(0));
        assertThat(valueOf(allocation, "South").compareTo(new BigDecimal("900")), is(0));
    }

    @Test
    public void reassignsReplacingTheWholeAssignmentAndLeavesOtherInstrumentsAlone() throws IOException
    {
        // Beta is placed in Bonds; reassigning Alpha must not touch it
        var seed = cli.apply(assignOp(classes, beta, to(bonds, "0.5")));
        assertThat(seed.stdout, seed.exitCode, is(0));

        var run = cli.apply(assignOp(classes, alpha, to(america, "0.4"), to(bonds, "0.6")));

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(placements(classes, alpha), containsInAnyOrder("America=40", "Bonds=60"));
        assertThat(placements(classes, beta), contains("Bonds=50"));

        var allocation = allocation(classes);
        assertThat(valueOf(allocation, "Europe").compareTo(BigDecimal.ZERO), is(0));
        assertThat(valueOf(allocation, "America").compareTo(new BigDecimal("480")), is(0));
        assertThat(valueOf(allocation, "Bonds").compareTo(new BigDecimal("720")), is(0));
    }

    @Test
    public void reportsTheAssignmentBeforeAndAfterAsOneUpdatedChange() throws IOException
    {
        var run = cli.apply(assignOp(classes, alpha, to(america, "0.4"), to(bonds, "0.6")));

        assertThat(run.stdout, run.exitCode, is(0));

        var updated = run.result(0).getAsJsonArray("updated");
        assertThat(updated.size(), is(1));
        var change = updated.get(0).getAsJsonObject();
        assertThat(change.get("entity").getAsString(), is("taxonomy-assignment"));
        assertThat(change.get("uuid").getAsString(), is(alpha.getUUID()));

        var before = change.getAsJsonObject("before").getAsJsonArray("assignments");
        assertThat(before.size(), is(1));
        assertThat(before.get(0).getAsJsonObject().get("classification").getAsString(), is(europe.getId()));
        assertThat(before.get(0).getAsJsonObject().get("weight").getAsBigDecimal().compareTo(BigDecimal.ONE), is(0));

        var after = change.getAsJsonObject("after").getAsJsonArray("assignments");
        assertThat(after.size(), is(2));
        assertThat(after.get(0).getAsJsonObject().get("classification").getAsString(), is(america.getId()));
        assertThat(after.get(0).getAsJsonObject().get("weight").getAsBigDecimal().compareTo(new BigDecimal("0.4")),
                        is(0));
    }

    @Test
    public void anEmptyListRemovesTheInstrumentFromTheTaxonomyOnly() throws IOException
    {
        var seed = cli.apply(assignOp(regions, alpha, to(north, "1")));
        assertThat(seed.stdout, seed.exitCode, is(0));

        var run = cli.apply(assignOp(classes, alpha));

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(placements(classes, alpha), is(empty()));
        assertThat(placements(regions, alpha), contains("North=100"));
        assertThat(valueOf(allocation(classes), "Europe").compareTo(BigDecimal.ZERO), is(0));
    }

    @Test
    public void aWeightOfExactlyOneHundredPercentInTotalIsAccepted() throws IOException
    {
        var run = cli.apply(assignOp(classes, beta, to(europe, "0.3333"), to(america, "0.3333"), to(bonds, "0.3334")));

        assertThat(run.stdout, run.exitCode, is(0));
        assertThat(placements(classes, beta), containsInAnyOrder("Europe=33.33", "America=33.33", "Bonds=33.34"));
    }

    @Test
    public void refusesNonPositiveWeightsAndWeightsFinerThanTheFileKeeps() throws IOException
    {
        var before = Files.readAllBytes(cli.file().toPath());

        var run = cli.apply(assignOp(classes, beta, to(europe, "0"), to(bonds, "-0.1")),
                        assignOp(classes, alpha, to(europe, "0.00001")),
                        assignOp(regions, beta,
                                        "{\"classification\":\"" + north.getId() + "\",\"weight\":\"half\"}"),
                        assignOp(regions, alpha, "{\"classification\":\"" + north.getId() + "\"}"));

        assertThat(run.exitCode, is(1));
        assertThat(run.failureKeys(),
                        containsInAnyOrder("0:assignments[0].weight:not-positive",
                                        "0:assignments[1].weight:not-positive",
                                        "1:assignments[0].weight:invalid-amount",
                                        "2:assignments[0].weight:invalid-type",
                                        "3:assignments[0].weight:required"));
        assertThat(Files.readAllBytes(cli.file().toPath()), is(before));
    }

    @Test
    public void refusesARepeatedClassificationOnTheRepeat() throws IOException
    {
        var run = cli.apply(assignOp(classes, beta, to(europe, "0.2"), to(bonds, "0.3"), to(europe, "0.1")));

        assertThat(run.exitCode, is(1));
        assertThat(run.failureKeys(), contains("0:assignments[2].classification:duplicate-classification"));
    }

    @Test
    public void refusesWeightsThatTotalMoreThanOneHundredPercent() throws IOException
    {
        var run = cli.apply(assignOp(classes, beta, to(europe, "0.6"), to(bonds, "0.4001")));

        assertThat(run.exitCode, is(1));
        assertThat(run.failureKeys(), contains("0:assignments:weight-total-exceeded"));
    }

    @Test
    public void refusesAClassificationThatHasChildren() throws IOException
    {
        var run = cli.apply(assignOp(classes, beta, to(equity, "0.5"), to(bonds, "0.5")));

        assertThat(run.exitCode, is(1));
        assertThat(run.failureKeys(), contains("0:assignments[0].classification:not-a-leaf"));
    }

    @Test
    public void refusesAnUnknownTaxonomyInstrumentOrClassificationAndOneOfAnotherTaxonomy() throws IOException
    {
        var unknown = "00000000-0000-0000-0000-000000000000";

        var run = cli.apply(assignOp(unknown, beta.getUUID(), to(bonds, "1")),
                        assignOp(classes.getId(), unknown, to(bonds, "1")),
                        assignOp(classes.getId(), beta.getUUID(),
                                        "{\"classification\":\"" + unknown + "\",\"weight\":1}"),
                        assignOp(classes, alpha, to(north, "1")),
                        assignOp(regions.getId(), beta.getUUID(),
                                        "{\"classification\":\"" + regions.getRoot().getId() + "\",\"weight\":1}"));

        assertThat(run.exitCode, is(1));
        assertThat(run.failureKeys(),
                        containsInAnyOrder("0:taxonomy:reference-not-found", "1:instrument:reference-not-found",
                                        "2:assignments[0].classification:reference-not-found",
                                        "3:assignments[0].classification:reference-not-found",
                                        "4:assignments[0].classification:reference-not-found"));
    }

    @Test
    public void refusesMissingOrMistypedMembers() throws IOException
    {
        var run = cli.apply("{\"operation\":\"taxonomy.assign\"}",
                        "{\"operation\":\"taxonomy.assign\",\"taxonomy\":7,\"instrument\":\"" + beta.getUUID()
                                        + "\",\"assignments\":{}}",
                        "{\"operation\":\"taxonomy.assign\",\"taxonomy\":\"" + classes.getId() + "\",\"instrument\":\""
                                        + beta.getUUID() + "\",\"assignments\":[1],\"colour\":\"red\"}");

        assertThat(run.exitCode, is(1));
        assertThat(run.failureKeys(),
                        containsInAnyOrder("0:taxonomy:required", "0:instrument:required",
                                        "0:assignments:required", "1:taxonomy:invalid-type",
                                        "1:assignments:invalid-type", "2:assignments[0]:invalid-type",
                                        "2:colour:unknown-field"));
    }

    @Test
    public void aFailingAssignmentLeavesTheFileAndOtherOperationsOfTheBatchUnapplied() throws IOException
    {
        var before = Files.readAllBytes(cli.file().toPath());

        var run = cli.apply(assignOp(classes, beta, to(bonds, "1")), assignOp(classes, alpha, to(equity, "1")));

        assertThat(run.exitCode, is(1));
        assertThat(run.failureKeys(), contains("1:assignments[0].classification:not-a-leaf"));
        assertThat(Files.readAllBytes(cli.file().toPath()), is(before));
    }

    @Test
    public void refusesASecondAssignmentOfTheSameInstrumentInTheSameTaxonomyButNotAnotherTaxonomy()
                    throws IOException
    {
        var run = cli.apply(assignOp(classes, beta, to(bonds, "1")), assignOp(classes, beta, to(europe, "1")),
                        assignOp(regions, beta, to(north, "1")),
                        "{\"operation\":\"instrument.update\",\"uuid\":\"" + beta.getUUID() + "\",\"note\":\"x\"}");

        assertThat(run.exitCode, is(1));
        assertThat(run.failureKeys(), contains("1:instrument:duplicate-target"));
    }
}
