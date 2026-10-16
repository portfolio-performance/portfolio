package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Client;

public class DescribeCommandTest
{
    @Test
    public void describeStatesKnownLimitsPinnedTagAndMaxSupportedVersion()
    {
        var data = data(CliRun.run("describe"));

        assertThat(data.get("pinnedUpstreamTag").getAsString(), is("0.88.0"));
        assertThat(data.get("maxSupportedVersion").getAsInt(), is(Client.CURRENT_VERSION));

        var limits = data.getAsJsonArray("limits").toString().toLowerCase();
        assertThat(limits, containsString("unsaved"));
        assertThat(limits, containsString("pdf"));
        assertThat(limits, containsString("price"));
    }

    @Test
    public void describeRendersWriteOperationsWithInputSchemas()
    {
        var operation = new OperationSpec("instrument.create", "Creates an instrument.",
                        Schema.object("Instrument to create.").property("name", Schema.string("Display name."))
                                        .optionalProperty("isin", Schema.string("ISIN, if known.")));

        var data = new Gson().toJsonTree(DescribeCommand.render(List.of(), List.of(operation))).getAsJsonObject();

        var described = data.getAsJsonArray("operations").get(0).getAsJsonObject();
        assertThat(described.get("name").getAsString(), is("instrument.create"));
        assertThat(described.get("summary").getAsString(), is("Creates an instrument."));

        var input = described.getAsJsonObject("input");
        assertThat(input.getAsJsonObject("properties").keySet(), is(Set.of("name", "isin")));
        assertThat(input.getAsJsonArray("required").toString(), is("[\"name\"]"));
    }

    private static JsonObject data(CliRun run)
    {
        return run.json().getAsJsonObject().getAsJsonObject("data");
    }

    private static JsonObject command(JsonObject data, String name)
    {
        for (var element : data.getAsJsonArray("commands"))
        {
            if (element.getAsJsonObject().get("name").getAsString().equals(name))
                return element.getAsJsonObject();
        }
        throw new AssertionError("command not described: " + name);
    }

    @Test
    public void describeListsCommandsWithOptionsAndResponseSchemas()
    {
        var data = data(CliRun.run("describe"));

        var info = command(data, "info");
        assertThat(info.get("summary").getAsString().isBlank(), is(false));
        assertThat(info.get("usesClientFile").getAsBoolean(), is(true));

        var file = info.getAsJsonArray("options").get(0).getAsJsonObject();
        assertThat(file.get("name").getAsString(), is("file"));
        assertThat(file.get("valueName").getAsString(), is("path"));
        assertThat(file.get("description").getAsString().isBlank(), is(false));

        var response = info.getAsJsonObject("response");
        var infoData = response.getAsJsonObject("data");
        assertThat(infoData.get("type").getAsString(), is("object"));
        assertThat(infoData.getAsJsonObject("properties").has("fileVersion"), is(true));
        assertThat(response.getAsJsonObject("meta").getAsJsonObject("properties").has("asOf"), is(true));

        var describe = command(data, "describe");
        assertThat(describe.get("usesClientFile").getAsBoolean(), is(false));
        assertThat(describe.getAsJsonArray("options").size(), is(0));
    }

    @Test
    public void describeWorksWithoutFileAndReturnsEnvelope()
    {
        var run = CliRun.run("describe");

        assertThat(run.exitCode, is(0));

        var envelope = run.json().getAsJsonObject();
        assertThat(envelope.keySet(), is(Set.of("data", "meta", "warnings")));
        assertThat(envelope.getAsJsonArray("warnings").size(), is(0));
        assertThat(envelope.getAsJsonObject("meta").get("asOf").getAsString(), is("2026-10-16"));
    }
}
