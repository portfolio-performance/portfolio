package name.abuchen.portfolio.datatransfer.ibflex;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import name.abuchen.portfolio.datatransfer.ibflex.IBFlexWebServiceClient.IBFlexException;

@SuppressWarnings("nls")
public class IBFlexWebServiceClientTest
{
    private static final String TOKEN = "test-token";
    private static final String QUERY_ID = "12345";

    // Sample XML responses from IB Flex Web Service
    private static final String SUCCESS_RESPONSE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <FlexStatementResponse timestamp="2024-01-15 10:30:00">
                <Status>Success</Status>
                <ReferenceCode>1234567890</ReferenceCode>
                <Url>https://example.com</Url>
            </FlexStatementResponse>
            """;

    private static final String ERROR_RESPONSE_TOKEN_INVALID = """
            <?xml version="1.0" encoding="UTF-8"?>
            <FlexStatementResponse timestamp="2024-01-15 10:30:00">
                <Status>Fail</Status>
                <ErrorCode>1003</ErrorCode>
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
    public void testExceptionWithCause()
    {
        RuntimeException cause = new RuntimeException("Network error");
        IBFlexException exception = new IBFlexException("HTTP request failed", cause);

        assertThat(exception.getMessage(), is("HTTP request failed"));
        assertThat(exception.getCause(), is(cause));
        assertThat(exception.getErrorCode(), is(-1));
        assertFalse(exception.isStatementGenerating());
        assertFalse(exception.isTokenExpired());
    }

    @Test
    public void testParseReferenceCodeSuccess() throws IBFlexException, IOException
    {
        IBFlexWebServiceClient client = new IBFlexWebServiceClient();
        String referenceCode = client.parseReferenceCode(SUCCESS_RESPONSE);
        assertThat(referenceCode, is("1234567890"));
    }

    @Test
    public void testParseReferenceCodeTokenInvalid()
                    throws IOException
    {
        IBFlexWebServiceClient client = new IBFlexWebServiceClient();
        IBFlexException exception = assertThrows(IBFlexException.class,
                        () -> client.parseReferenceCode(ERROR_RESPONSE_TOKEN_INVALID));

        assertException(exception, IBFlexWebServiceClient.ERROR_TOKEN_INVALID, false, true,
                        "Token is invalid or has expired");
    }

    @Test
    public void testParseErrorResponseStatementGenerating()
                    throws IOException
    {
        IBFlexWebServiceClient client = new IBFlexWebServiceClient();
        IBFlexException exception = assertThrows(IBFlexException.class,
                        () -> client.parseErrorResponse(ERROR_RESPONSE_STATEMENT_GENERATING));

        assertException(exception, IBFlexWebServiceClient.ERROR_STATEMENT_GENERATING, true, false,
                        "Statement generation in progress. Please try again shortly.");
    }

    @Test
    public void testFetchStatementSuccess() throws IBFlexException, IOException
    {
        // Mock HTTP executor that returns success response, then the actual statement
        int[] callCount = { 0 };
        IBFlexWebServiceClient client = new IBFlexWebServiceClient((host, path, token, queryOrRef, version) -> {
            callCount[0]++;
            if (path.contains("SendRequest"))
            {
                return SUCCESS_RESPONSE;
            }
            else
            {
                return FLEX_STATEMENT;
            }
        });

        String result = client.fetchStatement(TOKEN, QUERY_ID, () -> false);

        assertThat(callCount[0], is(2)); // One for SendRequest, one for GetStatement
        assertTrue(result.contains("FlexQueryResponse"));
    }

    @Test
    public void testFetchStatementTokenExpired() throws IOException
    {
        IBFlexWebServiceClient client = new IBFlexWebServiceClient(
                        (host, path, token, queryOrRef, version) -> ERROR_RESPONSE_TOKEN_INVALID);

        IBFlexException exception = assertThrows(IBFlexException.class,
                        () -> client.fetchStatement("expired-token", QUERY_ID, () -> false));

        assertException(exception, IBFlexWebServiceClient.ERROR_TOKEN_INVALID, false, true,
                        "Token is invalid or has expired");
    }

    @Test
    public void testFetchStatementNetworkError()
    {
        IBFlexWebServiceClient client = new IBFlexWebServiceClient((host, path, token, queryOrRef, version) -> {
            throw new IOException("Network unreachable");
        });

        IOException exception = assertThrows(IOException.class,
                        () -> client.fetchStatement(TOKEN, QUERY_ID, () -> false));

        assertThat(exception.getMessage(), is("Network unreachable"));
    }

    private void assertException(IBFlexException exception, int errorCode, boolean statementGenerating,
                    boolean tokenExpired, String message)
    {
        assertThat(exception.getErrorCode(), is(errorCode));
        assertThat(exception.isStatementGenerating(), is(statementGenerating));
        assertThat(exception.isTokenExpired(), is(tokenExpired));
        assertThat(exception.getMessage(), is(message));
    }
}
