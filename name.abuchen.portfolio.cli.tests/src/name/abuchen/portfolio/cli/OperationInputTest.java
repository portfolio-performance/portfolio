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

/**
 * The reading of one operation's input, seen from an operation in a batch: the
 * failures it reports and the values it hands back.
 */
@SuppressWarnings("nls")
public class OperationInputTest
{
    private static final Schema PROBE_SCHEMA = Schema.object("A probe.")
                    .property(ApplyBatch.OPERATION_MEMBER, Schema.enumOf("Operation name.", List.of("probe")))
                    .optionalProperty("name", Schema.string("A name."));

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
}
