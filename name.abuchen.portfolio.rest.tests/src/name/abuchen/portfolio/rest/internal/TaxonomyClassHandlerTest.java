package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import name.abuchen.portfolio.junit.TaxonomyBuilder;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Taxonomy;

/**
 * Setting the target weight of a class - the one number this file keeps that
 * says what a class should be worth.
 *
 * The interesting cases are at the edges of the number itself: zero is allowed,
 * more than a whole is stored as given because one tree's classes need not add
 * up to a hundred percent, and a negative is refused because it can only come
 * from the old dialog that took one. And, as everywhere in this API, a request
 * that changed nothing must not leave the file waiting to be saved.
 */
@SuppressWarnings("nls")
public class TaxonomyClassHandlerTest
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

    @Test
    public void aTargetWeightTravelsAsTheFractionTheReadGivesBack()
    {
        var client = new Client();
        var assets = assetClasses(client);
        var growth = assets.getClassificationById(classId(assets, "growth"));
        var dirty = watch(client);

        var json = TaxonomyClassHandler.patch(client, assets.getId(), classId(assets, "growth"),
                        new JsonPrimitive(0.6));

        assertThat(json.get("changed").getAsBoolean(), is(true));
        assertThat(json.get("targetWeight").getAsDouble(), is(closeTo(0.6, EPS)));
        // the model's own scale, and what the read side turned it into
        assertThat(growth.getWeight(), is((int) Math.round(0.6 * Classification.ONE_HUNDRED_PERCENT)));
        assertThat(dirty[0], is(1));
    }

    @Test
    public void theWeightBeforeTravelsAlongSoACallerCanSayWhatItChanged()
    {
        var client = new Client();
        var assets = assetClasses(client);
        var bonds = assets.getClassificationById(classId(assets, "bonds"));
        bonds.setWeight(Classification.ONE_HUNDRED_PERCENT);

        var json = TaxonomyClassHandler.patch(client, assets.getId(), classId(assets, "bonds"),
                        new JsonPrimitive(0.25));

        assertThat(json.get("previousTargetWeight").getAsDouble(), is(closeTo(1.0, EPS)));
        assertThat(json.get("targetWeight").getAsDouble(), is(closeTo(0.25, EPS)));
    }

    @Test
    public void theSameWeightTwiceChangesNothingAndStaysUnaskedToBeSaved()
    {
        var client = new Client();
        var assets = assetClasses(client);
        var first = TaxonomyClassHandler.patch(client, assets.getId(), classId(assets, "equity"),
                        new JsonPrimitive(0.5));
        var dirty = watch(client);

        var second = TaxonomyClassHandler.patch(client, assets.getId(), classId(assets, "equity"),
                        new JsonPrimitive(0.5));

        assertThat(first.get("changed").getAsBoolean(), is(true));
        assertThat(second.get("changed").getAsBoolean(), is(false));
        assertThat(second.get("targetWeight").getAsDouble(), is(closeTo(0.5, EPS)));
        assertThat(dirty[0], is(0));
    }

    @Test
    public void zeroIsAGenuineAnswerAndMoreThanAWholeIsStoredAsGiven()
    {
        var client = new Client();
        var assets = assetClasses(client);
        var id = classId(assets, "bonds");

        var nothing = TaxonomyClassHandler.patch(client, assets.getId(), id, new JsonPrimitive(0d));
        var satellite = TaxonomyClassHandler.patch(client, assets.getId(), id, new JsonPrimitive(1.5));

        assertThat(nothing.get("changed").getAsBoolean(), is(true));
        assertThat(nothing.get("targetWeight").getAsDouble(), is(closeTo(0d, EPS)));
        // the classes of one tree need not add up to a hundred percent - the
        // application stores what its dialog was given, and so does this
        assertThat(satellite.get("targetWeight").getAsDouble(), is(closeTo(1.5, EPS)));
    }

    @Test
    public void aNegativeWeightIsRefusedBecauseOnlyTheOldDialogAcceptedOne()
    {
        var client = new Client();
        var assets = assetClasses(client);

        var refusal = capture(() -> TaxonomyClassHandler.patch(client, assets.getId(), classId(assets, "bonds"),
                        new JsonPrimitive(-0.1)));

        assertThat(refusal.getStatus(), is(400));
        assertThat(refusal.getErrors().get(0).field(), is("targetWeight"));
        assertThat(refusal.getErrors().get(0).code(), is("out-of-range"));
    }

    @Test
    public void aWeightThatIsNotANumberIsRefusedNamingItsField()
    {
        var client = new Client();
        var assets = assetClasses(client);

        var absent = capture(() -> TaxonomyClassHandler.patch(client, assets.getId(), classId(assets, "bonds"),
                        null));
        var text = capture(() -> TaxonomyClassHandler.patch(client, assets.getId(), classId(assets, "bonds"),
                        new JsonPrimitive("sixty percent")));
        var nul = capture(() -> TaxonomyClassHandler.patch(client, assets.getId(), classId(assets, "bonds"),
                        JsonNull.INSTANCE));

        assertThat(absent.getStatus(), is(400));
        assertThat(absent.getErrors().get(0).code(), is("required"));
        assertThat(text.getErrors().get(0).code(), is("invalid-type"));
        assertThat(nul.getErrors().get(0).code(), is("required"));
    }

    @Test
    public void aClassOrATreeTheFileDoesNotHaveIsRefusedBeforeAnyoneIsAsked()
    {
        var client = new Client();
        var assets = assetClasses(client);

        var noTree = capture(() -> TaxonomyClassHandler.describe(client, "no-such-tree", classId(assets, "bonds"),
                        new JsonPrimitive(0.5), "f"));
        var noClass = capture(() -> TaxonomyClassHandler.describe(client, assets.getId(), "no-such-class",
                        new JsonPrimitive(0.5), "f"));

        assertThat(noTree.getStatus(), is(400));
        assertThat(noClass.getStatus(), is(400));
    }

    @Test
    public void thePromptNamesTheClassAndTheTreeItBelongsTo()
    {
        var client = new Client();
        var assets = assetClasses(client);

        var facts = TaxonomyClassHandler.describe(client, assets.getId(), classId(assets, "growth"),
                        new JsonPrimitive(0.3), "My File");

        assertThat(facts.subjectType(), is("classification"));
        assertThat(facts.subjectName(), is("growth"));
        assertThat(facts.targetName(), is(assets.getName()));
        assertThat(facts.fileLabel(), is("My File"));
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
