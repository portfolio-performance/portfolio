package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.junit.TaxonomyBuilder;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Taxonomy;

/**
 * Taxonomies as two shapes: the small headers, always whole, and one flat list
 * of classes that can be narrowed and paged - because the class list of a real
 * file is larger than one answer.
 */
@SuppressWarnings("nls")
public class TaxonomiesHandlerTest
{
    private static final double EPS = 1e-9;

    private static JsonObject list(Client client)
    {
        return list(client, null, null, null, null, null);
    }

    /** as the handler takes it, with the class slot left out - most tests do not narrow to one */
    private static JsonObject list(Client client, String taxonomy, String withAssignments, String withPath,
                    String cursor, String limit)
    {
        return TaxonomiesHandler.list(client, taxonomy, null, withAssignments, withPath, cursor, limit);
    }

    /** a single class of a taxonomy, asked for by id */
    private static JsonObject oneClass(Client client, String taxonomy, String klass, String withAssignments)
    {
        return TaxonomiesHandler.list(client, taxonomy, klass, withAssignments, null, null, null);
    }

    /** the first taxonomy, with equity, bonds and a growth class below equity */
    private static Taxonomy assetClasses(Client client)
    {
        return new TaxonomyBuilder() //
                        .addClassification("equity") //
                        .addClassification("bonds") //
                        .addClassification("equity", "growth") //
                        .addTo(client);
    }

    @Test
    public void testEmptyFileHasNoTaxonomies()
    {
        var json = list(new Client());
        assertThat(json.get("items").getAsJsonArray().size(), is(0));
        assertThat(json.get("classes").getAsJsonArray().size(), is(0));
        assertThat(json.has("page"), is(false));
    }

    /** the headers are all of them; the classes of every taxonomy arrive in one list */
    @Test
    public void testHeadersAndFlatClasses()
    {
        var client = new Client();
        var assets = assetClasses(client);
        var regions = new TaxonomyBuilder().addClassification("asia").addTo(client);

        var json = list(client);

        var items = json.get("items").getAsJsonArray();
        assertThat(items.size(), is(2));
        assertThat(items.get(0).getAsJsonObject().get("id").getAsString(), is(assets.getId()));
        assertThat(items.get(0).getAsJsonObject().get("classCount").getAsInt(), is(4));
        assertThat(items.get(0).getAsJsonObject().has("classes"), is(false));
        assertThat(items.get(1).getAsJsonObject().get("classCount").getAsInt(), is(2));

        var classes = json.get("classes").getAsJsonArray();
        assertThat(classes.size(), is(6));

        // a parent before its children, same rank decided by name
        assertThat(id(classes, 0), is(assets.getId()));
        assertThat(id(classes, 1), is("bonds"));
        assertThat(id(classes, 2), is("equity"));
        assertThat(id(classes, 3), is("growth"));
        assertThat(id(classes, 4), is(regions.getId()));
        assertThat(id(classes, 5), is("asia"));

        // every row names its taxonomy, so a page can be read on its own
        assertThat(classes.get(0).getAsJsonObject().get("taxonomy").getAsString(), is(assets.getId()));
        assertThat(classes.get(4).getAsJsonObject().get("taxonomy").getAsString(), is(regions.getId()));

        // the root carries the taxonomy's id and has nothing above it
        assertThat(classes.get(0).getAsJsonObject().has("parent"), is(false));
        assertThat(classes.get(1).getAsJsonObject().get("parent").getAsString(), is(assets.getId()));

        // the way down from the root is not in the row unless it is asked for
        assertThat(classById(classes, "growth").has("path"), is(false));
    }

    /** the whole way down from the root, for a reader that would rather not walk */
    @Test
    public void testThePathComesOnlyWhenAskedFor()
    {
        var client = new Client();
        assetClasses(client);

        var classes = list(client, null, null, "true", null, null).get("classes") //$NON-NLS-1$
                        .getAsJsonArray();

        var path = classById(classes, "growth").get("path").getAsJsonArray();
        assertThat(path.size(), is(3));
        assertThat(path.get(1).getAsString(), is("equity"));
        assertThat(path.get(2).getAsString(), is("growth"));
    }

    /** a target weight arrives as a fraction, the way the application shows 60 % as 0.6 */
    @Test
    public void testTargetWeightIsAFraction()
    {
        var client = new Client();
        var assets = assetClasses(client);
        assets.getClassificationById("equity").setWeight(Classification.ONE_HUNDRED_PERCENT * 60 / 100);

        var classes = list(client).get("classes").getAsJsonArray();

        assertThat(classById(classes, "equity").get("targetWeight").getAsDouble(), is(closeTo(0.6, EPS)));
        assertThat(classById(classes, "bonds").get("targetWeight").getAsDouble(), is(closeTo(0d, EPS)));
    }

    /** the vehicles are off by default, because one class can hold hundreds of them */
    @Test
    public void testAssignmentsAreOffByDefault()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.setName("ACME");

        var assets = assetClasses(client);
        assets.getClassificationById("growth")
                        .addAssignment(new Classification.Assignment(security, Classification.ONE_HUNDRED_PERCENT));
        // a place in the class that the page has to carry, because the page is
        // ordered by exactly that place
        assets.getClassificationById("growth").getAssignments().get(0).setRank(7);

        var growth = classById(list(client).get("classes").getAsJsonArray(), "growth");
        assertThat(growth.get("assignedCount").getAsInt(), is(1));
        assertThat(growth.has("assignments"), is(false));

        var with = list(client, null, "true", null, null, null).get("classes") //$NON-NLS-1$
                        .getAsJsonArray();
        var full = classById(with, "growth");
        assertThat(full.get("assignedCount").getAsInt(), is(1));

        var assignment = full.get("assignments").getAsJsonArray().get(0).getAsJsonObject();
        assertThat(assignment.get("uuid").getAsString(), is(security.getUUID()));
        assertThat(assignment.get("name").getAsString(), is("ACME"));
        assertThat(assignment.get("type").getAsString(), is("instrument"));
        assertThat(assignment.get("rank").getAsInt(), is(7));
        assertThat(assignment.get("weight").getAsDouble(), is(closeTo(1d, EPS)));
    }

    /** one taxonomy instead of all, which is how a big tree is read */
    @Test
    public void testNarrowedToOneTaxonomy()
    {
        var client = new Client();
        var assets = assetClasses(client);
        new TaxonomyBuilder().addClassification("asia").addTo(client);

        var json = list(client, assets.getId(), null, null, null, null);

        assertThat(json.get("items").getAsJsonArray().size(), is(1));
        assertThat(json.get("classes").getAsJsonArray().size(), is(4));
    }

    /** one class by id, with its vehicles: the point is that one answer holds them all */
    @Test
    public void testNarrowedToOneClassWithItsVehicles()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.setName("ACME");

        var assets = assetClasses(client);
        assets.getClassificationById("growth")
                        .addAssignment(new Classification.Assignment(security, Classification.ONE_HUNDRED_PERCENT));
        new TaxonomyBuilder().addClassification("asia").addTo(client);

        var classes = oneClass(client, null, "growth", "true").get("classes").getAsJsonArray();
        assertThat(classes.size(), is(1));

        var growth = classes.get(0).getAsJsonObject();
        assertThat(growth.get("id").getAsString(), is("growth"));
        assertThat(growth.get("assignedCount").getAsInt(), is(1));
        assertThat(growth.get("assignments").getAsJsonArray().get(0).getAsJsonObject().get("name").getAsString(),
                        is("ACME"));

        // the headers above stay whole even when the classes are narrowed to one
        var oneTree = oneClass(client, assets.getId(), "growth", null);
        assertThat(oneTree.get("classes").getAsJsonArray().size(), is(1));
        assertThat(oneTree.get("items").getAsJsonArray().size(), is(1));
    }

    /** an id nothing has, and an id several taxonomies define, are both refused */
    @Test
    public void testAClassIdOfNoUseIsRefused()
    {
        var client = new Client();
        assetClasses(client);
        var other = new TaxonomyBuilder().addClassification("growth").addTo(client);

        try
        {
            oneClass(client, null, "no-such-class", null);
            org.junit.Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            assertThat(e.getErrors().get(0).field(), is("class"));
        }

        // "growth" belongs to two taxonomies here, so it is a quarrel, not a class
        try
        {
            oneClass(client, null, "growth", null);
            org.junit.Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getErrors().get(0).code(), is("invalid-value"));
        }

        // named to one taxonomy, the very same id is one class again
        assertThat(oneClass(client, other.getId(), "growth", null).get("classes").getAsJsonArray().size(), is(1));
    }

    /** the classes are what the cursor walks, and the walk comes back in display order */
    @Test
    public void testClassesArePaged()
    {
        var client = new Client();
        var assets = assetClasses(client);
        new TaxonomyBuilder().addClassification("asia").addTo(client);

        var first = list(client, null, null, null, null, "2");
        var page = first.get("page").getAsJsonObject();
        var classes = first.get("classes").getAsJsonArray();

        assertThat(classes.size(), is(2));
        assertThat(page.get("total").getAsInt(), is(6));
        assertThat(page.get("limit").getAsInt(), is(2));
        assertThat(id(classes, 0), is(assets.getId()));
        assertThat(id(classes, 1), is("bonds"));

        // every row carries its own cursor, and every row of a page has one
        assertThat(classes.get(1).getAsJsonObject().has("cursor"), is(true));

        var second = list(client, null, null, null, page.get("nextCursor").getAsString(), "2");
        var more = second.get("classes").getAsJsonArray();
        assertThat(more.size(), is(2));
        assertThat(id(more, 0), is("equity"));
        assertThat(id(more, 1), is("growth"));

        // and the walk ends: three pages of two, then no cursor left to follow
        var third = list(client, null, null, null,
                        second.get("page").getAsJsonObject().get("nextCursor").getAsString(), "2");
        assertThat(third.get("classes").getAsJsonArray().size(), is(2));
        assertThat(third.get("page").getAsJsonObject().get("nextCursor").isJsonNull(), is(true));
    }

    @Test
    public void testUnknownTaxonomyIsAFieldError()
    {
        var client = new Client();
        assetClasses(client);

        try
        {
            list(client, "no-such-taxonomy", null, null, null, null);
            org.junit.Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            assertThat(e.getErrors().get(0).field(), is("taxonomy"));
            assertThat(e.getErrors().get(0).code(), is("invalid-value"));
        }
    }

    @Test
    public void testAFlagThatIsNoFlagIsAFieldError()
    {
        var client = new Client();
        assetClasses(client);

        try
        {
            list(client, null, "vielleicht", null, null, null);
            org.junit.Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(400));
            assertThat(e.getErrors().get(0).field(), is("withAssignments"));
            assertThat(e.getErrors().get(0).code(), is("invalid-value"));
        }
    }

    private static String id(JsonArray classes, int index)
    {
        return classes.get(index).getAsJsonObject().get("id").getAsString();
    }

    private static JsonObject classById(JsonArray classes, String id)
    {
        for (var element : classes)
        {
            var json = element.getAsJsonObject();
            if (json.get("id").getAsString().equals(id))
                return json;
        }
        throw new AssertionError("no class " + id);
    }

    /** the vehicles of one class, as the paged list they became */
    private static JsonObject vehiclesOf(Client client, String taxonomy, String klass, String cursor, String limit)
    {
        return TaxonomiesHandler.list(client, taxonomy, klass, "true", null, cursor, limit);
    }

    /**
     * One class asked for together with its vehicles: then the vehicles are the
     * many, and the class itself stays whole above them - a row of several
     * hundred vehicles is what no answer carries, and it is the class that says
     * how many there are in all.
     */
    @Test
    public void testTheVehiclesOfOneClassAreThePagedList()
    {
        var client = new Client();
        var assets = assetClasses(client);
        var growth = assets.getClassificationById("growth");

        // added in an order its ranks do not follow, so the page can only come
        // back in the order of the application's table if the rank leads the key
        var names = new String[] { "e", "d", "c", "b", "a" };
        for (var index = 0; index < names.length; index++)
        {
            var security = new SecurityBuilder().addTo(client);
            security.setName(names[index]);

            var assignment = new Classification.Assignment(security, Classification.ONE_HUNDRED_PERCENT);
            assignment.setRank(names.length - index);
            growth.addAssignment(assignment);
        }

        var first = vehiclesOf(client, assets.getId(), "growth", null, "2");

        // the class whole, without its vehicles - they are the list beside it
        var classes = first.get("classes").getAsJsonArray();
        assertThat(classes.size(), is(1));
        var row = classById(classes, "growth");
        assertThat(row.get("assignedCount").getAsInt(), is(5));
        assertThat(row.has("assignments"), is(false));

        var page = first.get("page").getAsJsonObject();
        assertThat(page.get("total").getAsInt(), is(5));
        assertThat(page.get("limit").getAsInt(), is(2));

        var assignments = first.get("assignments").getAsJsonArray();
        assertThat(assignments.size(), is(2));
        assertThat(assignments.get(0).getAsJsonObject().get("name").getAsString(), is("a"));
        assertThat(assignments.get(1).getAsJsonObject().get("name").getAsString(), is("b"));

        var second = vehiclesOf(client, assets.getId(), "growth", page.get("nextCursor").getAsString(), "2");
        var next = second.get("assignments").getAsJsonArray();
        assertThat(next.size(), is(2));
        assertThat(next.get(0).getAsJsonObject().get("name").getAsString(), is("c"));
        assertThat(second.get("page").getAsJsonObject().get("total").getAsInt(), is(5));

        var third = vehiclesOf(client, assets.getId(), "growth",
                        second.get("page").getAsJsonObject().get("nextCursor").getAsString(), "2");
        assertThat(third.get("assignments").getAsJsonArray().size(), is(1));
        assertThat(third.get("page").getAsJsonObject().has("nextCursor"), is(false));
    }

    /** a cursor of the class list is not a cursor of the vehicles of a class */
    @Test
    public void testAClassCursorIsNoUseForTheVehicles()
    {
        var client = new Client();
        var assets = assetClasses(client);
        var security = new SecurityBuilder().addTo(client);
        assets.getClassificationById("growth")
                        .addAssignment(new Classification.Assignment(security, Classification.ONE_HUNDRED_PERCENT));

        var classes = list(client, assets.getId(), null, null, null, "2");
        var cursor = classes.get("page").getAsJsonObject().get("nextCursor").getAsString();

        try
        {
            vehiclesOf(client, assets.getId(), "growth", cursor, "2");
        }
        catch (ApiException e)
        {
            assertThat(e.getErrors().get(0).field(), is("cursor"));
            return;
        }
        throw new AssertionError("a cursor of the classes paged the vehicles");
    }
}
