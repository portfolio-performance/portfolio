package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TaxonomyBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;

/**
 * Placing a vehicle into a class of a taxonomy, and taking it out again.
 *
 * Two of these tests are about a property rather than about the change: a PUT
 * that is sent twice must not do anything the second time, and must not ask the
 * user to save a file it did not touch.
 */
@SuppressWarnings("nls")
public class TaxonomyAssignmentHandlerTest
{
    private static final double EPS = 1e-9;

    /** counts what the model reports, because that is what makes the change visible */
    private static int[] watch(Client client)
    {
        var dirty = new int[1];
        client.addPropertyChangeListener(e -> {
            if ("dirty".equals(e.getPropertyName()))
                dirty[0]++;
        });
        return dirty;
    }

    /** the first taxonomy: equity with a growth class below it, and bonds */
    private static Taxonomy assetClasses(Client client)
    {
        return new TaxonomyBuilder() //
                        .addClassification("equity") //
                        .addClassification("bonds") //
                        .addClassification("equity", "growth") //
                        .addTo(client);
    }

    private static Account account(Client client, String name)
    {
        var account = new AccountBuilder("EUR").addTo(client);
        account.setName(name);
        return account;
    }

    private static Security security(Client client, String name)
    {
        var security = new SecurityBuilder().addTo(client);
        security.setName(name);
        return security;
    }

    private static JsonElement weightElement(double fraction)
    {
        return new JsonPrimitive(fraction);
    }

    private static String classId(Taxonomy taxonomy, String name)
    {
        return classId(taxonomy.getRoot(), name).orElseThrow();
    }

    /** a class of any depth, looked up by name - "growth" sits below "equity" */
    private static java.util.Optional<String> classId(Classification node, String name)
    {
        if (name.equals(node.getName()))
            return java.util.Optional.of(node.getId());

        return node.getChildren().stream() //
                        .map(child -> classId(child, name))
                        .filter(java.util.Optional::isPresent)
                        .findFirst()
                        .orElse(java.util.Optional.empty());
    }

    @Test
    public void testAVehicleIsPlacedAndTheCountsAroundItAreStated()
    {
        var client = new Client();
        var assets = assetClasses(client);
        var cash = account(client, "Bevi");
        var dirty = watch(client);

        var json = TaxonomyAssignmentHandler.put(client, assets.getId(), classId(assets, "bonds"), cash.getUUID(),
                        null);

        assertThat(json.get("created").getAsBoolean(), is(true));
        assertThat(json.get("changed").getAsBoolean(), is(true));
        assertThat(json.get("assignedBefore").getAsInt(), is(0));
        assertThat(json.get("assignedAfter").getAsInt(), is(1));
        assertThat(json.get("rank").getAsInt(), is(0));
        assertThat(json.get("vehicle").getAsJsonObject().get("type").getAsString(), is("cash-account"));
        assertThat(json.get("taxonomy").getAsJsonObject().get("id").getAsString(), is(assets.getId()));
        // a vehicle newly placed takes the whole of the class
        assertThat(json.get("weight").getAsDouble(), is(closeTo(1d, EPS)));

        var bonds = assets.getClassificationById(classId(assets, "bonds"));
        assertThat(bonds.getAssignments().size(), is(1));
        assertThat(bonds.getAssignments().get(0).getInvestmentVehicle().getUUID(), is(cash.getUUID()));

        // the one thing that tells the open views to agree with the change
        assertThat(dirty[0], is(1));
    }

    /** sent twice, the same request has done what it did once - and says so */
    @Test
    public void testPlacingTheSameVehicleAgainChangesNothing()
    {
        var client = new Client();
        var assets = assetClasses(client);
        var cash = account(client, "Bevi");
        var dirty = watch(client);

        var klass = classId(assets, "bonds");
        TaxonomyAssignmentHandler.put(client, assets.getId(), klass, cash.getUUID(), null);
        var again = TaxonomyAssignmentHandler.put(client, assets.getId(), klass, cash.getUUID(), null);

        assertThat(again.get("created").getAsBoolean(), is(false));
        assertThat(again.get("changed").getAsBoolean(), is(false));
        assertThat(again.get("assignedBefore").getAsInt(), is(1));
        assertThat(again.get("assignedAfter").getAsInt(), is(1));
        assertThat(assets.getClassificationById(klass).getAssignments().size(), is(1));

        // the file is not left with unsaved changes because of a restatement
        assertThat(dirty[0], is(1));
    }

    @Test
    public void testTheShareOfAClassTravelsAsAFraction()
    {
        var client = new Client();
        var assets = assetClasses(client);
        var name = security(client, "ACME");

        var klass = classId(assets, "growth");
        var json = TaxonomyAssignmentHandler.put(client, assets.getId(), klass, name.getUUID(),
                        weightElement(0.4d));

        assertThat(json.get("weight").getAsDouble(), is(closeTo(0.4d, EPS)));
        assertThat(assets.getClassificationById(klass).getAssignments().get(0).getWeight(),
                        is((int) Math.round(0.4d * Classification.ONE_HUNDRED_PERCENT)));
    }

    /** a share left out of a request is a share to keep, not one to reset */
    @Test
    public void testOmittingTheShareLeavesAnExistingOneAlone()
    {
        var client = new Client();
        var assets = assetClasses(client);
        var name = security(client, "ACME");

        var klass = classId(assets, "growth");
        TaxonomyAssignmentHandler.put(client, assets.getId(), klass, name.getUUID(), weightElement(0.4d));
        var restated = TaxonomyAssignmentHandler.put(client, assets.getId(), klass, name.getUUID(), null);

        assertThat(restated.get("changed").getAsBoolean(), is(false));
        assertThat(restated.get("weight").getAsDouble(), is(closeTo(0.4d, EPS)));
    }

    /** a new vehicle goes last, which is where the application puts it too */
    @Test
    public void testANewVehicleLandsBehindTheOnesAlreadyThere()
    {
        var client = new Client();
        var assets = assetClasses(client);
        var first = security(client, "first");
        var second = security(client, "second");

        var klass = classId(assets, "equity");
        TaxonomyAssignmentHandler.put(client, assets.getId(), klass, first.getUUID(), null);
        TaxonomyAssignmentHandler.put(client, assets.getId(), klass, second.getUUID(), null);

        var equity = assets.getClassificationById(klass);
        assertThat(equity.getAssignments().get(0).getRank(), is(0));
        assertThat(equity.getAssignments().get(1).getRank(), is(1));
        assertThat(equity.getAssignments().get(1).getInvestmentVehicle().getUUID(), is(second.getUUID()));
    }

    @Test
    public void testAnUnknownVehicleIsRefusedByNameOfTheField()
    {
        var client = new Client();
        var assets = assetClasses(client);

        try
        {
            TaxonomyAssignmentHandler.put(client, assets.getId(), classId(assets, "bonds"),
                            "00000000-0000-4000-8000-000000000000", null);
        }
        catch (ApiException e)
        {
            assertThat(e.getErrors().get(0).field(), is("vehicle"));
            assertThat(e.getErrors().get(0).message(), containsString("pp_list_instruments"));
            return;
        }
        throw new AssertionError("an unknown vehicle was placed");
    }

    /** a depot has a uuid, and cannot be placed: the refusal says so, not "no vehicle" */
    @Test
    public void testAnInvestmentAccountCannotBePlacedAnywhere()
    {
        var client = new Client();
        var assets = assetClasses(client);
        var portfolio = new Portfolio();
        portfolio.setName("Depot");
        portfolio.setReferenceAccount(account(client, "Giro"));
        client.addPortfolio(portfolio);

        try
        {
            TaxonomyAssignmentHandler.put(client, assets.getId(), classId(assets, "bonds"), portfolio.getUUID(), null);
        }
        catch (ApiException e)
        {
            assertThat(e.getErrors().get(0).field(), is("vehicle"));
            assertThat(e.getErrors().get(0).code(), is("vehicle-not-assignable"));
            return;
        }
        throw new AssertionError("an investment account was placed into a class");
    }

    @Test
    public void testAShareMustBeAShareOfOneClass()
    {
        var client = new Client();
        var assets = assetClasses(client);
        var name = security(client, "ACME");

        for (var fraction : new double[] { 0d, 1.5d })
        {
            try
            {
                TaxonomyAssignmentHandler.put(client, assets.getId(), classId(assets, "growth"), name.getUUID(),
                                weightElement(fraction));
                throw new AssertionError("a share of " + fraction + " was accepted");
            }
            catch (ApiException e)
            {
                assertThat(e.getErrors().get(0).field(), is("weight"));
            }
        }
    }

    @Test
    public void testAnUnknownClassIsRefused()
    {
        var client = new Client();
        var assets = assetClasses(client);
        var cash = account(client, "Bevi");

        try
        {
            TaxonomyAssignmentHandler.put(client, assets.getId(), "gibt-es-nicht", cash.getUUID(), null);
        }
        catch (ApiException e)
        {
            assertThat(e.getErrors().get(0).field(), is("class"));
            return;
        }
        throw new AssertionError("an unknown class was placed into");
    }

    @Test
    public void testRemovingTakesTheAssignmentOutAndRefusesTheSecondTime()
    {
        var client = new Client();
        var assets = assetClasses(client);
        var cash = account(client, "Bevi");
        var dirty = watch(client);

        var klass = classId(assets, "bonds");
        TaxonomyAssignmentHandler.put(client, assets.getId(), klass, cash.getUUID(), null);

        var removed = TaxonomyAssignmentHandler.remove(client, assets.getId(), klass, cash.getUUID());
        assertThat(removed.get("assignedBefore").getAsInt(), is(1));
        assertThat(removed.get("assignedAfter").getAsInt(), is(0));
        assertThat(removed.has("weight"), is(true));
        assertThat(assets.getClassificationById(klass).getAssignments().size(), is(0));
        assertThat(dirty[0], is(2));

        try
        {
            TaxonomyAssignmentHandler.remove(client, assets.getId(), klass, cash.getUUID());
            throw new AssertionError("a second removal went through");
        }
        catch (ApiException e)
        {
            // both things are there; only the relation between them is not
            assertThat(e.getStatus(), is(404));
            assertThat(e.getType(), is("not-assigned"));
        }
    }

    /**
     * What the user is asked about before a placement: names, not uuids, because
     * a person is reading it - and the uuid of a cash account means nothing to
     * the one who owns it.
     */
    @Test
    public void testThePromptNamesTheVehicleAndTheClass()
    {
        var client = new Client();
        var assets = assetClasses(client);
        var cash = account(client, "Bevi");

        var facts = TaxonomyAssignmentHandler.describe(client, assets.getId(), classId(assets, "bonds"),
                        cash.getUUID(), false, "Richard.portfolio");

        assertThat(facts.fileLabel(), is("Richard.portfolio"));
        assertThat(facts.subjectType(), is("cash-account"));
        assertThat(facts.subjectName(), is("Bevi"));
        assertThat(facts.targetName(), is("bonds"));
        assertThat(facts.detail(), is(assets.getName()));
    }

    /**
     * A removal of something that was never placed is refused where the user
     * would otherwise have been asked to approve it: nobody is interrupted for a
     * request that cannot be carried out.
     */
    @Test
    public void testARemovalOfSomethingNeverPlacedIsRefusedBeforeThePrompt()
    {
        var client = new Client();
        var assets = assetClasses(client);
        var cash = account(client, "Bevi");

        try
        {
            TaxonomyAssignmentHandler.describe(client, assets.getId(), classId(assets, "bonds"), cash.getUUID(), true,
                            "Richard.portfolio");
            throw new AssertionError("a removal of something never placed was described");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(404));
            assertThat(e.getType(), is("not-assigned"));
        }
    }
}
