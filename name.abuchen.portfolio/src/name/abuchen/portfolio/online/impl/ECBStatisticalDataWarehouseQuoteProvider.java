package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.InterestRateToSecurityPricesConverter;
import name.abuchen.portfolio.online.InterestRateToSecurityPricesConverter.InterestRateType;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.util.Pair;
import name.abuchen.portfolio.util.WebAccess;

public class ECBStatisticalDataWarehouseQuoteProvider implements QuoteFeed
{

    public static final String ID = "ECBSDW"; //$NON-NLS-1$

    private static final String ECB_SDW_PROTOCOL = "https"; //$NON-NLS-1$
    private static final String ECB_SDW_HOST = "sdw-wsrest.ecb.europa.eu"; //$NON-NLS-1$
    private static final String ECB_SDW_DATA_PATH = "/service/data/"; //$NON-NLS-1$
    private static final String ECB_SDW_DATE_FORMAT = "yyyy-MM-dd"; //$NON-NLS-1$

    public enum ECBSDWSeries
    {
        EONIA(new Exchange("ECB,EON,1.0/D.EONIA_TO.RATE", Messages.LabelEONIA)); //$NON-NLS-1$

        private final Exchange exchange;

        private ECBSDWSeries(Exchange exchange)
        {
            this.exchange = exchange;
        }
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return Messages.LabelECBStatisticalDataWarehouse;
    }

    @Override
    public Optional<LatestSecurityPrice> getLatestQuote(Security security)
    {
        return Optional.empty();
    }

    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse)
    {
        if (security.getTickerSymbol() == null)
        {
            return QuoteFeedData.withError(
                            new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName())));
        }

        QuoteFeedData data = new QuoteFeedData();

        try
        {
            String responseBody = requestData(security, collectRawResponse, data);
            extractQuotes(responseBody, data);
        }
        catch (IOException | URISyntaxException | ParserConfigurationException | SAXException e)
        {
            data.addError(new IOException(MessageFormat.format(Messages.MsgErrorDownloadECBStatisticalDataWarehouse, 1,
                            security.getTickerSymbol(), e.getMessage()), e));
        }

        return data;
    }

    @SuppressWarnings("nls")
    private String requestData(Security security, boolean collectRawResponse, QuoteFeedData data)
                    throws IOException, URISyntaxException
    {
        WebAccess webaccess = new WebAccess(ECB_SDW_HOST, ECB_SDW_DATA_PATH + security.getTickerSymbol()) //
                        .withScheme(ECB_SDW_PROTOCOL); // //$NON-NLS-1$

        String text = webaccess.get();

        if (collectRawResponse)
            data.addResponse(webaccess.getURL(), text);

        return text;
    }

    private void extractQuotes(String responseBody, QuoteFeedData data)
                    throws ParserConfigurationException, SAXException, IOException
    {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder builder = dbFactory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(responseBody)));

        Element root = document.getDocumentElement();
        NodeList dataList = root.getElementsByTagName("generic:Obs"); //$NON-NLS-1$

        List<Pair<LocalDate, Double>> interestRates = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(ECB_SDW_DATE_FORMAT);

        for (int i = 0; i < dataList.getLength(); i++)
        {
            Element node = (Element) dataList.item(i);

            NodeList dateNodeList = node.getElementsByTagName("generic:ObsDimension"); //$NON-NLS-1$
            if (dateNodeList.getLength() != 1)
                throw new SAXException("Expected one date, but found: " + dateNodeList.getLength()); //$NON-NLS-1$
            Element dateElement = (Element) dateNodeList.item(0);
            String dateString = dateElement.getAttribute("value"); //$NON-NLS-1$
            LocalDate date = LocalDate.parse(dateString, formatter);

            NodeList interestRateNodeList = node.getElementsByTagName("generic:ObsValue"); //$NON-NLS-1$
            if (interestRateNodeList.getLength() != 1)
                throw new SAXException("Expected one value, but found: " + interestRateNodeList.getLength()); //$NON-NLS-1$
            Element interestRateElement = (Element) interestRateNodeList.item(0);
            String interestRateString = interestRateElement.getAttribute("value"); //$NON-NLS-1$
            double interestRate = Double.parseDouble(interestRateString);
            interestRates.add(new Pair<LocalDate, Double>(date, interestRate));
        }
        Collection<LatestSecurityPrice> latestSecurityPrices = new InterestRateToSecurityPricesConverter(
                        InterestRateType.ACT_360).convert(interestRates);
        data.addAllPrices(latestSecurityPrices);
    }

    @Override
    public List<Exchange> getExchanges(Security subject, List<Exception> errors)
    {
        return Arrays.stream(ECBSDWSeries.values()).map(series -> series.exchange).collect(Collectors.toList());
    }
}
