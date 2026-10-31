package name.abuchen.portfolio.cli.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.google.gson.JsonParser;

import name.abuchen.portfolio.cli.masterdata.Instruments;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;

/**
 * The reading of one operation's input: the failures it reports into its sink
 * and the values it hands back.
 */
@SuppressWarnings("nls")
public class OperationInputTest
{
    private static final Schema ITEM_SCHEMA = Schema.object("An item.").property("size", Schema.number("A size."));

    private static final Schema PROBE_SCHEMA = Schema.object("A probe.")
                    .property(OperationSpec.OPERATION_MEMBER, Schema.enumOf("Operation name.", List.of("probe")))
                    .optionalProperty("name", Schema.string("A name."))
                    .optionalProperty("items", Schema.array("Items.", ITEM_SCHEMA))
                    .optionalProperty("instrument", Schema.string("UUID of an instrument."));

    private final Client client = new Client();
    private final Set<Object> existing = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<FieldFailure> failures = new ArrayList<>();

    private OperationInput input(String json)
    {
        return new OperationInput(JsonParser.parseString(json).getAsJsonObject(), client, existing::contains,
                        failures::add);
    }

    /** The failures reported so far, as field/code/message. */
    private List<String> failures()
    {
        return failures.stream().map(f -> f.field() + "/" + f.code().code() + "/" + f.message()).toList();
    }

    @Test
    public void aMemberTheSchemaDoesNotDeclareIsRefused()
    {
        input("{\"operation\":\"probe\",\"name\":\"x\",\"colour\":\"red\"}").rejectUnknownFields(PROBE_SCHEMA);

        assertThat(failures(), contains("colour/unknown-field/The operation has no field 'colour'."));
    }

    @Test
    public void everyDeclaredMemberIsAccepted()
    {
        input("{\"operation\":\"probe\",\"name\":\"x\"}").rejectUnknownFields(PROBE_SCHEMA);

        assertThat(failures, is(empty()));
    }

    @Test
    public void aBlankOrMissingTextIsReadAsNull()
    {
        var input = input("{\"name\":\"  \"}");

        assertThat(input.text("name"), is(nullValue()));
        assertThat(input.text("note"), is(nullValue()));
        assertThat(failures, is(empty()));
    }

    @Test
    public void aTextThatIsNotAStringIsAnInvalidType()
    {
        assertThat(input("{\"name\":7}").text("name"), is(nullValue()));
        assertThat(failures(), contains("name/invalid-type/The name must be a string."));
    }

    @Test
    public void aRequiredTextThatIsMissingOrBlankIsRequiredButAWrongTypeIsReportedOnce()
    {
        input("{\"name\":\"\"}").requiredText("name", "A probe needs a name.");
        input("{}").requiredText("name", "A probe needs a name.");
        input("{\"name\":[]}").requiredText("name", "A probe needs a name.");

        assertThat(failures(), contains("name/required/A probe needs a name.", "name/required/A probe needs a name.",
                        "name/invalid-type/The name must be a string."));
    }

    @Test
    public void aNumberMustBeANumberAndPositiveIfAsked()
    {
        input("{\"name\":\"7\"}").number("name", null, false);
        input("{\"name\":0}").number("name", null, true);
        input("{}").number("name", "A probe needs a name.", true);
        input("{}").number("name", null, true);

        assertThat(failures(), contains("name/invalid-type/The name must be a number.",
                        "name/not-positive/The name must be positive.", "name/required/A probe needs a name."));
    }

    @Test
    public void anAmountIsReadAsStoredUnitsAndRefusesMorePrecisionThanIsStored()
    {
        assertThat(input("{\"name\":12.34}").amount("name", Values.Amount, true, null), is(1234L));
        assertThat(failures, is(empty()));

        assertThat(input("{\"name\":12.345}").amount("name", Values.Amount, true, null), is(nullValue()));
        assertThat(failures(), contains("name/invalid-amount/The name has more decimal places than are stored."));
    }

    @Test
    public void aNestedObjectReportsItsFailuresUnderItsPathInOrder()
    {
        var read = input("{\"items\":[{\"size\":\"big\",\"colour\":1},7,{\"size\":0}]}").eachObject("items",
                        "An item must be a JSON object.", item -> {
                            item.rejectUnknownFields(ITEM_SCHEMA);
                            item.number("size", "An item needs a size.", true);
                        });

        assertThat(read, is(true));
        assertThat(failures(), contains("items[0].colour/unknown-field/The object has no field 'colour'.",
                        "items[0].size/invalid-type/The size must be a number.",
                        "items[1]/invalid-type/An item must be a JSON object.",
                        "items[2].size/not-positive/The size must be positive."));
    }

    @Test
    public void anArrayMemberThatIsMissingIsUnusableAndOneThatIsNotAnArrayIsAnInvalidType()
    {
        var items = new ArrayList<OperationInput>();

        assertThat(input("{}").eachObject("items", "", items::add), is(false));
        assertThat(input("{\"items\":{}}").eachObject("items", "", items::add), is(false));
        assertThat(items, is(empty()));
        assertThat(failures(), contains("items/invalid-type/The items must be an array."));
    }

    @Test
    public void anExistingEntityIsFoundByItsUuid()
    {
        var security = new Security("ACME", "EUR");
        client.addSecurity(security);
        existing.add(security);

        var found = input("{\"instrument\":\"" + security.getUUID() + "\"}").existing("instrument",
                        Instruments.INSTRUMENTS.entityKind(), "A probe needs an instrument.");

        assertThat(found, is(security));
        assertThat(failures, is(empty()));
    }

    @Test
    public void anEntityThatWasNotThereBeforeTheBatchIsNotAnExistingEntity()
    {
        var added = new Security("ACME", "EUR");
        client.addSecurity(added);

        var found = input("{\"instrument\":\"" + added.getUUID() + "\"}").existing("instrument",
                        Instruments.INSTRUMENTS.entityKind(), "A probe needs an instrument.");

        assertThat(found, is(nullValue()));
        assertThat(failures(), contains("instrument/reference-not-found/No instrument with UUID " + added.getUUID()
                        + " in the file."));
    }

    @Test
    public void aMissingExistingEntityIsRequired()
    {
        input("{}").existing("instrument", Instruments.INSTRUMENTS.entityKind(), "A probe needs an instrument.");

        assertThat(failures(), contains("instrument/required/A probe needs an instrument."));
    }
}
