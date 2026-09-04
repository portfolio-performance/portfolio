package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.Test;

@SuppressWarnings("nls")
public class ProblemJsonTest
{
    @Test
    public void testValidationProblemContainsAllFieldErrors()
    {
        var exception = ApiException.validation(List.of( //
                        new ApiException.FieldError("name", "required", "name must not be empty"),
                        new ApiException.FieldError("currencyCode", "unknown-currency",
                                        "XXX is not a known currency")));

        var json = ProblemJson.toJson(exception);

        assertThat(json.get("status").getAsInt(), is(422));
        assertThat(json.get("type").getAsString(), is("https://portfolio-performance.info/rest/problems/validation"));
        assertThat(json.get("errors").getAsJsonArray().size(), is(2));
        assertThat(json.get("errors").getAsJsonArray().get(0).getAsJsonObject().get("field").getAsString(), is("name"));
    }

    @Test
    public void testLockedProblemCarriesRetryAfterHeader()
    {
        var exception = ApiException.locked();
        assertThat(exception.getStatus(), is(423));
        assertThat(exception.getHeaders().get("Retry-After"), is("5"));
    }

    @Test
    public void testOccurrenceSpecificTextGoesIntoDetailNotTitle()
    {
        var json = ProblemJson.toJson(ApiException.badRequest("request body is not valid JSON"));

        // the title is invariant for the type, the detail carries the specifics
        assertThat(json.get("title").getAsString(), is("Invalid request"));
        assertThat(json.get("detail").getAsString(), is("request body is not valid JSON"));
    }

    @Test
    public void testProblemWithoutDetailOmitsTheMember()
    {
        var json = ProblemJson.toJson(ApiException.notFound());

        assertThat(json.get("title").getAsString(), is("Not found"));
        assertThat(json.has("detail"), is(false));
    }

    @Test
    public void testUnauthorizedCarriesWwwAuthenticateHeader()
    {
        var exception = ApiException.unauthorized();
        assertThat(exception.getStatus(), is(401));
        assertThat(exception.getHeaders().get("WWW-Authenticate"), is("Bearer"));
    }
}
