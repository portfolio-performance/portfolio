package name.abuchen.portfolio.rest.internal.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * What a streamed tool call puts on the wire while it runs: progress only when
 * asked for, every value larger than the one before, steps thinned out, and a
 * client that hung up taken for one that cancelled.
 */
@SuppressWarnings("nls")
public class McpCallTest
{
    private final List<String> sent = new ArrayList<>();

    @Test
    public void testNothingIsReportedWithoutAProgressToken()
    {
        var call = new McpCall(null);
        call.attach(sent::add);

        call.phase("calculating");
        call.step(1, 10, "instrument 2 of 10");

        assertThat(sent.isEmpty(), is(true));
    }

    @Test
    public void testAPhaseIsAProgressNotificationUnderTheToken()
    {
        var call = new McpCall(new JsonPrimitive("t"));
        call.attach(sent::add);

        call.phase("calculating");

        var params = notification(0).getAsJsonObject("params");
        assertThat(notification(0).get("method").getAsString(), is("notifications/progress"));
        assertThat(params.get("progressToken").getAsString(), is("t"));
        assertThat(params.get("progress").getAsLong(), is(1L));
        assertThat(params.get("message").getAsString(), is("calculating"));
        assertThat(params.has("total"), is(false));
    }

    /** the protocol wants every value larger than the last, across phases and steps */
    @Test
    public void testProgressKeepsRisingAcrossPhasesAndSteps()
    {
        var call = new McpCall(new JsonPrimitive(7));
        call.attach(sent::add);

        call.phase("waiting");
        call.phase("calculating");
        call.step(0, 3, "instrument 1 of 3");

        assertThat(sent.size(), is(3));
        assertThat(progressOf(0), is(1L));
        assertThat(progressOf(1), is(2L));
        assertThat(progressOf(2), is(3L));
        // the total counts on from where the steps began
        assertThat(notification(2).getAsJsonObject("params").get("total").getAsLong(), is(5L));
    }

    /** a step per instrument would be noise: within half a second only the first goes out */
    @Test
    public void testStepsAreThinnedOut()
    {
        var call = new McpCall(new JsonPrimitive("t"));
        call.attach(sent::add);

        for (int ii = 0; ii < 100; ii++)
            call.step(ii, 100, "instrument " + (ii + 1) + " of 100");

        assertThat(sent.size(), is(1));
    }

    @Test
    public void testAnEventIsOneDataLine()
    {
        var call = new McpCall(null);
        call.attach(sent::add);

        var message = new JsonObject();
        message.addProperty("text", "two\nlines");
        call.send(message);

        assertThat(sent.get(0), containsString("event: message\ndata: {"));
        assertThat(sent.get(0).split("\n").length, is(2));
        assertThat(sent.get(0).endsWith("\n\n"), is(true));
    }

    /** writing to a client that is gone fails - and that is how the call learns it is cancelled */
    @Test
    public void testAClientThatHungUpCancelsTheCall()
    {
        var call = new McpCall(null);
        call.attach(text -> {
            throw new IOException("connection reset");
        });

        assertThat(call.isCancelled(), is(false));
        call.heartbeat();
        assertThat(call.isCancelled(), is(true));
    }

    @Test
    public void testACancelledCallSendsNothingMore()
    {
        var call = new McpCall(new JsonPrimitive("t"));
        call.attach(sent::add);

        call.cancel();
        call.phase("calculating");
        call.heartbeat();

        assertThat(sent.isEmpty(), is(true));
    }

    private JsonObject notification(int index)
    {
        var data = sent.get(index).lines().filter(line -> line.startsWith("data: ")).findFirst().orElseThrow();
        return JsonParser.parseString(data.substring("data: ".length())).getAsJsonObject();
    }

    private long progressOf(int index)
    {
        return notification(index).getAsJsonObject("params").get("progress").getAsLong();
    }
}
