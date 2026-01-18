package name.abuchen.portfolio.datatransfer.ibflex;

import java.io.IOException;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.function.BooleanSupplier;

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
@SuppressWarnings("nls")
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
         * Returns true if this error indicates the token has expired.
         */
        public boolean isTokenExpired()
        {
            return errorCode == ERROR_TOKEN_INVALID;
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
     * Functional interface for making HTTP GET requests. Allows mocking in
     * tests.
     */
    @FunctionalInterface
    public interface HttpRequestExecutor
    {
        String execute(String host, String path, String token, String queryOrRef, int version) throws IOException;
    }

    private static final String SEND_REQUEST_HOST = "gdcdyn.interactivebrokers.com";
    private static final String SEND_REQUEST_PATH = "/Universal/servlet/FlexStatementService.SendRequest";
    private static final String GET_STATEMENT_PATH = "/Universal/servlet/FlexStatementService.GetStatement";
    private static final int VERSION = 3;

    // Error codes from IB documentation
    /* package */ static final int ERROR_STATEMENT_GENERATING = 1019;
    /* package */ static final int ERROR_TOKEN_INVALID = 1003;

    // Retry configuration
    /* package */ static final int MAX_RETRIES = 10;
    /* package */ static final long INITIAL_DELAY_MS = 2000;
    /* package */ static final long MAX_DELAY_MS = 30000;

    private final HttpRequestExecutor httpExecutor;

    /**
     * Creates a client using the default HTTP executor (WebAccess).
     */
    public IBFlexWebServiceClient()
    {
        this(IBFlexWebServiceClient::defaultHttpExecutor);
    }

    /**
     * Creates a client with a custom HTTP executor (for testing).
     */
    public IBFlexWebServiceClient(HttpRequestExecutor httpExecutor)
    {
        this.httpExecutor = httpExecutor;
    }

    private static String defaultHttpExecutor(String host, String path, String token, String queryOrRef, int version)
                    throws IOException
    {
        try
        {
            var appVersion = FrameworkUtil.getBundle(IBFlexWebServiceClient.class).getVersion().toString();
            return new WebAccess(host, path)
                            .addUserAgent("PortfolioPerformance/" + appVersion)
                            .addParameter("t", token)
                            .addParameter("q", queryOrRef)
                            .addParameter("v", String.valueOf(version))
                            .get();
        }
        catch (WebAccess.WebAccessException e)
        {
            throw new IOException(
                            MessageFormat.format(Messages.IBFlexMsgErrorHttpRequest, e.getHttpErrorCode()), e);
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
     * @param isCanceled
     *            Supplier that returns true if the operation should be canceled
     * @return The XML statement as a string
     * @throws IBFlexException
     *             if the API returns an error or operation is canceled
     * @throws IOException
     *             if a network error occurs
     */
    public String fetchStatement(String token, String queryId, BooleanSupplier isCanceled)
                    throws IBFlexException, IOException
    {
        String requestStatementResponse = httpExecutor.execute(SEND_REQUEST_HOST, SEND_REQUEST_PATH, token, queryId,
                        VERSION);
        String referenceCode = parseReferenceCode(requestStatementResponse);

        return fetchStatementWithRetry(token, referenceCode, isCanceled);
    }

    /* package */ String parseReferenceCode(String xml) throws IBFlexException, IOException
    {
        try
        {
            Document doc = parseXml(xml);

            // Check for error response
            NodeList errorNodes = doc.getElementsByTagName("ErrorCode");
            if (errorNodes.getLength() > 0)
            {
                int errorCode = parseErrorCode(errorNodes.item(0).getTextContent().trim());
                String errorMessage = getErrorMessage(doc, errorCode);
                throw new IBFlexException(errorCode, errorMessage);
            }

            // Get reference code from success response
            NodeList refNodes = doc.getElementsByTagName("ReferenceCode");
            if (refNodes.getLength() == 0)
            {
                throw new IBFlexException(Messages.IBFlexMsgErrorNoReferenceCode,
                                new IllegalStateException("Missing ReferenceCode in response"));
            }

            return refNodes.item(0).getTextContent().trim();
        }
        catch (ParserConfigurationException | SAXException e)
        {
            throw new IBFlexException(Messages.IBFlexMsgErrorParsingResponse, e);
        }
    }

    private String fetchStatementWithRetry(String token, String referenceCode, BooleanSupplier isCanceled)
                    throws IBFlexException, IOException
    {
        long delay = INITIAL_DELAY_MS;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++)
        {
            // Check for cancellation before each attempt
            if (isCanceled.getAsBoolean())
                throw new IBFlexException(Messages.IBFlexMsgErrorInterrupted, new InterruptedException());

            try
            {
                return fetchStatementOnce(token, referenceCode);
            }
            catch (IBFlexException e)
            {
                if (!e.isStatementGenerating())
                    throw e;

                // Statement still generating, wait and retry
                try
                {
                    Thread.sleep(delay);
                }
                catch (InterruptedException ie)
                {
                    Thread.currentThread().interrupt();
                    throw new IBFlexException(Messages.IBFlexMsgErrorInterrupted, ie);
                }

                // Exponential backoff, capped at MAX_DELAY_MS
                delay = Math.min(delay * 2, MAX_DELAY_MS);
            }
        }

        throw new IBFlexException(ERROR_STATEMENT_GENERATING, Messages.IBFlexMsgErrorStatementNotReady);
    }

    private String fetchStatementOnce(String token, String referenceCode) throws IBFlexException, IOException
    {
        String response = httpExecutor.execute(SEND_REQUEST_HOST, GET_STATEMENT_PATH, token, referenceCode, VERSION);

        // Check if response is an error XML
        if (response.trim().startsWith("<FlexStatementResponse"))
        {
            parseErrorResponse(response);
        }

        // Response is the actual Flex statement XML
        return response;
    }

    /* package */ void parseErrorResponse(String xml) throws IBFlexException, IOException
    {
        try
        {
            Document doc = parseXml(xml);

            NodeList errorNodes = doc.getElementsByTagName("ErrorCode");
            if (errorNodes.getLength() > 0)
            {
                int errorCode = parseErrorCode(errorNodes.item(0).getTextContent().trim());
                String errorMessage = getErrorMessage(doc, errorCode);
                throw new IBFlexException(errorCode, errorMessage);
            }
        }
        catch (ParserConfigurationException | SAXException e)
        {
            throw new IBFlexException(Messages.IBFlexMsgErrorParsingResponse, e);
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
        NodeList msgNodes = doc.getElementsByTagName("ErrorMessage");
        if (msgNodes.getLength() > 0)
        {
            return msgNodes.item(0).getTextContent().trim();
        }
        return MessageFormat.format(Messages.IBFlexMsgErrorUnknown, errorCode);
    }

    private Document parseXml(String xml) throws ParserConfigurationException, SAXException, IOException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }
}
