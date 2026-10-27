package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

@SuppressWarnings("nls")
public class OpenApiHandlerTest
{
    /**
     * Serves the real {@code openapi.yaml}. Also asserts that the document is
     * genuinely reachable as a bundle entry - the packaging (build.properties)
     * and the loading in {@link OpenApiHandler} together - not just in the IDE.
     */
    @Test
    public void testServesTheOpenApiDocument()
    {
        var response = OpenApiHandler.serve();

        assertThat(response.status(), is(200));
        assertThat(response.contentType(), is("application/yaml"));

        var body = new String(response.body(), StandardCharsets.UTF_8);
        assertThat(body, startsWith("openapi:"));
        assertThat(body, containsString("Portfolio Performance REST API"));
    }
}
