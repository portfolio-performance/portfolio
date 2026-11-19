package name.abuchen.portfolio.datatransfer.ibflex;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.osgi.framework.FrameworkUtil;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.util.WebAccess;

/**
 * Client for the Interactive Brokers Flex Web Service API.
 * <p>
 * The API uses a two-step process:
 * <ol>
 * <li>Request statement generation (returns a reference code)</li>
 * <li>Fetch the generated statement using the reference code</li>
 * </ol>
 *
 * @see <a href=
 *      "https://www.interactivebrokers.com/campus/ibkr-api-page/flex-web-service/">IB
 *      Flex Web Service Documentation</a>
 */
public class IBFlexWebServiceClient
{
    /**
     * Exception thrown when the IB Flex Web Service returns an error.
     */
    public static class IBFlexException extends Exception
    {
        private static final long serialVersionUID = 1L;

        private final int errorCode;

        public IBFlexException(int errorCode, String message)
        {
            super(message);
            this.errorCode = errorCode;
        }

        public IBFlexException(String message, Throwable cause)
        {
            super(message, cause);
            this.errorCode = -1;
        }

        public int getErrorCode()
        {
            return errorCode;
        }

        /**
         * Returns true if this error indicates the statement is still being
         * generated and the request should be retried.
         */
        public boolean isStatementGenerating()
        {
            return errorCode == ERROR_STATEMENT_GENERATING;
        }

        /**
         * Returns true if IB reports a temporary condition and the request
         * should be retried.
         */
        public boolean isRetryable()
        {
            return RETRYABLE_ERRORS.contains(errorCode);
        }

        /**
         * Returns true if this error indicates the token has expired or is
         * invalid.
         */
        public boolean isTokenExpired()
        {
            return errorCode == ERROR_TOKEN_EXPIRED || errorCode == ERROR_TOKEN_INVALID;
        }

        /**
         * Returns true if this error indicates the request could not be
         * validated by IB.
         */
        public boolean isRequestInvalid()
        {
            return errorCode == ERROR_REQUEST_INVALID;
        }

        /**
         * Returns true if this error indicates the operation was canceled by
         * the user.
         */
        public boolean isCanceled()
        {
            return getCause() instanceof InterruptedException;
        }
    }

    /**
     * Functional interface for making HTTP POST requests. Allows mocking in
     * tests.
     */
    @FunctionalInterface
    public interface HttpRequestExecutor
    {
        /**
         * @param query
         *            non-sensitive parameters sent in the URL
         * @param form
         *            parameters sent form-encoded in the request body,
         *            including the token
         */
        String execute(String host, String path, Map<String, String> query, Map<String, String> form)
                        throws IOException;
    }

    /**
     * Receives progress updates while fetching statements.
     */
    @FunctionalInterface
    public interface ProgressListener
    {
        /**
         * @param current
         *            1-based number of the request about to be sent
         * @param total
         *            total number of requests
         * @param range
         *            the date range covered by this request
         */
        void onRequest(int current, int total, IBFlexDateRange range);
    }

    /**
     * Functional interface for waiting between requests. Allows tests to run
     * without delays.
     */
    @FunctionalInterface
    public interface Sleeper
    {
        void sleep(long millis) throws InterruptedException;
    }

    /**
     * Response of SendRequest: the reference code of the statement and the URL
     * to retrieve it from.
     */
    /* package */ record StatementAccess(String referenceCode, URI url)
    {
    }

    // see https://www.ibkrguides.com/complianceportal/complianceportal/flexweb3.htm
    /* package */ static final String SEND_REQUEST_HOST = "ndcdyn.interactivebrokers.com";
    /* package */ static final String SEND_REQUEST_PATH = "/AccountManagement/FlexWebService/SendRequest";
    private static final int VERSION = 3;

    // Error codes from IB documentation, see
    // https://www.ibkrguides.com/clientportal/performanceandstatements/flex3error.htm
    /* package */ static final int ERROR_COULD_NOT_GENERATE = 1001;
    /* package */ static final int ERROR_TOKEN_EXPIRED = 1012;
    /* package */ static final int ERROR_TOKEN_INVALID = 1015;
    /* package */ static final int ERROR_TOO_MANY_REQUESTS = 1018;
    /* package */ static final int ERROR_STATEMENT_GENERATING = 1019;
    /* package */ static final int ERROR_REQUEST_INVALID = 1020;

    /**
     * Temporary errors ("Please try again shortly").
     */
    private static final Set<Integer> RETRYABLE_ERRORS = Set.of(ERROR_COULD_NOT_GENERATE, 1004, 1005, 1006, 1007,
                    1008, 1009, ERROR_TOO_MANY_REQUESTS, ERROR_STATEMENT_GENERATING, 1021);

    /**
     * Pause between SendRequest and the first attempt to retrieve the
     * statement.
     */
    /* package */ static final long STATEMENT_DELAY_MS = 1000;

    /**
     * Pause between the requests for consecutive date ranges. IB allows at
     * most one request per second per token (error 1018).
     */
    /* package */ static final long REQUEST_DELAY_MS = 1000;

    // Retry configuration for retrieving the statement
    /* package */ static final int MAX_RETRIES = 10;
    /* package */ static final long INITIAL_DELAY_MS = 2000;
    /* package */ static final long MAX_DELAY_MS = 10000;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final HttpRequestExecutor httpExecutor;
    private final Sleeper sleeper;

    private final AtomicReference<WebAccess> inFlight = new AtomicReference<>();
    private volatile boolean canceled;

    /**
     * Creates a client using the default HTTP executor (WebAccess).
     */
    public IBFlexWebServiceClient()
    {
        this.httpExecutor = this::defaultHttpExecutor;
        this.sleeper = Thread::sleep;
    }

    /**
     * Creates a client with a custom HTTP executor (for testing).
     */
    public IBFlexWebServiceClient(HttpRequestExecutor httpExecutor)
    {
        this(httpExecutor, Thread::sleep);
    }

    /**
     * Creates a client with a custom HTTP executor and sleeper (for testing).
     */
    public IBFlexWebServiceClient(HttpRequestExecutor httpExecutor, Sleeper sleeper)
    {
        this.httpExecutor = httpExecutor;
        this.sleeper = sleeper;
    }

    /**
     * Sends the request as POST with the token and query id / reference code
     * as form-encoded body.
     * <p>
     * This diverges from IB's documentation and the reference implementations,
     * which send all parameters in the URL of a GET request. Error messages of
     * the HTTP layer (and therefore the log) include the URL, which would leak
     * the token. IB accepts both SendRequest and GetStatement as POST.
     */
    private String defaultHttpExecutor(String host, String path, Map<String, String> query,
                    Map<String, String> form) throws IOException
    {
        try
        {
            var appVersion = FrameworkUtil.getBundle(IBFlexWebServiceClient.class).getVersion().toString();
            var request = new WebAccess(host, path) //
                            .addUserAgent("PortfolioPerformance/" + appVersion); //$NON-NLS-1$
            query.forEach(request::addParameter);

            inFlight.set(request);
            if (canceled)
                request.cancel();

            return request.postForm(form);
        }
        catch (WebAccess.WebAccessException e)
        {
            throw new IOException(MessageFormat.format(Messages.IBFlexMsgErrorHttpRequest, e.getHttpErrorCode()), e);
        }
        finally
        {
            inFlight.set(null);
        }
    }

    /**
     * Fetches a Flex statement from the IB Web Service with cancellation
     * support.
     *
     * @param token
     *            The Flex Web Service token
     * @param queryId
     *            The Flex Query ID
     * @return The XML statement as a string
     * @throws IBFlexException
     *             if the API returns an error or operation is canceled
     * @throws IOException
     *             if a network error occurs
     */
    public String fetchStatement(String token, String queryId) throws IBFlexException, IOException
    {
        return fetchStatement(token, queryId, null);
    }

    /**
     * Fetches a Flex statement for the given date range.
     *
     * @param range
     *            the date range to report on, at most
     *            {@link IBFlexDateRange#MAX_DAYS_PER_REQUEST} days; if null,
     *            the period configured in the Flex Query is used
     */
    public String fetchStatement(String token, String queryId, IBFlexDateRange range)
                    throws IBFlexException, IOException
    {
        var query = new LinkedHashMap<String, String>();

        if (range != null)
        {
            if (range.split().size() > 1)
                throw new IllegalArgumentException("date range exceeds " + IBFlexDateRange.MAX_DAYS_PER_REQUEST
                                + " days: " + range);

            query.put("fd", DATE_FORMAT.format(range.from()));
            query.put("td", DATE_FORMAT.format(range.to()));
        }

        // do not send another request once the user canceled
        checkCanceled();

        // errors of the initial request are not retried
        var access = parseStatementAccess(execute(SEND_REQUEST_HOST, SEND_REQUEST_PATH, query,
                        credentials(token, queryId)));

        // the statement is never ready immediately
        sleep(STATEMENT_DELAY_MS);

        return withRetry(() -> fetchStatementOnce(token, access));
    }

    /**
     * Fetches Flex statements covering the given date range. Ranges longer
     * than {@link IBFlexDateRange#MAX_DAYS_PER_REQUEST} days are split into
     * multiple requests.
     *
     * @param progress
     *            called before each request; may be null
     * @return one statement per request, in chronological order
     */
    public List<String> fetchStatements(String token, String queryId, IBFlexDateRange range,
                    ProgressListener progress) throws IBFlexException, IOException
    {
        var chunks = range.split();
        var statements = new ArrayList<String>();

        for (int index = 0; index < chunks.size(); index++)
        {
            if (index > 0)
                sleep(REQUEST_DELAY_MS);

            if (progress != null)
                progress.onRequest(index + 1, chunks.size(), chunks.get(index));

            statements.add(fetchStatement(token, queryId, chunks.get(index)));
        }

        checkCanceled();

        return statements;
    }

    /**
     * Cancels the fetch: aborts the HTTP request currently in flight and
     * prevents further requests. May be called from any thread. The fetching
     * thread fails with an {@link IBFlexException} for which
     * {@link IBFlexException#isCanceled()} is true.
     */
    public void cancel()
    {
        canceled = true;
        var request = inFlight.get();
        if (request != null)
            request.cancel();
    }

    private void checkCanceled() throws IBFlexException
    {
        if (canceled || Thread.currentThread().isInterrupted())
            throw new IBFlexException(Messages.IBFlexMsgErrorInterrupted, new InterruptedException());
    }

    private String execute(String host, String path, Map<String, String> query, Map<String, String> form)
                    throws IBFlexException, IOException
    {
        try
        {
            return httpExecutor.execute(host, path, query, form);
        }
        catch (IOException e)
        {
            // an aborted request fails with an I/O error
            checkCanceled();
            throw e;
        }
    }

    private static Map<String, String> credentials(String token, String queryOrRef)
    {
        var parameters = new LinkedHashMap<String, String>();
        parameters.put("t", token);
        parameters.put("q", queryOrRef);
        parameters.put("v", String.valueOf(VERSION));
        return parameters;
    }

    private void sleep(long millis) throws IBFlexException
    {
        try
        {
            sleeper.sleep(millis);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IBFlexException(Messages.IBFlexMsgErrorInterrupted, e);
        }
    }

    /* package */ StatementAccess parseStatementAccess(String xml) throws IBFlexException, IOException
    {
        try
        {
            Document doc = parseXml(xml);
            throwOnError(doc);

            // Get reference code from success response
            NodeList refNodes = doc.getElementsByTagName("ReferenceCode"); //$NON-NLS-1$
            if (refNodes.getLength() == 0)
            {
                throw new IBFlexException(Messages.IBFlexMsgErrorNoReferenceCode,
                                new IllegalStateException("Missing ReferenceCode in response")); //$NON-NLS-1$
            }

            // Get the URL to retrieve the statement from
            NodeList urlNodes = doc.getElementsByTagName("Url");
            if (urlNodes.getLength() == 0)
            {
                throw new IBFlexException(Messages.IBFlexMsgErrorNoStatementUrl,
                                new IllegalStateException("Missing Url in response"));
            }

            var url = new URI(urlNodes.item(0).getTextContent().trim());
            if (url.getHost() == null)
                throw new IBFlexException(Messages.IBFlexMsgErrorNoStatementUrl,
                                new IllegalStateException("Invalid Url in response: " + url));

            return new StatementAccess(refNodes.item(0).getTextContent().trim(), url);
        }
        catch (ParserConfigurationException | SAXException | URISyntaxException e)
        {
            throw new IBFlexException(Messages.IBFlexMsgErrorParsingResponse, e);
        }
    }

    @FunctionalInterface
    private interface Request
    {
        String run() throws IBFlexException, IOException;
    }

    /**
     * Runs the request, retrying with exponential backoff as long as IB
     * reports a temporary error.
     */
    private String withRetry(Request request) throws IBFlexException, IOException
    {
        long delay = INITIAL_DELAY_MS;
        IBFlexException lastError = null;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++)
        {
            if (attempt > 0)
            {
                sleep(delay);
                delay = Math.min(delay * 2, MAX_DELAY_MS);
            }

            checkCanceled();

            try
            {
                return request.run();
            }
            catch (IBFlexException e)
            {
                if (!e.isRetryable())
                    throw e;
                lastError = e;
            }
        }

        throw new IBFlexException(lastError.getErrorCode(),
                        Messages.IBFlexMsgErrorStatementNotReady + "\n\n" + lastError.getMessage());
    }

    private String fetchStatementOnce(String token, StatementAccess access) throws IBFlexException, IOException
    {
        String response = execute(access.url().getHost(), access.url().getPath(), Map.of(),
                        credentials(token, access.referenceCode()));

        checkForErrorResponse(response);

        // Response is the actual Flex statement XML
        return response;
    }

    /* package */ void checkForErrorResponse(String xml) throws IBFlexException, IOException
    {
        // statements can be large, so do not parse them just to tell them
        // apart from the (small) error responses
        if (xml.contains("<FlexQueryResponse"))
            return;

        try
        {
            throwOnError(parseXml(xml));
        }
        catch (ParserConfigurationException | SAXException e)
        {
            throw new IBFlexException(Messages.IBFlexMsgErrorParsingResponse, e);
        }

        // neither a statement nor an error
        throw new IBFlexException(Messages.IBFlexMsgErrorParsingResponse,
                        new IllegalStateException("Unexpected response from GetStatement"));
    }

    private void throwOnError(Document doc) throws IBFlexException
    {
        NodeList errorNodes = doc.getElementsByTagName("ErrorCode"); //$NON-NLS-1$
        if (errorNodes.getLength() > 0)
        {
            int errorCode = parseErrorCode(errorNodes.item(0).getTextContent().trim());
            String errorMessage = getErrorMessage(doc, errorCode);
            throw new IBFlexException(errorCode, errorMessage);
        }
    }

    private int parseErrorCode(String text)
    {
        try
        {
            return Integer.parseInt(text);
        }
        catch (NumberFormatException e)
        {
            // Return -1 for unknown/unparseable error codes
            return -1;
        }
    }

    private String getErrorMessage(Document doc, int errorCode)
    {
        NodeList msgNodes = doc.getElementsByTagName("ErrorMessage"); //$NON-NLS-1$
        if (msgNodes.getLength() > 0)
            return msgNodes.item(0).getTextContent().trim();
        return MessageFormat.format(Messages.IBFlexMsgErrorUnknown, errorCode);
    }

    private Document parseXml(String xml) throws ParserConfigurationException, SAXException, IOException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); //$NON-NLS-1$

        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }
}
