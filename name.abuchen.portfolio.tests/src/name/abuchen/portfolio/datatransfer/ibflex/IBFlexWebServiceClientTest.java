package name.abuchen.portfolio.datatransfer.ibflex;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ibflex.IBFlexWebServiceClient.IBFlexException;
import name.abuchen.portfolio.datatransfer.ibflex.IBFlexWebServiceClient.Sleeper;

@SuppressWarnings("nls")
public class IBFlexWebServiceClientTest
{
    private static final String TOKEN = "test-token";
    private static final String QUERY_ID = "12345";
    private static final Sleeper NO_SLEEP = millis -> {
    };

    // Sample XML responses from IB Flex Web Service
    private static final String SUCCESS_RESPONSE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <FlexStatementResponse timestamp="2024-01-15 10:30:00">
                <Status>Success</Status>
                <ReferenceCode>1234567890</ReferenceCode>
                <Url>https://gdcdyn.interactivebrokers.com/AccountManagement/FlexWebService/GetStatement</Url>
            </FlexStatementResponse>
            """;

    private static final String ERROR_RESPONSE_TOKEN_INVALID = """
            <?xml version="1.0" encoding="UTF-8"?>
            <FlexStatementResponse timestamp="2024-01-15 10:30:00">
                <Status>Fail</Status>
                <ErrorCode>1015</ErrorCode>
                <ErrorMessage>Token is invalid or has expired</ErrorMessage>
            </FlexStatementResponse>
            """;

    private static final String ERROR_RESPONSE_STATEMENT_GENERATING = """
            <?xml version="1.0" encoding="UTF-8"?>
            <FlexStatementResponse timestamp="2024-01-15 10:30:00">
                <Status>Fail</Status>
                <ErrorCode>1019</ErrorCode>
                <ErrorMessage>Statement generation in progress. Please try again shortly.</ErrorMessage>
            </FlexStatementResponse>
            """;

    private static final String ERROR_RESPONSE_COULD_NOT_GENERATE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <FlexStatementResponse timestamp="2024-01-15 10:30:00">
                <Status>Fail</Status>
                <ErrorCode>1001</ErrorCode>
                <ErrorMessage>Statement could not be generated at this time. Please try again shortly.</ErrorMessage>
            </FlexStatementResponse>
            """;

    private static final String ERROR_RESPONSE_REQUEST_INVALID = """
            <?xml version="1.0" encoding="UTF-8"?>
            <FlexStatementResponse timestamp="2024-01-15 10:30:00">
                <Status>Fail</Status>
                <ErrorCode>1020</ErrorCode>
                <ErrorMessage>Invalid request or unable to validate request.</ErrorMessage>
            </FlexStatementResponse>
            """;

    private static final String FLEX_STATEMENT = """
            <?xml version="1.0" encoding="UTF-8"?>
            <FlexQueryResponse queryName="Test Query" type="AF">
                <FlexStatements count="1">
                    <FlexStatement accountId="U1234567" fromDate="2024-01-01" toDate="2024-01-15">
                    </FlexStatement>
                </FlexStatements>
            </FlexQueryResponse>
            """;

    @Test
    public void testIsStatementGenerating()
    {
        IBFlexException statementGeneratingException = new IBFlexException(
                        IBFlexWebServiceClient.ERROR_STATEMENT_GENERATING, "Statement generation in progress");
        assertTrue(statementGeneratingException.isStatementGenerating());

        IBFlexException otherException = new IBFlexException(1001, "Invalid token");
        assertFalse(otherException.isStatementGenerating());
    }

    @Test
    public void testIsTokenExpired()
    {
        IBFlexException tokenExpiredException = new IBFlexException(IBFlexWebServiceClient.ERROR_TOKEN_INVALID,
                        "Token is invalid");
        assertTrue(tokenExpiredException.isTokenExpired());

        IBFlexException otherException = new IBFlexException(IBFlexWebServiceClient.ERROR_STATEMENT_GENERATING,
                        "Statement generation in progress");
        assertFalse(otherException.isTokenExpired());
    }

    @Test
    public void testIsRequestInvalid()
    {
        IBFlexException requestInvalidException = new IBFlexException(IBFlexWebServiceClient.ERROR_REQUEST_INVALID,
                        "Invalid request");
        assertTrue(requestInvalidException.isRequestInvalid());

        IBFlexException otherException = new IBFlexException(IBFlexWebServiceClient.ERROR_TOKEN_INVALID,
                        "Token is invalid");
        assertFalse(otherException.isRequestInvalid());
    }

    @Test
    public void testExceptionWithCause()
    {
        RuntimeException cause = new RuntimeException("Network error");
        IBFlexException exception = new IBFlexException("HTTP request failed", cause);

        assertThat(exception.getMessage(), is("HTTP request failed"));
        assertThat(exception.getCause(), is(cause));
        assertThat(exception.getErrorCode(), is(-1));
        assertFalse(exception.isStatementGenerating());
        assertFalse(exception.isTokenExpired());
        assertFalse(exception.isRequestInvalid());
    }

    @Test
    public void testParseStatementAccessSuccess() throws IBFlexException, IOException
    {
        IBFlexWebServiceClient client = new IBFlexWebServiceClient();
        var access = client.parseStatementAccess(SUCCESS_RESPONSE);
        assertThat(access.referenceCode(), is("1234567890"));
        assertThat(access.url(), is(URI.create(
                        "https://gdcdyn.interactivebrokers.com/AccountManagement/FlexWebService/GetStatement")));
    }

    @Test
    public void testParseStatementAccessWithoutUrlFails()
    {
        var response = SUCCESS_RESPONSE.replaceAll("<Url>.*</Url>", "");
        var client = new IBFlexWebServiceClient();

        var exception = assertThrows(IBFlexException.class, () -> client.parseStatementAccess(response));
        assertThat(exception.getMessage(), is(Messages.IBFlexMsgErrorNoStatementUrl));
    }

    @Test
    public void testParseStatementAccessTokenInvalid()
                    throws IOException
    {
        IBFlexWebServiceClient client = new IBFlexWebServiceClient();
        IBFlexException exception = assertThrows(IBFlexException.class,
                        () -> client.parseStatementAccess(ERROR_RESPONSE_TOKEN_INVALID));

        assertException(exception, IBFlexWebServiceClient.ERROR_TOKEN_INVALID, false, true, false,
                        "Token is invalid or has expired");
    }

    @Test
    public void testParseErrorResponseStatementGenerating()
                    throws IOException
    {
        IBFlexWebServiceClient client = new IBFlexWebServiceClient();
        IBFlexException exception = assertThrows(IBFlexException.class,
                        () -> client.checkForErrorResponse(ERROR_RESPONSE_STATEMENT_GENERATING));

        assertException(exception, IBFlexWebServiceClient.ERROR_STATEMENT_GENERATING, true, false,
                        false, "Statement generation in progress. Please try again shortly.");
    }

    @Test
    public void testParseStatementAccessRequestInvalid()
                    throws IOException
    {
        IBFlexWebServiceClient client = new IBFlexWebServiceClient();
        IBFlexException exception = assertThrows(IBFlexException.class,
                        () -> client.parseStatementAccess(ERROR_RESPONSE_REQUEST_INVALID));

        assertException(exception, IBFlexWebServiceClient.ERROR_REQUEST_INVALID, false, false, true,
                        "Invalid request or unable to validate request.");
    }

    @Test
    public void testFetchStatementSuccess() throws IBFlexException, IOException
    {
        // Mock HTTP executor that returns success response, then the actual statement
        int[] callCount = { 0 };
        IBFlexWebServiceClient client = new IBFlexWebServiceClient((host, path, query, params) -> {
            callCount[0]++;
            if (path.contains("SendRequest"))
            {
                return SUCCESS_RESPONSE;
            }
            else
            {
                return FLEX_STATEMENT;
            }
        }, NO_SLEEP);

        String result = client.fetchStatement(TOKEN, QUERY_ID);

        assertThat(callCount[0], is(2)); // One for SendRequest, one for GetStatement
        assertTrue(result.contains("FlexQueryResponse"));
    }

    @Test
    public void testFetchStatementTokenExpired() throws IOException
    {
        IBFlexWebServiceClient client = new IBFlexWebServiceClient(
                        (host, path, query, params) -> ERROR_RESPONSE_TOKEN_INVALID, NO_SLEEP);

        IBFlexException exception = assertThrows(IBFlexException.class,
                        () -> client.fetchStatement("expired-token", QUERY_ID));

        assertException(exception, IBFlexWebServiceClient.ERROR_TOKEN_INVALID, false, true, false,
                        "Token is invalid or has expired");
    }

    @Test
    public void testFetchStatementNetworkError()
    {
        IBFlexWebServiceClient client = new IBFlexWebServiceClient((host, path, query, params) -> {
            throw new IOException("Network unreachable");
        }, NO_SLEEP);

        IOException exception = assertThrows(IOException.class,
                        () -> client.fetchStatement(TOKEN, QUERY_ID));

        assertThat(exception.getMessage(), is("Network unreachable"));
    }

    @Test
    public void testFetchStatementWithoutRangeUsesQueryPeriod() throws IBFlexException, IOException
    {
        var urls = new ArrayList<String>();
        var queries = new ArrayList<Map<String, String>>();
        var requests = new ArrayList<Map<String, String>>();
        var client = new IBFlexWebServiceClient((host, path, query, params) -> {
            urls.add(host + path);
            queries.add(query);
            requests.add(params);
            return path.contains("SendRequest") ? SUCCESS_RESPONSE : FLEX_STATEMENT;
        }, NO_SLEEP);

        client.fetchStatement(TOKEN, QUERY_ID);

        // GetStatement uses the URL returned by SendRequest
        assertThat(urls, is(List.of("ndcdyn.interactivebrokers.com/AccountManagement/FlexWebService/SendRequest",
                        "gdcdyn.interactivebrokers.com/AccountManagement/FlexWebService/GetStatement")));
        // the token is only sent in the body, never in the URL
        assertThat(queries, is(List.of(Map.of(), Map.of())));
        assertThat(requests.get(0), is(Map.of("t", TOKEN, "q", QUERY_ID, "v", "3")));
        assertThat(requests.get(1), is(Map.of("t", TOKEN, "q", "1234567890", "v", "3")));
    }

    @Test
    public void testStatementIsNotParsed() throws IBFlexException, IOException
    {
        // not well-formed, but recognizable as statement
        new IBFlexWebServiceClient().checkForErrorResponse("<FlexQueryResponse queryName=\"Test\"><FlexStatements");
    }

    @Test
    public void testUnexpectedResponseFails()
    {
        var client = new IBFlexWebServiceClient();
        var exception = assertThrows(IBFlexException.class,
                        () -> client.checkForErrorResponse("<html><body>Maintenance</body></html>"));
        assertThat(exception.getMessage(), is(Messages.IBFlexMsgErrorParsingResponse));
    }

    @Test
    public void testFetchStatementWithRangeSendsFromAndToDateOnSendRequest() throws IBFlexException, IOException
    {
        var queries = new ArrayList<Map<String, String>>();
        var requests = new ArrayList<Map<String, String>>();
        var client = new IBFlexWebServiceClient((host, path, query, params) -> {
            queries.add(query);
            requests.add(params);
            return path.contains("SendRequest") ? SUCCESS_RESPONSE : FLEX_STATEMENT;
        }, NO_SLEEP);

        client.fetchStatement(TOKEN, QUERY_ID,
                        new IBFlexDateRange(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 3, 1)));

        assertThat(queries.get(0), is(Map.of("fd", "20260105", "td", "20260301")));
        assertThat(requests.get(0), is(Map.of("t", TOKEN, "q", QUERY_ID, "v", "3")));
        assertThat(queries.get(1), is(Map.of()));
    }

    @Test
    public void testFetchStatementRejectsRangeLongerThanOneRequest()
    {
        var client = new IBFlexWebServiceClient((host, path, query, params) -> SUCCESS_RESPONSE, NO_SLEEP);

        assertThrows(IllegalArgumentException.class, () -> client.fetchStatement(TOKEN, QUERY_ID,
                        new IBFlexDateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 1))));
    }

    @Test
    public void testFetchStatementsSplitsLongRanges() throws IBFlexException, IOException
    {
        var sendRequests = new ArrayList<Map<String, String>>();
        var sleeps = new ArrayList<Long>();
        var progress = new ArrayList<String>();

        var client = new IBFlexWebServiceClient((host, path, query, params) -> {
            if (path.contains("SendRequest"))
            {
                sendRequests.add(query);
                return SUCCESS_RESPONSE;
            }
            return FLEX_STATEMENT;
        }, sleeps::add);

        var statements = client.fetchStatements(TOKEN, QUERY_ID,
                        new IBFlexDateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2026, 1, 10)),
                        (current, total, chunk) -> progress
                                        .add(current + "/" + total + " " + chunk.from() + ".." + chunk.to()));

        assertThat(statements.size(), is(3));
        assertThat(sendRequests.stream().map(p -> p.get("fd") + "-" + p.get("td")).toList(),
                        is(List.of("20240101-20241230", "20241231-20251230", "20251231-20260110")));
        // pause before each statement retrieval and between the date ranges
        assertThat(sleeps, is(List.of(IBFlexWebServiceClient.STATEMENT_DELAY_MS,
                        IBFlexWebServiceClient.REQUEST_DELAY_MS, IBFlexWebServiceClient.STATEMENT_DELAY_MS,
                        IBFlexWebServiceClient.REQUEST_DELAY_MS, IBFlexWebServiceClient.STATEMENT_DELAY_MS)));
        assertThat(progress, is(List.of("1/3 2024-01-01..2024-12-30", "2/3 2024-12-31..2025-12-30",
                        "3/3 2025-12-31..2026-01-10")));
    }

    @Test
    public void testFetchStatementRetriesWhileStatementIsGenerating() throws IBFlexException, IOException
    {
        int[] getStatementCalls = { 0 };
        var sleeps = new ArrayList<Long>();

        var client = new IBFlexWebServiceClient((host, path, query, params) -> {
            if (path.contains("SendRequest"))
                return SUCCESS_RESPONSE;
            return getStatementCalls[0]++ < 2 ? ERROR_RESPONSE_STATEMENT_GENERATING : FLEX_STATEMENT;
        }, sleeps::add);

        var result = client.fetchStatement(TOKEN, QUERY_ID);

        assertTrue(result.contains("FlexQueryResponse"));
        assertThat(sleeps, is(List.of(IBFlexWebServiceClient.STATEMENT_DELAY_MS,
                        IBFlexWebServiceClient.INITIAL_DELAY_MS, IBFlexWebServiceClient.INITIAL_DELAY_MS * 2)));
    }

    @Test
    public void testRetryableErrorCodes()
    {
        // all codes documented with "Please try again shortly"
        for (int code : List.of(1001, 1004, 1005, 1006, 1007, 1008, 1009, 1018, 1019, 1021))
            assertTrue(String.valueOf(code), new IBFlexException(code, "").isRetryable());

        for (int code : List.of(1003, 1010, 1011, 1012, 1013, 1014, 1015, 1016, 1017, 1020, -1))
            assertFalse(String.valueOf(code), new IBFlexException(code, "").isRetryable());
    }

    @Test
    public void testTokenExpiredErrorCodes()
    {
        assertTrue(new IBFlexException(1012, "Token has expired.").isTokenExpired());
        assertTrue(new IBFlexException(1015, "Token is invalid.").isTokenExpired());
        // 1003 is "Statement is not available."
        assertFalse(new IBFlexException(1003, "Statement is not available.").isTokenExpired());
    }

    @Test
    public void testSendRequestIsNotRetried()
    {
        int[] calls = { 0 };
        var sleeps = new ArrayList<Long>();

        var client = new IBFlexWebServiceClient((host, path, query, params) -> {
            calls[0]++;
            return ERROR_RESPONSE_COULD_NOT_GENERATE;
        }, sleeps::add);

        var exception = assertThrows(IBFlexException.class, () -> client.fetchStatement(TOKEN, QUERY_ID));

        assertThat(exception.getErrorCode(), is(1001));
        assertThat(calls[0], is(1));
        assertThat(sleeps, is(List.of()));
    }

    @Test
    public void testGivesUpAfterMaxRetries()
    {
        var sleeps = new ArrayList<Long>();
        var client = new IBFlexWebServiceClient((host, path, query, params) -> path.contains("SendRequest")
                        ? SUCCESS_RESPONSE
                        : ERROR_RESPONSE_COULD_NOT_GENERATE, sleeps::add);

        var exception = assertThrows(IBFlexException.class, () -> client.fetchStatement(TOKEN, QUERY_ID));

        assertThat(exception.getErrorCode(), is(1001));
        // initial pause plus one pause between each of the attempts
        assertThat(sleeps.size(), is(IBFlexWebServiceClient.MAX_RETRIES));
        assertThat(sleeps.get(0), is(IBFlexWebServiceClient.STATEMENT_DELAY_MS));
        assertTrue(sleeps.stream().allMatch(d -> d <= IBFlexWebServiceClient.MAX_DELAY_MS));
    }

    private void assertException(IBFlexException exception, int errorCode, boolean statementGenerating,
                    boolean tokenExpired, boolean requestInvalid, String message)
    {
        assertThat(exception.getErrorCode(), is(errorCode));
        assertThat(exception.isStatementGenerating(), is(statementGenerating));
        assertThat(exception.isTokenExpired(), is(tokenExpired));
        assertThat(exception.isRequestInvalid(), is(requestInvalid));
        assertThat(exception.getMessage(), is(message));
    }

    @Test
    public void testCancelBeforeFetchSendsNoRequest()
    {
        int[] calls = { 0 };
        var client = new IBFlexWebServiceClient((host, path, query, params) -> {
            calls[0]++;
            return SUCCESS_RESPONSE;
        }, NO_SLEEP);

        client.cancel();

        var exception = assertThrows(IBFlexException.class, () -> client.fetchStatement(TOKEN, QUERY_ID));
        assertTrue(exception.isCanceled());
        assertThat(calls[0], is(0));
    }

    @Test
    public void testAbortedRequestIsReportedAsCancel()
    {
        var client = new IBFlexWebServiceClient[1];
        client[0] = new IBFlexWebServiceClient((host, path, query, params) -> {
            // simulates the user pressing cancel while the request is in flight
            client[0].cancel();
            throw new IOException("Request aborted");
        }, NO_SLEEP);

        var exception = assertThrows(IBFlexException.class, () -> client[0].fetchStatement(TOKEN, QUERY_ID));
        assertTrue(exception.isCanceled());
    }

    @Test
    public void testCancelStopsRemainingRequests()
    {
        var sendRequests = new ArrayList<Map<String, String>>();
        var client = new IBFlexWebServiceClient[1];
        client[0] = new IBFlexWebServiceClient((host, path, query, params) -> {
            if (path.contains("SendRequest"))
            {
                sendRequests.add(query);
                return SUCCESS_RESPONSE;
            }
            // canceled while the first statement is being downloaded
            client[0].cancel();
            return FLEX_STATEMENT;
        }, NO_SLEEP);

        var exception = assertThrows(IBFlexException.class, () -> client[0].fetchStatements(TOKEN, QUERY_ID,
                        new IBFlexDateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2026, 1, 10)), null));

        assertTrue(exception.isCanceled());
        assertThat(sendRequests.size(), is(1));
    }
}
