package name.abuchen.portfolio.cli.apply;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.gson.JsonParser;

import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.cli.command.ErrorCode;
import name.abuchen.portfolio.cli.command.FieldFailure;
import name.abuchen.portfolio.cli.command.OperationSpec;
import name.abuchen.portfolio.cli.command.Outcome;
import name.abuchen.portfolio.cli.command.Schema;
import name.abuchen.portfolio.model.Client;

/** How an operation's {@link Outcome} shows in the batch result: as its change, or as its failures. */
@SuppressWarnings("nls")
public class OutcomeReportingTest
{
    private static final Schema PROBE = Schema.object("A probe.").property(OperationSpec.OPERATION_MEMBER,
                    Schema.enumOf("Operation name.", List.of("probe")));

    private static List<Map<String, Object>> run(Outcome outcome) throws CliException
    {
        var spec = OperationSpec.<OperationContext> of("probe", "A probe.", PROBE, context -> context.record(outcome));
        return ApplyBatch.run(new Client(), JsonParser.parseString("[{\"operation\":\"probe\"}]").getAsJsonArray(),
                        List.of(spec));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> changes(Map<String, Object> result, String list)
    {
        return (List<Map<String, Object>>) result.get(list);
    }

    @Test
    public void anAppliedOutcomeWithoutBeforeIsACreatedEntity() throws CliException
    {
        var result = run(new Outcome.Applied(Outcome.Entity.CASH_ACCOUNT, "u1", null, Map.of("name", "Cash")))
                        .get(0);

        assertThat(changes(result, "created"),
                        contains(Map.of("entity", "cash-account", "uuid", "u1", "after", Map.of("name", "Cash"))));
        assertThat(changes(result, "updated"), is(empty()));
        assertThat(changes(result, "deleted"), is(empty()));
    }

    @Test
    public void anAppliedOutcomeWithBeforeAndAfterIsAnUpdatedEntity() throws CliException
    {
        var result = run(new Outcome.Applied(Outcome.Entity.TAXONOMY_ASSIGNMENT, "u2", Map.of("weight", 1),
                        Map.of("weight", 2))).get(0);

        assertThat(changes(result, "updated"), contains(Map.of("entity", "taxonomy-assignment", "uuid", "u2",
                        "before", Map.of("weight", 1), "after", Map.of("weight", 2))));
        assertThat(changes(result, "created"), is(empty()));
    }

    @Test
    public void anAppliedOutcomeWithAnEmptyAfterIsADeletedEntity() throws CliException
    {
        var result = run(new Outcome.Applied(Outcome.Entity.TRANSACTION, "u3", Map.of("type", "DEPOSIT"),
                        Map.of())).get(0);

        assertThat(changes(result, "deleted"), contains(Map.of("entity", "transaction", "uuid", "u3", "before",
                        Map.of("type", "DEPOSIT"), "after", Map.of())));
    }

    @Test
    public void anUnchangedOutcomeReportsNoChange() throws CliException
    {
        var result = run(new Outcome.Unchanged("u4")).get(0);

        assertThat(changes(result, "created"), is(empty()));
        assertThat(changes(result, "updated"), is(empty()));
        assertThat(changes(result, "deleted"), is(empty()));
    }

    @Test
    public void aRefusedOutcomeFailsTheBatchWithItsFailuresUnderTheOperationIndex()
    {
        var e = assertThrows(CliException.class, () -> run(new Outcome.Refused(
                        List.of(new FieldFailure("name", ErrorCode.REQUIRED, "A name.", null),
                                        new FieldFailure("isin", ErrorCode.INVALID_ISIN, "Bad.", "Isin.isValid")))));

        assertThat(e.getCode(), is("validation-failed"));
        assertThat(e.toProblem().get("failures"), is(List.of(
                        Map.of("operationIndex", 0, "field", "name", "code", "required", "message", "A name."),
                        Map.of("operationIndex", 0, "field", "isin", "code", "invalid-isin", "message", "Bad.",
                                        "check", "Isin.isValid"))));
    }
}
