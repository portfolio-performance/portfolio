package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.annotations.VisibleForTesting;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.impl.TLVMarket.TLVEntities;
import name.abuchen.portfolio.online.impl.TLVMarket.TLVFund;
import name.abuchen.portfolio.online.impl.TLVMarket.TLVSecurity;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.FundHistory;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.IndiceListing;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.SecurityHistory;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.Language;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.SecuritySubType;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.SecurityType;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.TLVType;

/*
 * Developer portal: https://datahubapi.tase.co.il/ List of indices -
 * https://datahubapi.tase.co.il/spec/8149ecc8-ca0f-4391-92bf-79f69587919d/
 * 4301d3e3-3dae-4844-bae4-6cf4d103bba8 Securities basic -
 * https://datahubapi.tase.co.il/spec/089150ca-f932-4a79-8eef-9e1dc0619e75/
 * 3e88b5e7-739f-48d0-a04f-fda8dae28971 Mutual Funds basics -
 * https://datahubapi.tase.co.il/spec/a957e6f2-855c-4e5c-a199-033c0e1edf1e/
 * 0b795b36-2f7a-458b-81b9-7b5432daef0c
 */
public class TLVQuoteFeed implements QuoteFeed
{

    public static final String ID = "TASE"; //$NON-NLS-1$
    private TLVSecurity TLVSecurities;
    private TLVFund TLVFunds;
    private Boolean ismapped;
    private List<IndiceListing> mappedEntities;
    // DateTimeFormatter formatter;

    
    
    public TLVQuoteFeed()
    {
        this.TLVSecurities = new TLVSecurity();
        this.TLVFunds = new TLVFund();
        this.ismapped = false;
        // this.formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        // //$NON-NLS-1$


        try
        {
            this.mapEntities();
            this.ismapped = true;
        }
        catch (IOException e)
        {
            System.out.print("Could not get TLV Stock Exchange Entities"); //$NON-NLS-1$
            this.ismapped = false;
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
        return Messages.LabelTelAvivFinance;
    }

    @Override
    public String getGroupingCriterion(Security security)
    { //
        return "mayaapi.tase.co.il"; //$NON-NLS-1$
    }

    // returns cached index of all Tel-Aviv Entities (stock, bonds, indexes,
    // companies)
    public List<IndiceListing> getTLVEntities()
    {
        return this.mappedEntities;
    }

    // public List<IndiceListing> getAllSecurities(Language lang) throws
    // Exception
    // {
    // TLVEntities indices = new TLVEntities();
    // return indices.getAllListings(Language.ENGLISH).get();
    //
    // }

    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse)
    {

        Optional<QuoteFeedData> historicalprices = Optional.of(new QuoteFeedData());
        TLVType securityType = this.getSecurityType(security.getWkn());

        if (securityType == TLVType.NONE)
            return new QuoteFeedData();

        if (securityType == TLVType.FUND)
        {

            historicalprices = this.TLVFunds.getHistoricalQuotes(security, false);
            if (historicalprices.isEmpty())
                return new QuoteFeedData();
            return historicalprices.get();
        }
        if (securityType == TLVType.SECURITY)
        {
            historicalprices = this.TLVSecurities.getHistoricalQuotes(security, false);
            if (historicalprices.isEmpty())
                return new QuoteFeedData();
            return historicalprices.get();
        }
        return historicalprices.get();
    }


    @Override
    public Optional<LatestSecurityPrice> getLatestQuote(Security security)
    {
        if ((security.getWkn() == null) || (security.getWkn().length() == 0))
            return Optional.empty();

        TLVType type = getSecurityType(security.getWkn());
        Optional<LatestSecurityPrice> priceOpt = Optional.empty();

        try
        {
            if (type == TLVType.FUND)
            {
                priceOpt = this.TLVFunds.getLatestQuote(security);
            }
            if (type == TLVType.SECURITY)
                priceOpt = this.TLVSecurities.getLatestQuote(security);

            return priceOpt;
        }
        catch (Exception e)
        {
            return priceOpt;
        }
    }

    private void mapEntities() throws IOException
    {
        TLVEntities entities = new TLVEntities();

        Optional<List<IndiceListing>> mappedEntitiesOptional = entities.getAllListings(Language.ENGLISH);

        if (!mappedEntitiesOptional.isEmpty())
        {
            this.mappedEntities = mappedEntitiesOptional.get();
            
            Iterator<IndiceListing> entitiesIterator = this.mappedEntities.iterator();

            while (entitiesIterator.hasNext())
            {
                IndiceListing listing = entitiesIterator.next();

                int type = listing.getType();
                String subtype = listing.getSubType();
                listing.setTLVType(TLVType.NONE);

                if (type == SecurityType.MUTUAL_FUND.getValue() && subtype == null) // $NON-NLS-1$
                {
                    listing.setTLVType(TLVType.FUND);
                }
                if (type == SecurityType.SECURITY.getValue() && subtype != SecuritySubType.WARRENTS.toString())
                {
                    listing.setTLVType(TLVType.SECURITY);
                }
            }
        }
        else
        {
            System.out.println("Could not get TLV Stock Exchange Entities"); //$NON-NLS-1$
        }
    }

    private TLVType getSecurityType(String securityId)
    {
        if (!this.ismapped)
            return TLVType.NONE;

        IndiceListing foundIndice = this.mappedEntities.stream().filter(p -> p.getId().equals(securityId)).findFirst()
                        .orElse(null);
        if (foundIndice != null)
            return foundIndice.getTLVType();

        return TLVType.NONE;
    }


    /****************************************/

    // public Map<String, Object> getDetails(Security security, Language lang)
    // throws Exception
    // {
    // try
    // {
    // // Object TLVClass = getTLVClass(security);
    // TLVType securityType = this.getSecurityType(security.getWkn());
    // if (securityType != TLVType.NONE)
    // {
    // if (securityType == TLVType.SECURITY)
    // {
    // SecurityListing securityDetails = this.TLVSecurities.getDetails(security,
    // lang);
    // return TLVSecurity.ObjectToMap(securityDetails);
    // }
    // if (securityType == TLVType.FUND)
    // {
    // FundListing fundDetails = this.TLVFunds.getDetails(security, lang);
    // return TLVFunds.ObjectToMap(fundDetails);
    // }
    // return Collections.emptyMap();
    // }
    // else
    // {
    // return Collections.emptyMap();
    // }
    //
    // }
    // catch (IOException e)
    // {
    // return Collections.emptyMap();
    // }
    // }

    // public Map<String, String> getNames(Security security)
    // {
    // try
    // {
    // // Object TLVClass = getTLVClass(security);
    // TLVType securityType = this.getSecurityType(security.getWkn());
    // if (securityType != TLVType.NONE)
    // {
    // if (securityType == TLVType.SECURITY)
    // {
    // // SecurityListing securityDetails =
    // // this.TLVSecurities.getDetails(security, lang);
    // // return this.TLVSecurities.ObjectToMap(securityDetails);
    // SecurityListing englishDetails = this.TLVSecurities.getDetails(security,
    // Language.ENGLISH);
    // SecurityListing hebrewDetails = this.TLVSecurities.getDetails(security,
    // Language.HEBREW);
    // return this.TLVSecurities.getNames(englishDetails, hebrewDetails);
    // }
    // if (securityType == TLVType.FUND)
    // {
    // // FundListing fundDetails =
    // // this.TLVFunds.getDetails(security, lang);
    // // return this.TLVFunds.ObjectToMap(fundDetails);
    // FundListing englishDetails = this.TLVFunds.getDetails(security,
    // Language.ENGLISH);
    // FundListing hebrewDetails = this.TLVFunds.getDetails(security,
    // Language.HEBREW);
    // return this.TLVFunds.getNames(englishDetails, hebrewDetails);
    // }
    // return Collections.emptyMap();
    // }
    // else
    // {
    // return Collections.emptyMap();
    // }
    // }
    // catch (Exception e)
    // {
    // return Collections.emptyMap();
    // }
    //
    // }




    // private Optional<LatestSecurityPrice> NotInUse(Security security)
    // {
    // String json;
    // LatestSecurityPrice price = new LatestSecurityPrice();
    // // LocalDate start = calculateStartDay();
    //
    // TLVType type = getSecurityType(security.getWkn());
    // // System.out.println("type " + type); //$NON-NLS-1$
    //
    // try
    // {
    // json = this.rpcLatestQuoteFund(security);
    //
    // Optional<String> time = extract(json, 0, "\"RelevantDate\":", ",");
    // //$NON-NLS-1$ //$NON-NLS-2$
    // if (time.isPresent())
    // {
    // LocalDate rt = TLVHelper.asDateTime(time.get());
    // price.setDate(rt);
    // }
    //
    //
    // // TradeDate
    // Optional<String> tradeDate = extract(json, 0, "\"TradeDate\":", ",");
    // //$NON-NLS-1$ //$NON-NLS-2$
    // if (tradeDate.isPresent())
    // {
    // LocalDate dt = TLVHelper.asDate(tradeDate.get());
    // price.setDate(dt);
    // }
    //
    //
    // // QuoteCurrency
    //
    // // Optional<String> quoteCurrency = Optional.of("ILA");
    // // //$NON-NLS-1$
    // Optional<String> quoteCurrency = getQuoteCurrency(security);
    // // System.out.println("Currency Quote: " +
    // // quoteCurrency);//$NON-NLS-1$
    //
    //
    // // Unite Price
    // Optional<String> value = extract(json, 0, "\"UnitValuePrice\":", ",");
    // //$NON-NLS-1$ //$NON-NLS-2$
    //
    // if (value.isPresent())
    // {
    // String p = value.get().trim();
    // long asPrice = asPrice(p);
    // long convertedPrice = convertILS(asPrice, quoteCurrency.orElse(null),
    // security.getCurrencyCode());
    // // System.out.println("Converted Price: " + quoteCurrency);
    // // //$NON-NLS-1$
    // price.setValue(convertedPrice);
    // }
    //
    // // Last Rate
    // Optional<String> lastRate = extract(json, 0, "\"LastRate\":", ",");
    // //$NON-NLS-1$ //$NON-NLS-2$
    //
    //
    // if (lastRate.isPresent())
    // {
    // String p = lastRate.get().trim();
    // long asPrice = asPrice(p);
    // price.setValue(convertILS(asPrice, quoteCurrency.orElse(null),
    // security.getCurrencyCode()));
    // }
    //
    //
    //
    // // MarketVolume - OverallTurnOverUnits
    // Optional<String> marketvolume = extract(json, 0,
    // "\"OverallTurnOverUnits\":", ","); //$NON-NLS-1$ //$NON-NLS-2$
    //
    // if (marketvolume.isPresent())
    // {
    // price.setVolume(Long.parseLong((String) marketvolume.get().trim()));
    // }
    // else
    // {
    // price.setVolume(LatestSecurityPrice.NOT_AVAILABLE);
    //
    // }
    //
    //
    // Optional<String> highrate = extract(json, 0, "\"HighRate\":", ",");
    // //$NON-NLS-1$ //$NON-NLS-2$
    // if (highrate.isPresent())
    // {
    // long asPrice = asPrice(highrate.get().trim());
    // price.setHigh(convertILS(asPrice, quoteCurrency.orElse(null),
    // security.getCurrencyCode()));
    // }
    // else
    // {
    // price.setHigh(LatestSecurityPrice.NOT_AVAILABLE);
    // }
    //
    //
    // Optional<String> lowrate = extract(json, 0, "\"LowRate\":", ",");
    // //$NON-NLS-1$ //$NON-NLS-2$
    // if (lowrate.isPresent())
    // {
    // String p = lowrate.get().trim();
    // long asPrice = asPrice(p);
    // price.setLow(convertILS(asPrice, quoteCurrency.orElse(null),
    // security.getCurrencyCode()));
    // }
    // else
    // {
    // price.setLow(LatestSecurityPrice.NOT_AVAILABLE);
    // }
    //
    //
    //
    // if (price.getDate() == null || price.getValue() <= 0)
    // {
    // return Optional.empty();
    // }
    // else
    // {
    // return Optional.of(price);
    // }
    // }
    // catch (Exception e)
    // {
    // System.out.println(e.getMessage());
    // // throw new QuoteFeedException();
    //
    // return Optional.empty();
    // }
    // }



    // private long DoubletoLong(JSONObject item, String str, Optional<String>
    // quoteCurrency, String securityCurrency)
    // {
    // Double doublevalue = null;
    // Long longValue = null;
    //
    // try
    // {
    // doublevalue = (Double) item.get(str);
    // }
    // catch (IndexOutOfBoundsException e)
    // {
    // //
    // }
    // catch (ClassCastException e)
    // {
    // longValue = (Long) item.get(str);
    // if (longValue != null && longValue > 0l)
    // {
    // long convertedprice =
    // convertILS(Values.Quote.factorize(roundQuoteValue(longValue)),
    // quoteCurrency.orElse(null), securityCurrency);
    // longValue = convertedprice;
    // }
    // }
    //
    // if (doublevalue != null && doublevalue.doubleValue() > 0)
    // {
    // long convertedprice =
    // convertILS(Values.Quote.factorize(roundQuoteValue(doublevalue)),
    // quoteCurrency.orElse(null), securityCurrency);
    // longValue = convertedprice;
    // }
    // else
    // {
    // longValue = LatestSecurityPrice.NOT_AVAILABLE;
    // }
    // return longValue;
    //
    // }


    // private long convertILSToILA(Double value)
    // {
    // final ThreadLocal<DecimalFormat> FMT_PRICE = new
    // ThreadLocal<DecimalFormat>()
    // {
    // @Override
    // protected DecimalFormat initialValue()
    // {
    // DecimalFormat fmt = new DecimalFormat("0.###", new
    // DecimalFormatSymbols(Locale.US)); //$NON-NLS-1$
    // fmt.setParseBigDecimal(true);
    // return fmt;
    // }
    // };
    //
    // String strvalue = Double.toString(value);
    // BigDecimal v;
    // try
    // {
    // v = (BigDecimal) FMT_PRICE.get().parse(strvalue);
    // return
    // v.multiply(BigDecimal.ONE).multiply(BigDecimal.valueOf(100)).longValue();
    // }
    // catch (ParseException e)
    // {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // return BigDecimal.ZERO.longValue();
    // }
    //
    // }

    /**
     * Calculate the first date to request historical quotes for.
     */
    /* package */final LocalDate caculateStart(Security security)
    {
        if (!security.getPrices().isEmpty())
        {
            SecurityPrice lastHistoricalQuote = security.getPrices().get(security.getPrices().size() - 1);
            return lastHistoricalQuote.getDate();
        }
        else
        {
            return LocalDate.of(1900, 1, 1);
        }
    }
    //
    // private Optional<SecurityHistory> getPriceHistoryChunkSecurity(Security
    // security, LocalDate fromDate, LocalDate toDate,
    // int page,
    // Language lang)
    // {
    // try
    // {
    // Optional<SecurityHistory> securityHistory =
    // this.TLVSecurities.getPriceHistoryChunkSec(security, fromDate,
    // toDate, page, lang);
    // if (securityHistory.isPresent())
    // return securityHistory;
    //
    // return Optional.empty();
    // }
    // catch (Exception e)
    // {
    // return Optional.empty();
    // }
    //
    // }
    //
    // private Optional<FundHistory> getPriceHistoryChunkFund(Security security,
    // LocalDate fromDate, LocalDate toDate,
    // int page, Language lang)
    // {
    // try
    // {
    // Optional<FundHistory> fundHistory =
    // this.TLVFunds.getPriceHistoryChunk(security, fromDate, toDate, page,
    // lang);
    // if (fundHistory.isPresent())
    // return fundHistory;
    //
    // return Optional.empty();
    // }
    // catch (Exception e)
    // {
    // return Optional.empty();
    // }
    // }
    // private String getPriceHistoryChunk2(Security security, LocalDate
    // fromDate, LocalDate toDate,
    // int page,
    // Language lang) throws Exception
    // {
    // TLVType securityType = this.getSecurityType(security.getWkn());
    // JSONObject emptyjson = new JSONObject();
    // if (securityType != TLVType.NONE)
    // {
    // if (securityType == TLVType.SECURITY)
    // {
    // Optional<SecurityHistory> securityHistory =
    // this.TLVSecurities.getPriceHistoryChunkSec(security, fromDate,
    // toDate, page, lang);
    // if (securityHistory.isPresent())
    // // return TLVHelper.ObjectToMap(securityHistory.get());
    // return this.HistoryToJson(securityHistory.get());
    // }
    // if (securityType == TLVType.FUND)
    // {
    // Optional<FundHistory> fundHistory =
    // this.TLVFunds.getPriceHistoryChunk(security, fromDate, toDate,
    // page, lang);
    // if (fundHistory.isPresent())
    // // return TLVHelper.ObjectToMap(fundHistory.get());
    // return this.HistoryToJson(fundHistory.get());
    // }
    // // return Collections.emptyMap();
    //
    // return emptyjson.toString();
    // }
    // else
    // {
    // // return Collections.emptyMap();
    // return emptyjson.toString();
    // }
    //
    // }
    
    // TODO do we need to make this Optional?
    public Map<String, Object> getPriceHistory(Security security, LocalDate fromDate, LocalDate toDate, int page,
                    Language lang) throws Exception
    {
        if (toDate == null)
        {
            toDate = LocalDate.now();
        }
        if (fromDate == null)
        {
            fromDate = toDate.minusDays(1);
        }

        if ((security.getWkn() == null) || (security.getWkn().length() == 0))
            return Collections.emptyMap();

        TLVType securityType = this.getSecurityType(security.getWkn());
        if (securityType != TLVType.NONE)
        {
            if (securityType == TLVType.SECURITY)
            {
                Optional<SecurityHistory> securityHistory = this.TLVSecurities.getPriceHistory(security, fromDate,
                                toDate, page,
                                lang);
                if (securityHistory.isPresent())
                    return TLVHelper.ObjectToMap(securityHistory);
            }
            if (securityType == TLVType.FUND)
            {
                Optional<FundHistory> fundHistory = this.TLVFunds.getPriceHistory(security, fromDate, toDate,
                                page, lang);
                if (fundHistory.isPresent())
                    return TLVHelper.ObjectToMap(fundHistory);
            }
            return Collections.emptyMap();
        }
        else
        {
            return Collections.emptyMap();
        }

    }

    /* package */
    
    QuoteFeedData extractQuotes(String responseBody)
    {
        return extractQuotes(responseBody, ""); //$NON-NLS-1$
    }



//    public Optional<String> extract(String body, int startIndex, String startToken, String endToken)
//    {
//        int begin = body.indexOf(startToken, startIndex);
//
//        if (begin < 0)
//            return Optional.empty();
//
//        int end = body.indexOf(endToken, begin + startToken.length());
//        if (end < 0)
//            return Optional.empty();
//
//        return Optional.of(body.substring(begin + startToken.length(), end));
//    }

// private String HistoryToJson(Object listing)
// {
// Gson gson = GSONUtil.createGson();
// return gson.toJson(listing);
// }

    private QuoteFeedData extractQuotes(String responseBody, String securityCurrency)
    {

        QuoteFeedData data = new QuoteFeedData();
        data.addResponse("n/a", responseBody); //$NON-NLS-1$
        return data;
    }



    // private static long convertILS(long price, String quoteCurrency, String
    // securityCurrency)
    // {
    // if (quoteCurrency != null)
    // {
    // if ("ILA".equals(quoteCurrency) && "ILS".equals(securityCurrency))
    // //$NON-NLS-1$ //$NON-NLS-2$
    // return price / 100;
    // if ("ILS".equals(quoteCurrency) && "ILA".equals(securityCurrency))
    // //$NON-NLS-1$ //$NON-NLS-2$
    // return price * 100;
    // }
    // return price;
    // }




    // List<SecurityListing> getAllSecurities(Language lang) throws Exception
    // {
    //
    // // https://api.tase.co.il/api/content/searchentities?lang=1
    // String response = new WebAccess("api.tase.co.il",
    // "/api/content/searchentities") //$NON-NLS-1$ //$NON-NLS-2$
    // .addUserAgent("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; FSL
    // 7.0.6.01001") //$NON-NLS-1$
    // .addParameter("lang", "1") //$NON-NLS-1$ //$NON-NLS-2$
    // .addHeader("Accept", "*/*") //$NON-NLS-1$ //$NON-NLS-2$
    // .addHeader("referer", "https://www.tase.co.il/") //$NON-NLS-1$
    // //$NON-NLS-2$
    // .addHeader("Cache-Control", "no-cache") //$NON-NLS-1$ //$NON-NLS-2$
    // .addHeader("Content-Type", "application/json") //$NON-NLS-1$
    // //$NON-NLS-2$
    // .get();
    // System.out.println(response);
    // return ResponsetoSecurityList(response);
    // }

    // private List<SecurityListing> ResponsetoSecurityList(String response)
    // {
    // Gson gson = new Gson();
    // Type SecurityListingType = new TypeToken<List<SecurityListing>>()
    // {
    // }.getType();
    // List<SecurityListing> list = gson.fromJson(response,
    // SecurityListingType);
    //
    //
    // return list;
    // }


    // private Optional<Object> getTLVClass(Security security)
    // {
    // if (this.mapped)
    // {
    // SecurityType type = getSecurityType(security.getWkn());
    // if ((type == SecurityType.MUTUAL_FUND) || (type == SecurityType.INDEX))
    // return Optional.of(this.TLVFunds);
    // if (type == SecurityType.SHARES)
    // return Optional.of(this.TLVSecurities);
    // }
    // return Optional.empty();

    @VisibleForTesting
    public String rpcLatestQuoteFund(Security security)
    {
        // LatestSecurityPrice price = new LatestSecurityPrice();
        String json = ""; //$NON-NLS-1$
        TLVType type = getSecurityType(security.getWkn());
        // System.out.println("Security is: " + security); //$NON-NLS-1$
        try
        {
            if (type == TLVType.FUND)
            {
                json = this.TLVFunds.rpcLatestQuoteFund(security);
            }
            if (type == TLVType.SECURITY)
            {
                // json = this.TLVSecurities.getLatestQuote(security);
            }
            json = "{}"; //$NON-NLS-1$

        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "{}"; //$NON-NLS-1$
        }
        return json;

    }



    // private Optional<String> quoteCurrency = Optional.of("ILA");
    // //$NON-NLS-1$
    public Optional<String> getQuoteCurrency(Security security)
    {

        TLVType type = getSecurityType(security.getWkn());
        if (type == TLVType.FUND)
        { //
            return Optional.of("ILS"); //$NON-NLS-1$
        }
        if (type == TLVType.SECURITY)
        { // 
            return Optional.of("ILA"); //$NON-NLS-1$
        }
        return Optional.of("ILA");//$NON-NLS-1$
    }

    protected double roundQuoteValue(double value)
    {
        return Math.round(value * 10000) / 10000d;
    }

    // public Map<String, Object> ObjectToMap(Object listing)
    // {
    // Gson gson = GSONUtil.createGson();
    // String json = gson.toJson(listing);
    // Map<String, Object> map = gson.fromJson(json, new TypeToken<Map<String,
    // Object>>()
    // {
    // }.getType());
    // return map;
    //
    // }
}

