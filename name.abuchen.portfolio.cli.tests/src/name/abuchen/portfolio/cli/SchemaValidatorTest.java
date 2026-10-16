package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.List;

import org.junit.Test;

import com.google.gson.JsonParser;

/** Guards the guard: the consistency test is only as good as its validator. */
public class SchemaValidatorTest
{
    private static final String SCHEMA = """
                    {"type":"object","required":["a"],"additionalProperties":false,
                     "properties":{"a":{"type":"integer"},"b":{"type":["string","null"]},
                                   "c":{"type":"array","items":{"type":"string","enum":["x"]}}}}""";

    private static List<String> validate(String json)
    {
        return SchemaValidator.validate(JsonParser.parseString(json), JsonParser.parseString(SCHEMA).getAsJsonObject());
    }

    @Test
    public void acceptsConformingValue()
    {
        assertThat(validate("{\"a\":1,\"b\":null,\"c\":[\"x\"]}"), is(empty()));
    }

    @Test
    public void acceptsAnyOfSeveralTypes()
    {
        var schema = JsonParser.parseString("{\"type\":[\"string\",\"number\",\"boolean\"]}").getAsJsonObject();

        assertThat(SchemaValidator.validate(JsonParser.parseString("\"a\""), schema), is(empty()));
        assertThat(SchemaValidator.validate(JsonParser.parseString("1.5"), schema), is(empty()));
        assertThat(SchemaValidator.validate(JsonParser.parseString("true"), schema), is(empty()));
        assertThat(SchemaValidator.validate(JsonParser.parseString("null"), schema), not(empty()));
        assertThat(SchemaValidator.validate(JsonParser.parseString("[]"), schema), not(empty()));
    }

    @Test
    public void rejectsMissingUndeclaredAndMistypedMembers()
    {
        assertThat(validate("{}"), hasItem("$: missing required property 'a'"));
        assertThat(validate("{\"a\":1,\"z\":1}"), hasItem("$: undeclared property 'z'"));
        assertThat(validate("{\"a\":1.5}"), not(empty()));
        assertThat(validate("{\"a\":1,\"b\":2}"), not(empty()));
        assertThat(validate("{\"a\":1,\"c\":[\"y\"]}"), not(empty()));
    }
}
