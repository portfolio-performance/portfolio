package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.gson.JsonParser;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;

/**
 * The reading of one operation's input, seen from an operation in a batch: the
 * failures it reports and the values it hands back.
 */
@SuppressWarnings("nls")
public class OperationInputTest
{
    private static final Schema ITEM_SCHEMA = Schema.object("An item.").property("size", Schema.number("A size."));

    private static final Schema PROBE_SCHEMA = Schema.object("A probe.")
                    .property(ApplyBatch.OPERATION_MEMBER, Schema.enumOf("Operation name.", List.of("probe")))
                    .optionalProperty("name", Schema.string("A name."))
                    .optionalProperty("items", Schema.array("Items.", ITEM_SCHEMA))
                    .optionalProperty("instrument", Schema.string("UUID of an instrument."));

    private final Client client = new Client();
    private final List<Object> values = new ArrayList<>();

    /** Runs the input as the only operation of a batch and returns the failures as field/code/message. */
    private List<String> run(String input, OperationSpec.Handler handler)
    {
        var spec = new OperationSpec("probe", "A probe.", PROBE_SCHEMA, handler);
        var operations = JsonParser.parseString("[" + input + "]").getAsJsonArray();
        try
        {
            ApplyBatch.run(client, operations, List.of(spec));
            return List.of();
        }
        catch (CliException e)
        {
            @SuppressWarnings("unchecked")
            var failures = (List<Map<String, Object>>) e.toProblem().get("failures");
            return failures.stream().map(f -> f.get("field") + "/" + f.get("code") + "/" + f.get("message")).toList();
        }
    }

    @Test
    public void aMemberTheSchemaDoesNotDeclareIsRefusedAndTheOperationStillRuns()
    {
        var failures = run("{\"operation\":\"probe\",\"name\":\"x\",\"colour\":\"red\"}",
                        context -> values.add("ran"));

        assertThat(failures, contains("colour/unknown-field/The operation has no field 'colour'."));
        assertThat(values, contains("ran"));
    }

    @Test
    public void everyDeclaredMemberIsAccepted()
    {
        assertThat(run("{\"operation\":\"probe\",\"name\":\"x\"}", context -> {
        }), is(empty()));
    }

    @Test
    public void aBlankOrMissingTextIsReadAsNull()
    {
        var failures = run("{\"operation\":\"probe\",\"name\":\"  \"}", context -> {
            values.add(context.input().text("name"));
            values.add(context.input().text("note"));
        });

        assertThat(failures, is(empty()));
        assertThat(values, contains(null, null));
    }

    @Test
    public void aTextThatIsNotAStringIsAnInvalidType()
    {
        var failures = run("{\"operation\":\"probe\",\"name\":7}", context -> values.add(context.input().text("name")));

        assertThat(failures, contains("name/invalid-type/The name must be a string."));
        assertThat(values, contains((Object) null));
    }

    @Test
    public void aRequiredTextThatIsMissingOrBlankIsRequiredButAWrongTypeIsReportedOnce()
    {
        assertThat(run("{\"operation\":\"probe\",\"name\":\"\"}",
                        context -> context.input().requiredText("name", "A probe needs a name.")),
                        contains("name/required/A probe needs a name."));
        assertThat(run("{\"operation\":\"probe\"}",
                        context -> context.input().requiredText("name", "A probe needs a name.")),
                        contains("name/required/A probe needs a name."));
        assertThat(run("{\"operation\":\"probe\",\"name\":[]}",
                        context -> context.input().requiredText("name", "A probe needs a name.")),
                        contains("name/invalid-type/The name must be a string."));
    }

    @Test
    public void aNumberMustBeANumberAndPositiveIfAsked()
    {
        assertThat(run("{\"operation\":\"probe\",\"name\":\"7\"}",
                        context -> context.input().number("name", null, false)),
                        contains("name/invalid-type/The name must be a number."));
        assertThat(run("{\"operation\":\"probe\",\"name\":0}", context -> context.input().number("name", null, true)),
                        contains("name/not-positive/The name must be positive."));
        assertThat(run("{\"operation\":\"probe\"}", context -> context.input().number("name", "A probe needs a name.", true)),
                        contains("name/required/A probe needs a name."));
        assertThat(run("{\"operation\":\"probe\"}", context -> context.input().number("name", null, true)), is(empty()));
    }

    @Test
    public void anAmountIsReadAsStoredUnitsAndRefusesMorePrecisionThanIsStored()
    {
        assertThat(run("{\"operation\":\"probe\",\"name\":12.34}",
                        context -> values.add(context.input().amount("name", Values.Amount, true, null))),
                        is(empty()));
        assertThat(values, contains(1234L));

        assertThat(run("{\"operation\":\"probe\",\"name\":12.345}",
                        context -> context.input().amount("name", Values.Amount, true, null)),
                        contains("name/invalid-amount/The name has more decimal places than are stored."));
    }

    @Test
    public void aNestedObjectReportsItsFailuresUnderItsPathInOrder()
    {
        var failures = run("{\"operation\":\"probe\",\"items\":[{\"size\":\"big\",\"colour\":1},7,{\"size\":0}]}",
                        context -> values.add(context.input().eachObject("items", "An item must be a JSON object.",
                                        item -> {
                                            item.rejectUnknownFields(ITEM_SCHEMA);
                                            item.number("size", "An item needs a size.", true);
                                        })));

        assertThat(failures, contains("items[0].colour/unknown-field/The object has no field 'colour'.",
                        "items[0].size/invalid-type/The size must be a number.",
                        "items[1]/invalid-type/An item must be a JSON object.",
                        "items[2].size/not-positive/The size must be positive."));
        assertThat(values, contains(true));
    }

    @Test
    public void anArrayMemberThatIsMissingIsUnusableAndOneThatIsNotAnArrayIsAnInvalidType()
    {
        assertThat(run("{\"operation\":\"probe\"}",
                        context -> values.add(context.input().eachObject("items", "", item -> values.add(item)))),
                        is(empty()));
        assertThat(run("{\"operation\":\"probe\",\"items\":{}}",
                        context -> values.add(context.input().eachObject("items", "", item -> values.add(item)))),
                        contains("items/invalid-type/The items must be an array."));
        assertThat(values, contains(false, false));
    }

    @Test
    public void anExistingEntityIsFoundByItsUuid()
    {
        var security = new Security("ACME", "EUR");
        client.addSecurity(security);

        var failures = run("{\"operation\":\"probe\",\"instrument\":\"" + security.getUUID() + "\"}",
                        context -> values.add(context.input().existing("instrument", EntityKind.INSTRUMENT,
                                        "A probe needs an instrument.")));

        assertThat(failures, is(empty()));
        assertThat(values, contains(security));
    }

    @Test
    public void anEntityAddedEarlierInTheBatchIsNotAnExistingEntity()
    {
        var added = new Security("ACME", "EUR");

        var failures = run("{\"operation\":\"probe\",\"instrument\":\"" + added.getUUID() + "\"}", context -> {
            context.client().addSecurity(added);
            values.add(context.input().existing("instrument", EntityKind.INSTRUMENT, "A probe needs an instrument."));
        });

        assertThat(failures, contains("instrument/reference-not-found/No instrument with UUID " + added.getUUID()
                        + " in the file."));
        assertThat(values, contains((Object) null));
    }

    @Test
    public void aMissingExistingEntityIsRequired()
    {
        assertThat(run("{\"operation\":\"probe\"}",
                        context -> context.input().existing("instrument", EntityKind.INSTRUMENT,
                                        "A probe needs an instrument.")),
                        contains("instrument/required/A probe needs an instrument."));
    }
}
