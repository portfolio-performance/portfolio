package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.InterestRateToSecurityPricesConverter;
import name.abuchen.portfolio.online.InterestRateToSecurityPricesConverter.InterestRateType;
import name.abuchen.portfolio.online.InterestRateToSecurityPricesConverter.Interval;
import name.abuchen.portfolio.online.InterestRateToSecurityPricesConverter.Maturity;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.util.Pair;
import name.abuchen.portfolio.util.TradeCalendar;
import name.abuchen.portfolio.util.TradeCalendarManager;
import name.abuchen.portfolio.util.WebAccess;

public class ECBStatisticalDataWarehouseQuoteFeed implements QuoteFeed
{

    public static final String ID = "ECBSDW"; //$NON-NLS-1$

    private static final String ECB_SDW_PROTOCOL = "https"; //$NON-NLS-1$
    private static final String ECB_SDW_HOST = "sdw-wsrest.ecb.europa.eu"; //$NON-NLS-1$
    private static final String ECB_SDW_DATA_PATH = "/service/data/"; //$NON-NLS-1$
    private static final String MONTH_REGEX = "\\d\\d\\d\\d-\\d\\d"; //$NON-NLS-1$
    private static final String MONTH_POSTFIX = "-01"; //$NON-NLS-1$
    private static final String ECB_SDW_DATE_FORMAT = "yyyy-MM-dd"; //$NON-NLS-1$

    private static final String ESTR_EONIA_COMBINED_ID = "€STR/EONIA"; //$NON-NLS-1$
    private static final String ESTR_ID = "ECB,EST,1.0/B.EU000A2QQF08.CI"; //$NON-NLS-1$
    private static final String EONIA_ID = "ECB,EON,1.0/D.EONIA_TO.RATE"; //$NON-NLS-1$

    public enum ECBSDWSeries
    {
        ESTR(ESTR_ID, Messages.LabelESTR),
        ESTREONIA(ESTR_EONIA_COMBINED_ID, Messages.LabelESTREONIACombined),
        EONIA(EONIA_ID, Messages.LabelEONIA),
        EURIBOR1M("ECB,FM,1.0/M.U2.EUR.RT.MM.EURIBOR1MD_.HSTA", Messages.LabelEURIBOR1M), //$NON-NLS-1$
        EURIBOR3M("ECB,FM,1.0/M.U2.EUR.RT.MM.EURIBOR3MD_.HSTA", Messages.LabelEURIBOR3M), //$NON-NLS-1$
        EURIBOR6M("ECB,FM,1.0/M.U2.EUR.RT.MM.EURIBOR6MD_.HSTA", Messages.LabelEURIBOR6M), //$NON-NLS-1$
        EURIBOR1Y("ECB,FM,1.0/M.U2.EUR.RT.MM.EURIBOR1YD_.HSTA", Messages.LabelEURIBOR1Y), //$NON-NLS-1$
        EURGBBY2Y("ECB,FM,1.0/M.U2.EUR.4F.BB.U2_2Y.YLD", Messages.LabelEURGBBY2Y), //$NON-NLS-1$
        EURGBBY3Y("ECB,FM,1.0/M.U2.EUR.4F.BB.U2_3Y.YLD", Messages.LabelEURGBBY3Y), //$NON-NLS-1$
        EURGBBY5Y("ECB,FM,1.0/M.U2.EUR.4F.BB.U2_5Y.YLD", Messages.LabelEURGBBY5Y), //$NON-NLS-1$
        EURGBBY7Y("ECB,FM,1.0/M.U2.EUR.4F.BB.U2_7Y.YLD", Messages.LabelEURGBBY7Y), //$NON-NLS-1$
        EURGBBY10Y("ECB,FM,1.0/M.U2.EUR.4F.BB.U2_10Y.YLD", Messages.LabelEURGBBY10Y), //$NON-NLS-1$
        JPYLIBOR3M("ECB,FM,1.0/M.GB.JPY.RT.MM.JPY3MFSR_.HSTA", Messages.LabelJPYLIBOR3M), //$NON-NLS-1$
        JPYGBBY10Y("ECB,FM,1.0/M.JP.JPY.4F.BB.JP10YT_RR.YLDA", Messages.LabelJPYGBBY10Y), //$NON-NLS-1$
        USDLIBOR3M("ECB,FM,1.0/M.GB.USD.RT.MM.USD3MFSR_.HSTA", Messages.LabelUSDLIBOR3M), //$NON-NLS-1$
        USDGBBY10Y("ECB,FM,1.0/M.US.USD.4F.BB.US10YT_RR.YLDA", Messages.LabelUSDGBBY10Y), //$NON-NLS-1$
        GBPLIBOR3M("ECB,FM,1.0/M.GB.JPY.RT.MM.JPY3MFSR_.HSTA", Messages.LabelGBPLIBOR3M), //$NON-NLS-1$
        DKKINTERBANKOFFERED3M("ECB,FM,1.0/M.DK.DKK.DS.MM.CIBOR3M.ASKA", Messages.LabelDKKINTERBANKOFFERED3M), //$NON-NLS-1$
        SEKINTERBANKOFFERED3M("ECB,FM,1.0/M.SE.SEK.DS.MM.SIBOR3M.ASKA", Messages.LabelSEKINTERBANKOFFERED3M); //$NON-NLS-1$

        private final Exchange exchange;

        private ECBSDWSeries(String id, String name)
        {
            this.exchange = new Exchange(id, name, Exchange.DISPLAY_NAME_FORMAT_EXCHANGE_NAME_ONLY);
        }
    }

    private static InterestRateToSecurityPricesConverter.Interval getInterval(String freq)
    {
        switch (freq)
        {
            case "B": //$NON-NLS-1$
            case "D": //$NON-NLS-1$
                return InterestRateToSecurityPricesConverter.Interval.DAILY;
            case "M": //$NON-NLS-1$
                return InterestRateToSecurityPricesConverter.Interval.MONTHLY;
            default:
                return null;
        }
    }

    private static InterestRateToSecurityPricesConverter.Maturity getMaturity(String provider_fm_id)
    {
        if (provider_fm_id == null)
        {
            // EONIA data is from EON, not FM, so it does not have a
            // provider_fm_id
            return InterestRateToSecurityPricesConverter.Maturity.OVER_NIGHT;
        }
        switch (provider_fm_id)
        {
            case "EURIBOR1MD_": //$NON-NLS-1$
                return InterestRateToSecurityPricesConverter.Maturity.ONE_MONTH;
            case "EURIBOR2MD_": //$NON-NLS-1$
                return InterestRateToSecurityPricesConverter.Maturity.TWO_MONTHS;
            case "EURIBOR3MD_": //$NON-NLS-1$
            case "JPY3MFSR_": //$NON-NLS-1$
            case "USD3MFSR_": //$NON-NLS-1$
            case "CIBOR3M": //$NON-NLS-1$
            case "SIBOR3M": //$NON-NLS-1$
                return InterestRateToSecurityPricesConverter.Maturity.THREE_MONTHS;
            case "EURIBOR6MD_": //$NON-NLS-1$
                return InterestRateToSecurityPricesConverter.Maturity.SIX_MONTHS;
            case "EURIBOR1YD_": //$NON-NLS-1$
                return InterestRateToSecurityPricesConverter.Maturity.ONE_YEAR;
            case "U2_2Y": //$NON-NLS-1$
                return InterestRateToSecurityPricesConverter.Maturity.TWO_YEARS;
            case "U2_3Y": //$NON-NLS-1$
                return InterestRateToSecurityPricesConverter.Maturity.THREE_YEARS;
            case "U2_5Y": //$NON-NLS-1$
                return InterestRateToSecurityPricesConverter.Maturity.FIVE_YEARS;
            case "U2_7Y": //$NON-NLS-1$
                return InterestRateToSecurityPricesConverter.Maturity.SEVEN_YEARS;
            case "U2_10Y": //$NON-NLS-1$
            case "JP10YT_RR": //$NON-NLS-1$
            case "US10YT_RR": //$NON-NLS-1$
                return InterestRateToSecurityPricesConverter.Maturity.TEN_YEARS;
            default:
                return null;
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
            if (security.getTickerSymbol().equals(ESTR_EONIA_COMBINED_ID))
            {
                // first add ESTR data
                String responseBody = requestData(ESTR_ID, collectRawResponse, data);
                extractQuotes(responseBody, data);

                // then get EONIA data
                String eoniaString = requestData(EONIA_ID, collectRawResponse, data);
                ExtractedData extractedData = extractData(eoniaString);
                // subtract 8.5 base points from EONIA index
                // Since publishing €STR in 2019-10-02, EONIA was calculated as
                // €STR + 8.5 base points
                // To get a consistent index, we have to subtract 8.5 pase
                // points from the EONIA index.
                BigDecimal toSubtract = new BigDecimal("0.085"); //$NON-NLS-1$
                extractedData.dataSeries = extractedData.dataSeries.stream()
                                .map(pair -> new Pair<>(pair.getLeft(), pair.getRight().subtract(toSubtract)))
                                .collect(Collectors.toList());
                Collection<Pair<LocalDate, BigDecimal>> eoniaPrices = new InterestRateToSecurityPricesConverter(
                                InterestRateType.ACT_360).convertBigDecimal(extractedData.dataSeries, Interval.DAILY,
                                                Maturity.OVER_NIGHT);
                // Align with €STR index: On 2019-10-01 this index has the value
                // 100
                BigDecimal alignFactor = new BigDecimal(100).multiply(Values.Quote.getBigDecimalFactor()).divide(eoniaPrices.stream()
                                .filter(pair -> pair.getLeft().isAfter(LocalDate.of(2019, 10, 01).minusDays(1)))
                                .findFirst().orElseThrow().getRight(), 20, RoundingMode.HALF_EVEN);
                List<LatestSecurityPrice> pricesToAdd = eoniaPrices.stream()
                                .filter(pair -> pair.getLeft().isBefore(LocalDate.of(2019, 10, 01)))
                                .map(pair -> new Pair<>(pair.getLeft(), pair.getRight().multiply(alignFactor)))
                                .map(pair -> InterestRateToSecurityPricesConverter.toLatestSecurityPrice(pair.getLeft(),
                                                pair.getRight()))
                                .toList();
                data.addAllPrices(pricesToAdd);
            }
            else
            {
                String responseBody = requestData(security, collectRawResponse, data);
                extractQuotes(responseBody, data);
            }
        }
        catch (IOException | URISyntaxException | ParserConfigurationException | SAXException e)
        {
            data.addError(new IOException(MessageFormat.format(Messages.MsgErrorDownloadECBStatisticalDataWarehouse, 1,
                            security.getTickerSymbol(), e.getMessage()), e));
        }

        return data;
    }

    private String requestData(Security security, boolean collectRawResponse, QuoteFeedData data)
                    throws IOException, URISyntaxException
    {
        return requestData(security.getTickerSymbol(), collectRawResponse, data);
    }

    private String requestData(String dataSeriesID, boolean collectRawResponse, QuoteFeedData data)
                    throws IOException, URISyntaxException
    {
        WebAccess webaccess = new WebAccess(ECB_SDW_HOST, ECB_SDW_DATA_PATH + dataSeriesID) //
                        .withScheme(ECB_SDW_PROTOCOL); //

        String text = webaccess.get();

        if (collectRawResponse)
            data.addResponse(webaccess.getURL(), text);

        return text;
    }

    private class ExtractedData
    {
        InterestRateToSecurityPricesConverter.Interval interval = null;
        InterestRateToSecurityPricesConverter.Maturity maturity = null;
        String providerIdFM = null;
        String eoniaItem = null;
        List<Pair<LocalDate, BigDecimal>> dataSeries;
    }

    private void extractQuotes(String responseBody, QuoteFeedData data)
                    throws ParserConfigurationException, SAXException, IOException
    {
        ExtractedData extractedData = extractData(responseBody);
        // EONIA and data from FM dataset need conversion from interest rate to
        // prices
        if (extractedData.providerIdFM != null || extractedData.eoniaItem != null)
        {
            Collection<LatestSecurityPrice> latestSecurityPrices = new InterestRateToSecurityPricesConverter(
                            InterestRateToSecurityPricesConverter.InterestRateType.ACT_360).convert(
                                            extractedData.dataSeries, extractedData.interval, extractedData.maturity);
            data.addAllPrices(latestSecurityPrices);
        }
        else
        {
            data.addAllPrices(extractedData.dataSeries.stream()
                            .map((pair -> new LatestSecurityPrice(pair.getLeft(),
                                            pair.getRight().multiply(Values.Quote.getBigDecimalFactor())
                                                            .setScale(0, RoundingMode.HALF_UP).longValue(),
                                            LatestSecurityPrice.NOT_AVAILABLE, LatestSecurityPrice.NOT_AVAILABLE,
                                            LatestSecurityPrice.NOT_AVAILABLE)))
                            .toList());
        }
    }

    private ExtractedData extractData(String responseBody)
                    throws ParserConfigurationException, SAXException, IOException
    {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); //$NON-NLS-1$
        dbFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder builder = dbFactory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(responseBody)));

        Element root = document.getDocumentElement();

        NodeList seriesKeys = root.getElementsByTagName("generic:SeriesKey"); //$NON-NLS-1$
        if (seriesKeys.getLength() != 1)
            throw new SAXException("Expected one generic:SeriesKey\", but found: " + seriesKeys.getLength()); //$NON-NLS-1$
        Element seriesKey = (Element) seriesKeys.item(0);
        NodeList seriesKeyValues = seriesKey.getElementsByTagName("generic:Value"); //$NON-NLS-1$

        InterestRateToSecurityPricesConverter.Interval interval = null;
        InterestRateToSecurityPricesConverter.Maturity maturity = null;
        String providerIdFM = null;
        String eoniaItem = null;
        for (int i = 0; i < seriesKeyValues.getLength(); i++)
        {
            Element e = (Element) seriesKeyValues.item(i);
            if (e.getAttribute("id").equals("FREQ")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                String freq = e.getAttribute("value"); //$NON-NLS-1$
                interval = getInterval(freq);
            }
            if (e.getAttribute("id").equals("PROVIDER_FM_ID")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                providerIdFM = e.getAttribute("value"); //$NON-NLS-1$
            }
            if (e.getAttribute("id").equals("EONIA_ITEM")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                eoniaItem = e.getAttribute("value"); //$NON-NLS-1$
            }
        }
        maturity = getMaturity(providerIdFM);

        NodeList dataList = root.getElementsByTagName("generic:Obs"); //$NON-NLS-1$

        List<Pair<LocalDate, BigDecimal>> dataSeries = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(ECB_SDW_DATE_FORMAT);
        LocalDate lastInterestRateDay = LocalDate.MIN;

        for (int i = 0; i < dataList.getLength(); i++)
        {
            Element node = (Element) dataList.item(i);

            NodeList dateNodeList = node.getElementsByTagName("generic:ObsDimension"); //$NON-NLS-1$
            if (dateNodeList.getLength() != 1)
                throw new SAXException("Expected one date, but found: " + dateNodeList.getLength()); //$NON-NLS-1$
            Element dateElement = (Element) dateNodeList.item(0);
            String dateString = dateElement.getAttribute("value"); //$NON-NLS-1$
            if (dateString.matches(MONTH_REGEX))
            {
                // If there is only one datapoint per month, we set the date to
                // the first date of the month.
                // When converting interest rates to cummulated indices, the
                // InterestRateToSecurityPricesConverter
                // will then use this interest for every day of the month
                dateString = dateString + MONTH_POSTFIX;
            }
            LocalDate date = LocalDate.parse(dateString, formatter);

            if (providerIdFM != null) // Data comes from FM
            {
                // The data is a monthly average. We interpret it as end of the
                // month datapoint.
                date = date.plusMonths(1);
            }

            if (date.isAfter(lastInterestRateDay))
                lastInterestRateDay = date;

            NodeList interestRateNodeList = node.getElementsByTagName("generic:ObsValue"); //$NON-NLS-1$
            if (interestRateNodeList.getLength() != 1)
                throw new SAXException("Expected one value, but found: " + interestRateNodeList.getLength()); //$NON-NLS-1$
            Element interestRateElement = (Element) interestRateNodeList.item(0);
            String interestRateString = interestRateElement.getAttribute("value"); //$NON-NLS-1$

            // This check is necessary because for USA 10year yield NaN is
            // returned in the period 1914-08 - 1914-11
            if (!interestRateString.equals("NaN")) //$NON-NLS-1$
            {
                BigDecimal interestRate = new BigDecimal(interestRateString);
                dataSeries.add(new Pair<>(date, interestRate));
            }
        }

        if (eoniaItem != null) // Is EONIA index?
        {
            TradeCalendar tradeCalendar = TradeCalendarManager.getInstance(TradeCalendarManager.TARGET2_CALENDAR_CODE);
            LocalDate lastQuoteDate = tradeCalendar.getNextNonHoliday(lastInterestRateDay.plusDays(1));
            // EONIA is an over-night index, so it has modified duration 0 and
            // we can calculate the quote for one
            // more day given the current interest rate.
            dataSeries.add(new Pair<>(lastQuoteDate, new BigDecimal(0)));
        }
        ExtractedData extractedData = new ExtractedData();
        extractedData.dataSeries = dataSeries;
        extractedData.eoniaItem = eoniaItem;
        extractedData.providerIdFM = providerIdFM;
        extractedData.interval = interval;
        extractedData.maturity = maturity;
        return extractedData;
    }

    @Override
    public List<Exchange> getExchanges(Security subject, List<Exception> errors)
    {
        return Arrays.stream(ECBSDWSeries.values()).map(series -> series.exchange).collect(Collectors.toList());
    }
}
