package name.abuchen.portfolio.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.rest.internal.TransactionFixture;

/** The transaction write endpoints over real HTTP. */
@SuppressWarnings("nls")
public class TransactionsHttpTest
{
    private static final String TOKEN = "test-token";

    private TransactionFixture f;
    private RestApiServer server;
    private HttpClient http;

    @Before
    public void setUp() throws Exception
    {
        f = new TransactionFixture();
        server = new RestApiServer(0, TOKEN::equals, f.router());
        server.start();
        http = HttpClient.newHttpClient();
    }

    @After
    public void tearDown() throws Exception
    {
        server.stop();
        f.dispose();
    }

    private HttpResponse<String> send(String method, String path, String contentType, String body) throws Exception
    {
        var builder = HttpRequest
                        .newBuilder(URI.create("http://127.0.0.1:" + server.getPort() + "/v1/files/" + f.fileId()
                                        + path))
                        .header("Authorization", "Bearer " + TOKEN)
                        .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                                        : HttpRequest.BodyPublishers.ofString(body.replace('\'', '"')));
        if (contentType != null)
            builder.header("Content-Type", contentType);
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    public void testCreatePatchWithEitherContentTypeAndDelete() throws Exception
    {
        var created = send("POST", "/transactions", "application/json",
                        "{'type':'deposit','date':'2026-01-02','cashAccount':'" + f.cash.getUUID()
                                        + "','amount':'100.10'}");
        assertThat(created.statusCode(), is(201));
        assertThat(created.headers().firstValue("Location").orElse(""), containsString("/transactions/"));
        var uuid = f.cash.getTransactions().get(0).getUUID();

        var merge = send("PATCH", "/transactions/" + uuid, "application/merge-patch+json", "{'note':'merge'}");
        assertThat(merge.statusCode(), is(200));
        assertThat(f.cash.getTransactions().get(0).getNote(), is("merge"));

        var plain = send("PATCH", "/transactions/" + uuid, "application/json", "{'amount':'200'}");
        assertThat(plain.statusCode(), is(200));
        assertThat(plain.body(), containsString("\"value\":200"));

        var dry = send("DELETE", "/transactions/" + uuid + "?dry_run=true", null, null);
        assertThat(dry.statusCode(), is(200));
        assertThat(dry.body(), containsString("\"removed\""));
        assertThat(f.cash.getTransactions().size(), is(1));

        var deleted = send("DELETE", "/transactions/" + uuid, null, null);
        assertThat(deleted.statusCode(), is(204));
        assertThat(f.cash.getTransactions().isEmpty(), is(true));
    }

    @Test
    public void testValidationErrorIsProblemJson() throws Exception
    {
        var response = send("POST", "/transactions", "application/json", "{'type':'deposit'}");

        assertThat(response.statusCode(), is(422));
        assertThat(response.headers().firstValue("Content-Type").orElse(""), is("application/problem+json"));
        assertThat(response.body(), containsString("\"code\":\"required\""));
    }
}
