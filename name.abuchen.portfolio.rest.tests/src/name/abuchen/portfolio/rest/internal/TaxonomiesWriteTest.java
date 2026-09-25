package name.abuchen.portfolio.rest.internal;

import static name.abuchen.portfolio.rest.internal.TransactionFixture.assertError;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.body;
import static name.abuchen.portfolio.rest.internal.TransactionFixture.json;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.junit.TaxonomyBuilder;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Taxonomy;

/** Taxonomy, category and assignment writes */
@SuppressWarnings("nls")
public class TaxonomiesWriteTest
{
    private TransactionFixture f;
    private IdempotencyIndex idempotency;
    private Taxonomy taxonomy;

    @Before
    public void setUp()
    {
        f = new TransactionFixture();
        idempotency = new IdempotencyIndex();
        taxonomy = new TaxonomyBuilder().addClassification("equity").addClassification("equity", "europe")
                        .addClassification("bonds").addTo(f.client);
        taxonomy.getClassificationById("equity").setWeight(6000);
        taxonomy.getClassificationById("bonds").setWeight(3000);
        f.file.setDirty(false);
    }

    @After
    public void tearDown() throws Exception
    {
        f.dispose();
    }

    private Classification category(String id)
    {
        return taxonomy.getClassificationById(id);
    }

    private static ApiException fails(Runnable runnable)
    {
        try
        {
            runnable.run();
            Assert.fail("expected ApiException");
            return null;
        }
        catch (ApiException e)
        {
            return e;
        }
    }

    private JsonObject assign(String category, String vehicle, String body)
    {
        return TaxonomiesHandler.putAssignment(f.context(false, null), taxonomy.getId(), category, vehicle,
                        json(body)).entity();
    }

    // taxonomies

    @Test
    public void testCreateTaxonomyWithRoot()
    {
        var result = TaxonomiesHandler.create(f.context(false, null), idempotency, json("{'name':'Regions'}"));

        var created = f.client.getTaxonomy(result.entity().get("id").getAsString());
        assertThat(created.getName(), is("Regions"));
        assertThat(created.getRoot().getName(), is("Regions"));
        assertThat(created.getRoot().getId() != null, is(true));
        assertThat(result.entity().getAsJsonArray("categories").size(), is(0));
        assertThat(f.file.isDirty(), is(true));
    }

    @Test
    public void testCreateTaxonomyValidationAndDryRun()
    {
        assertError(fails(() -> TaxonomiesHandler.create(f.context(false, null), idempotency, json("{'x':1}"))),
                        "name", "required");

        var dry = TaxonomiesHandler.create(f.context(true, null), idempotency, json("{'name':'Preview'}"));
        assertThat(dry.entity().get("dryRun").getAsBoolean(), is(true));
        assertThat(f.client.getTaxonomies().size(), is(1));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testRenameTaxonomy()
    {
        TaxonomiesHandler.rename(f.context(true, null), taxonomy.getId(), json("{'name':'Preview'}"));
        assertThat(f.file.isDirty(), is(false));

        var noop = TaxonomiesHandler.rename(f.context(false, null), taxonomy.getId(),
                        json("{'name':'" + taxonomy.getName() + "'}"));
        assertThat(noop.changed(), is(false));
        assertThat(f.file.isDirty(), is(false));

        TaxonomiesHandler.rename(f.context(false, null), taxonomy.getId(), json("{'name':'Asset Classes'}"));
        assertThat(taxonomy.getName(), is("Asset Classes"));
        assertThat(f.file.isDirty(), is(true));
    }

    @Test
    public void testDeleteTaxonomy()
    {
        var preview = TaxonomiesHandler.delete(f.context(true, null), taxonomy.getId());
        assertThat(preview.getAsJsonArray("removed").size(), is(1));
        assertThat(f.client.getTaxonomies().size(), is(1));

        TaxonomiesHandler.delete(f.context(false, null), taxonomy.getId());
        assertThat(f.client.getTaxonomies(), is(empty()));
        assertThat(f.file.isDirty(), is(true));
    }

    // categories

    @Test
    public void testCreateTopLevelCategoryTakesTheRemainingWeight()
    {
        var result = TaxonomiesHandler.createClassification(f.context(false, null), idempotency, taxonomy.getId(),
                        json("{'name':'Cash','color':'#aabbcc','note':'n'}"));

        var id = result.entity().get("id").getAsString();
        var cash = category(id);
        assertThat(cash.getParent(), is(taxonomy.getRoot()));
        assertThat(taxonomy.getRoot().getChildren().contains(cash), is(true));
        assertThat(cash.getWeight(), is(1000)); // 100 % - 60 % - 30 %
        assertThat(cash.getColor(), is("#aabbcc"));
        assertThat(cash.getNote(), is("n"));
        assertThat(result.entity().has("parent"), is(false));
        assertThat(result.entity().get("weight").getAsInt(), is(10));
        assertThat(f.file.isDirty(), is(true));
    }

    @Test
    public void testCreateSubcategory()
    {
        var result = TaxonomiesHandler.createClassification(f.context(false, null), idempotency, taxonomy.getId(),
                        json("{'name':'Asia','parent':'equity','weight':'25.5'}"));

        var asia = category(result.entity().get("id").getAsString());
        assertThat(asia.getParent(), is(category("equity")));
        assertThat(category("equity").getChildren(), contains(category("europe"), asia));
        assertThat(asia.getWeight(), is(2550));
        assertThat(asia.getRank(), is(1));
        assertThat(asia.getColor().startsWith("#"), is(true));
        assertThat(result.entity().get("parent").getAsString(), is("equity"));
    }

    @Test
    public void testCreateCategoryValidationAndDryRun()
    {
        var e = fails(() -> TaxonomiesHandler.createClassification(f.context(false, null), idempotency,
                        taxonomy.getId(), json("{'parent':'nope','color':'red','weight':'101','x':1}")));
        assertError(e, "name", "required");
        assertError(e, "parent", "unknown-reference");
        assertError(e, "color", "invalid-value");
        assertError(e, "weight", "invalid-value");
        assertError(e, "x", "unknown-field");

        var count = taxonomy.getAllClassifications().size();
        var dry = TaxonomiesHandler.createClassification(f.context(true, null), idempotency, taxonomy.getId(),
                        json("{'name':'Preview'}"));
        assertThat(dry.entity().get("dryRun").getAsBoolean(), is(true));
        assertThat(taxonomy.getAllClassifications().size(), is(count));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testPatchAndMoveCategory()
    {
        var result = TaxonomiesHandler.patchClassification(f.context(false, null), taxonomy.getId(), "europe",
                        json("{'parent':null,'name':'Europe','color':'#010203','weight':'5','note':'x'}"));

        var europe = category("europe");
        assertThat(result.changed(), is(true));
        assertThat(europe.getParent(), is(taxonomy.getRoot()));
        assertThat(taxonomy.getRoot().getChildren().contains(europe), is(true));
        assertThat(category("equity").getChildren(), is(empty()));
        assertThat(europe.getName(), is("Europe"));
        assertThat(europe.getColor(), is("#010203"));
        assertThat(europe.getWeight(), is(500));
        assertThat(europe.getNote(), is("x"));
        assertThat(f.file.isDirty(), is(true));
    }

    @Test
    public void testCategoryCannotMoveBelowItself()
    {
        var e = fails(() -> TaxonomiesHandler.patchClassification(f.context(false, null), taxonomy.getId(), "equity",
                        json("{'parent':'europe'}")));
        assertError(e, "parent", "invalid-value");

        e = fails(() -> TaxonomiesHandler.patchClassification(f.context(false, null), taxonomy.getId(), "equity",
                        json("{'parent':'equity','name':null,'color':null}")));
        assertError(e, "parent", "invalid-value");
        assertError(e, "name", "required");
        assertError(e, "color", "required");
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testPatchCategoryDryRunAndNoOp()
    {
        var dry = TaxonomiesHandler.patchClassification(f.context(true, null), taxonomy.getId(), "europe",
                        json("{'parent':'bonds','name':'Renamed'}"));
        assertThat(dry.entity().get("parent").getAsString(), is("bonds"));
        assertThat(dry.entity().get("name").getAsString(), is("Renamed"));
        assertThat(category("europe").getParent(), is(category("equity")));
        assertThat(category("europe").getName(), is("europe"));

        var noop = TaxonomiesHandler.patchClassification(f.context(false, null), taxonomy.getId(), "europe",
                        json("{'parent':'equity','name':'europe'}"));
        assertThat(noop.changed(), is(false));
        assertThat(f.file.isDirty(), is(false));
    }

    @Test
    public void testDeleteCategoryNeedsCascadeWhenNotEmpty()
    {
        assign("europe", f.eurSecurity.getUUID(), "{}");
        f.file.setDirty(false);

        var e = fails(() -> TaxonomiesHandler.deleteClassification(f.context(false, null), taxonomy.getId(), "equity",
                        null));
        assertThat(e.getStatus(), is(409));
        assertThat(e.getType(), is("delete-blocked"));
        assertThat(e.getErrors().get(0).field(), is("children"));

        var preview = TaxonomiesHandler.deleteClassification(f.context(true, null), taxonomy.getId(), "equity",
                        "true");
        assertThat(preview.get("dryRun").getAsBoolean(), is(true));
        assertThat(category("equity") != null, is(true));
        assertThat(f.file.isDirty(), is(false));

        TaxonomiesHandler.deleteClassification(f.context(false, null), taxonomy.getId(), "equity", "true");
        assertThat(category("equity"), is(nullValue()));
        assertThat(category("europe"), is(nullValue()));
        assertThat(taxonomy.getClassifications(f.eurSecurity), is(empty()));
        assertThat(f.file.isDirty(), is(true));

        TaxonomiesHandler.deleteClassification(f.context(false, null), taxonomy.getId(), "bonds", null);
        assertThat(category("bonds"), is(nullValue()));
    }

    @Test
    public void testUnknownCategoryAndTaxonomyAre404()
    {
        for (Runnable call : new Runnable[] {
                        () -> TaxonomiesHandler.getClassification(f.client, taxonomy.getId(), "nope"),
                        () -> TaxonomiesHandler.getClassification(f.client, taxonomy.getId(),
                                        taxonomy.getRoot().getId()),
                        () -> TaxonomiesHandler.get(f.client, "nope") })
        {
            assertThat(fails(call).getStatus(), is(404));
        }
    }

    // assignments

    @Test
    public void testAssignWithWeightsUpTo100Percent()
    {
        var first = assign("europe", f.eurSecurity.getUUID(), "{'weight':'60'}");
        assertThat(first.get("weight").getAsInt(), is(60));
        assertThat(first.get("type").getAsString(), is("instrument"));
        assertThat(f.file.isDirty(), is(true));

        // the default is what is left
        var second = assign("bonds", f.eurSecurity.getUUID(), "{}");
        assertThat(second.get("weight").getAsInt(), is(40));

        var e = fails(() -> assign("equity", f.eurSecurity.getUUID(), "{'weight':'0.01'}"));
        assertError(e, "weight", "weight-exceeds-100");

        // changing an existing assignment counts only the others
        assign("europe", f.eurSecurity.getUUID(), "{'weight':'50'}");
        assertThat(category("europe").getAssignments().get(0).getWeight(), is(5000));
        assertThat(category("europe").getAssignments().size(), is(1));

        assertError(fails(() -> assign("europe", f.eurSecurity.getUUID(), "{'weight':'61'}")), "weight",
                        "weight-exceeds-100");
        assertError(fails(() -> assign("europe", f.eurSecurity.getUUID(), "{'weight':'0'}")), "weight",
                        "must-be-positive");
    }

    @Test
    public void testAssignCashAccountAndDryRun()
    {
        var dry = TaxonomiesHandler.putAssignment(f.context(true, null), taxonomy.getId(), "bonds",
                        f.cash.getUUID(), json("{}"));
        assertThat(dry.entity().get("type").getAsString(), is("cash-account"));
        assertThat(dry.entity().get("dryRun").getAsBoolean(), is(true));
        assertThat(category("bonds").getAssignments(), is(empty()));
        assertThat(f.file.isDirty(), is(false));

        assign("bonds", f.cash.getUUID(), "{}");
        assertThat(category("bonds").getAssignments().get(0).getInvestmentVehicle(), is(f.cash));

        var unchanged = TaxonomiesHandler.putAssignment(f.context(false, null), taxonomy.getId(), "bonds",
                        f.cash.getUUID(), json("{'weight':100}"));
        assertThat(unchanged.changed(), is(false));
    }

    @Test
    public void testUnassign()
    {
        assign("bonds", f.cash.getUUID(), "{}");
        f.file.setDirty(false);

        var preview = TaxonomiesHandler.deleteAssignment(f.context(true, null), taxonomy.getId(), "bonds",
                        f.cash.getUUID());
        assertThat(preview.getAsJsonArray("removed").size(), is(1));
        assertThat(category("bonds").getAssignments().size(), is(1));

        TaxonomiesHandler.deleteAssignment(f.context(false, null), taxonomy.getId(), "bonds", f.cash.getUUID());
        assertThat(category("bonds").getAssignments(), is(empty()));
        assertThat(f.file.isDirty(), is(true));

        assertThat(fails(() -> TaxonomiesHandler.deleteAssignment(f.context(false, null), taxonomy.getId(), "bonds",
                        f.cash.getUUID())).getStatus(), is(404));
    }

    @Test
    public void testThroughRouter() throws Exception
    {
        var created = f.call("POST", "/taxonomies", Map.of(), "{'name':'Regions','clientRef':'t-1'}");
        assertThat(created.status(), is(201));
        var id = body(created).get("id").getAsString();
        assertThat(created.headers().get("Location"), is("/v1/files/" + f.fileId() + "/taxonomies/" + id));

        var replay = f.call("POST", "/taxonomies", Map.of(), "{'name':'Regions','clientRef':'t-1'}");
        assertThat(replay.status(), is(200));
        assertThat(body(replay).get("replayed").getAsBoolean(), is(true));

        var category = f.call("POST", "/taxonomies/" + id + "/classifications", Map.of(), "{'name':'Europe'}");
        assertThat(category.status(), is(201));
        var cid = body(category).get("id").getAsString();
        assertThat(category.headers().get("Location"),
                        is("/v1/files/" + f.fileId() + "/taxonomies/" + id + "/classifications/" + cid));

        // PUT without a body assigns what is left
        var put = f.call("PUT", "/taxonomies/" + id + "/classifications/" + cid + "/assignments/"
                        + f.eurSecurity.getUUID(), Map.of(), null);
        assertThat(put.status(), is(200));
        assertThat(body(put).get("weight").getAsInt(), is(100));

        var get = f.call("GET", "/taxonomies/" + id + "/classifications/" + cid, Map.of(), null);
        assertThat(body(get).getAsJsonArray("assignments").size(), is(1));

        var list = f.call("GET", "/taxonomies", Map.of(), null);
        assertThat(body(list).getAsJsonArray("items").size(), is(2));

        assertThat(f.call("DELETE", "/taxonomies/" + id + "/classifications/" + cid + "/assignments/"
                        + f.eurSecurity.getUUID(), Map.of(), null).status(), is(204));
        assertThat(f.call("DELETE", "/taxonomies/" + id + "/classifications/" + cid, Map.of(), null).status(),
                        is(204));
        assertThat(f.call("DELETE", "/taxonomies/" + id, Map.of(), null).status(), is(204));
        assertThat(f.client.getTaxonomies().size(), is(1));
        assertThat(f.host().hasAccessedOutsideUIThread(), is(false));
    }
}
