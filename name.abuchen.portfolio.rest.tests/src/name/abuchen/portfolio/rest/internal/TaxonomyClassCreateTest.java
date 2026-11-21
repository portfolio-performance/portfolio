package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import name.abuchen.portfolio.junit.TaxonomyBuilder;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Taxonomy;

/**
 * Bringing one class into being - the half of the taxonomy writes that
 * pp_update_class could not do: that one only moves the target weight of a
 * class that already has an id.
 *
 * The cases worth having: where a class without a parent lands (the root, like
 * the application's own dialog), what it starts at (a whole, like a fresh class
 * of the application), and the one refusal that keeps the file a work of art -
 * two classes of one name under one parent, which only a caller that went and
 * looked could ever tell apart.
 */
@SuppressWarnings("nls")
public class TaxonomyClassCreateTest
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

    /** the file's asset tree: equity with a growth class below it, and bonds */
    private static Taxonomy assetClasses(Client client)
    {
        return new TaxonomyBuilder() //
                        .addClassification("equity") //
                        .addClassification("bonds") //
                        .addClassification("equity", "growth") //
                        .addTo(client);
    }

    private static String classId(Taxonomy taxonomy, String name)
    {
        return taxonomy.getAllClassifications().stream() //
                        .filter(c -> c.getName().equals(name)) //
                        .findFirst().orElseThrow().getId();
    }

    /** the class block of an answer, where id and parent travel */
    private static JsonObject cls(JsonObject answer)
    {
        return answer.getAsJsonObject("class");
    }

    private static JsonObject body(String... keyValues)
    {
        var json = new JsonObject();
        for (int ii = 0; ii < keyValues.length; ii += 2)
        {
            var value = keyValues[ii + 1];
            if (value.startsWith("#"))
                json.add(keyValues[ii], new JsonPrimitive(Double.parseDouble(value.substring(1))));
            else
                json.addProperty(keyValues[ii], value);
        }
        return json;
    }

    @Test
    public void aFreshClassHangsUnderTheRootAndStartsAtAWhole()
    {
        var client = new Client();
        var assets = assetClasses(client);
        var dirty = watch(client);

        var json = TaxonomyClassHandler.create(client, assets.getId(), body("name", "Walmart"));

        assertThat(json.get("created").getAsBoolean(), is(true));
        assertThat(json.get("changed").getAsBoolean(), is(true));
        assertThat(cls(json).get("parent").getAsString(), is(assets.getRoot().getId()));
        assertThat(json.get("targetWeight").getAsDouble(), is(closeTo(1.0, EPS)));

        // the id in the answer is the id the model carries, found the way the
        // read side finds it - so pp_assign_vehicle and pp_update_class accept
        // this class at once
        var id = cls(json).get("id").getAsString();
        var found = assets.getClassificationById(id);
        assertThat("readable by id at once", found != null, is(true));
        assertThat(found.getName(), is("Walmart"));
        assertThat(dirty[0], is(1));
    }

    @Test
    public void aParentPutsItAnywhereInTheTree()
    {
        var client = new Client();
        var assets = assetClasses(client);

        var json = TaxonomyClassHandler.create(client, assets.getId(),
                        body("name", "Growth And Value", "parent", classId(assets, "equity")));

        assertThat(cls(json).get("parent").getAsString(), is(classId(assets, "equity")));
        var equity = assets.getClassificationById(classId(assets, "equity"));
        assertThat(equity.getChildren().stream() //
                        .anyMatch(c -> "Growth And Value".equals(c.getName())), is(true));
    }

    @Test
    public void aTargetWeightTravelsAsTheFractionTheReadGivesBack()
    {
        var client = new Client();
        var assets = assetClasses(client);

        var json = TaxonomyClassHandler.create(client, assets.getId(), body("name", "Satellite", "targetWeight", "#0.25"));

        assertThat(json.get("targetWeight").getAsDouble(), is(closeTo(0.25, EPS)));
        var created = assets.getClassificationById(cls(json).get("id").getAsString());
        assertThat(created.getWeight(), is((int) Math.round(0.25 * Classification.ONE_HUNDRED_PERCENT)));
    }

    @Test
    public void aNameTheParentAlreadyCarriesIsRefusedAndNothingGrows()
    {
        var client = new Client();
        var assets = assetClasses(client);

        var twice = capture(() -> TaxonomyClassHandler.create(client, assets.getId(), body("name", "bonds")));
        assertThat(twice.getStatus(), is(409));
        assertThat(twice.getType(), is("name-taken"));
        assertThat(twice.getErrors().get(0).field(), is("name"));

        // the same name under another parent is ordinary: "growth" hangs under
        // "equity", and a "growth" beside the root is a different class
        TaxonomyClassHandler.create(client, assets.getId(), body("name", "growth"));
        assertThat(assets.getRoot().getChildren().stream() //
                        .anyMatch(c -> "growth".equals(c.getName())), is(true));
    }

    @Test
    public void aNameIsTheOneThingThatCannotBeLeftOut()
    {
        var client = new Client();
        var assets = assetClasses(client);

        var noName = capture(() -> TaxonomyClassHandler.create(client, assets.getId(), new JsonObject()));
        assertThat(noName.getStatus(), is(400));
        assertThat(noName.getErrors().get(0).field(), is("name"));
        assertThat(noName.getErrors().get(0).code(), is("required"));

        var blank = capture(() -> TaxonomyClassHandler.create(client, assets.getId(), body("name", "   ")));
        assertThat(blank.getStatus(), is(400));
    }

    @Test
    public void anUnknownParentIsRefusedOnItsOwnField()
    {
        var client = new Client();
        var assets = assetClasses(client);

        var refusal = capture(() -> TaxonomyClassHandler.create(client, assets.getId(),
                        body("name", "Wherever", "parent", "no-such-class")));
        assertThat(refusal.getStatus(), is(400));
        assertThat(refusal.getErrors().get(0).field(), is("parent"));
    }

    @Test
    public void theNumberFollowsTheRulesOfTheUpdateItMatches()
    {
        var client = new Client();
        var assets = assetClasses(client);

        var negative = capture(() -> TaxonomyClassHandler.create(client, assets.getId(),
                        body("name", "Old File", "targetWeight", "#-1")));
        assertThat(negative.getStatus(), is(400));
        assertThat(negative.getErrors().get(0).field(), is("targetWeight"));

        var overWhole = TaxonomyClassHandler.create(client, assets.getId(),
                        body("name", "Satellite 150", "targetWeight", "#1.5"));
        assertThat(overWhole.get("targetWeight").getAsDouble(), is(closeTo(1.5, EPS)));
    }

    @Test
    public void thePromptNamesTheFreshClassAndTheTreeItJoins()
    {
        var client = new Client();
        var assets = assetClasses(client);

        var facts = TaxonomyClassHandler.describeCreate(client, assets.getId(), body("name", "  SpaceX  "),
                        "My File");

        assertThat(facts.subjectType(), is("classification"));
        assertThat(facts.subjectName(), is("SpaceX"));
        assertThat(facts.targetName(), is(assets.getName()));
        assertThat(facts.fileLabel(), is("My File"));
    }

    @Test
    public void theRefusalComesBeforeThePromptWould()
    {
        var client = new Client();
        var assets = assetClasses(client);

        // describe asks everything create will need, including the sibling
        // check: a user is never offered a class the file cannot take
        var twice = capture(() -> TaxonomyClassHandler.describeCreate(client, assets.getId(), body("name", "bonds"),
                        "My File"));
        assertThat(twice.getStatus(), is(409));
    }

    private static ApiException capture(Runnable call)
    {
        try
        {
            call.run();
        }
        catch (ApiException e)
        {
            return e;
        }
        throw new AssertionError("expected a refusal, but the call went through");
    }
}
