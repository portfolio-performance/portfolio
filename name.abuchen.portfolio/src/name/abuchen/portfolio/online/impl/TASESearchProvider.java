package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.impl.TASE.jsondata.IndiceListing;
import name.abuchen.portfolio.online.impl.TASE.utils.TASEHelper.TaseType;

/**
     * @see https://api.tase.co.il/api/content/searchentities?lang=1
     * 
     *  @formatter:off
     *  @array 
     *  [
     *      {
            "Id": "1397",
            "Name": "ABRA",
            "Smb": null,
            "ISIN": null,
            "Type": 5,
            "SubType": "0",
            "SubTypeDesc": "Company",
            "SubId": "01101666",
            "ETFType": null
        },
        {
            "Id": "273011",
            "Name": "NICE",
            "Smb": "NICE",
            "ISIN": "IL0002730112",
            "Type": 1,
            "SubType": "1",
            "SubTypeDesc": "Shares",
            "SubId": "000273",
            "ETFType": null
        },
        
        ]
        
    }
     *  @formatter:on
     */
public class TASESearchProvider implements SecuritySearchProvider
{

    static class Result implements ResultItem
    {
        private String Id;
        private String name;
        private String symbol;
        private String isin;
        private String exchange;
        private String type;
        private String currencyCode;

        @SuppressWarnings("nls")
        // public static Result from(JSONObject json)
        // {
        // PortfolioLog.info(json.toString());
        // // Extract values from the JSON object
        // var isin = (String) json.get("ISIN");
        // var tickerSymbol = (String) json.get("Smb");
        // var exchange = "TLV";
        // var name = (String) json.get("Name");
        // var type = String.valueOf(json.get("Type"));
        //
        // var currencyCode = (String) "ILA";
        // var id = (String) json.get("Id");
        //
        // var symbol = new StringBuilder(tickerSymbol);
        // symbol.append(".");
        // symbol.append("TLV");
        //
        // return new Result(isin, symbol.toString(), id, currencyCode, name,
        // type, exchange);
        //
        // }

        public static Result from(IndiceListing listing)
        {
            String id = (String) listing.getId();
            String name = (String) listing.getName();
            String smb = (String) listing.getSmb();
            String isin = (String) listing.getISIN();
            String type = String.valueOf(listing.getSubTypeDesc());
            
            // TODO - fix
            var currencyCode = (String) "ILA";
            var exchange = "TLV";

            return new Result(isin, smb, id, name, exchange, type, currencyCode);
        }

        public Result(String isin, String symbol, String id, String name, String exchange, String type,
                        String currencyCode)
        {
            this.isin = isin;
            this.symbol = symbol;
            this.name = name;
            this.exchange = exchange;
            this.type = type;
            this.currencyCode = currencyCode;
            this.Id = id;
        }

        @Override
        public String getSymbol()
        {
            return symbol;
        }

        @Override
        public String getName()
        {
            return name;
        }

        @Override
        public String getType()
        {
            return type;
        }

        @Override
        public String getExchange()
        {
            return exchange;
        }

        @Override
        public String getIsin()
        {
            return isin;
        }

        @Override
        public String getWkn()
        {
            return Id;
        }

        @Override
        public String getCurrencyCode()
        {
            return currencyCode;
        }

        public void setCurrentCode(String code)
        {
            currencyCode = code;
        }

        @Override
        public String getSource()
        {
            return NAME;
        }

        @Override
        public String getFeedId()
        {
            return TASEQuoteFeed.ID;
        }

        @Override
        public boolean hasPrices()
        {
            return true;
        }

        @Override
        public String getSymbolWithoutStockMarket()
        {
            PortfolioLog.info("getSymbolwithoutStockmarket for TLV");

            String symbol = getSymbol();
            if (symbol == null)
                return null;

            int p = symbol.indexOf('.');
            // return (p >= 0) ? symbol.substring(0, p) : symbol;
            if (p >= 0)
            {
                String d = symbol.substring(p, symbol.length());
                PortfolioLog.error("getSymbol " + symbol);

                return d.equals("TLV") ? symbol.substring(0, p) : symbol;
                // $NON-NLS-1$
            }
            PortfolioLog.error("getSymbol " + symbol);
            return symbol;
        }

        @Override
        public Security create(Client client)
        {
            var security = new Security(name, currencyCode);
            security.setTickerSymbol(symbol);
            security.setWkn(Id);
            security.setFeed(TASEQuoteFeed.ID);
            return security;
        }

        @Override
        public String toString()
        {
            return "Result [Id=" + Id + ", name=" + name + ", symbol=" + symbol + ", isin=" + isin + ", exchange="
                            + exchange + ", type=" + type + ", currencyCode=" + currencyCode + "]";
        }
    }

    private static final String NAME = "Tel Aviv Exchange"; //$NON-NLS-1$
    private static final String URL = "api.tase.co.il"; //$NON-NLS-1$
    private static final String PATH = "/api/content/searchentities"; //$NON-NLS-1$
    private List<IndiceListing> tlvEntities;

    


    public TASESearchProvider()
    {
        super();
        // Since TASE API only has a search by ISIN, cache all the known indices
        // upfront
        populateEntitiesList();

    }


    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public List<ResultItem> search(String query) throws IOException
    {
        List<ResultItem> answer = new ArrayList<>();
        PortfolioLog.info("search " + query + " in TLV");
        // addStocksearchPage(answer, query.trim());

        if (this.tlvEntities == null || this.tlvEntities.isEmpty())
        {
            PortfolioLog.info("TLV Listing is empty");
            return answer;
        }

        Stream<IndiceListing> filteredListingStreamByQuery = this.tlvEntities.stream()
                        .filter(a -> a.getId().equals(query.trim()) || 
                                        a.getSmb().equals(query.trim()) || a.getId().equals(query.trim())
        );
        List<IndiceListing> filteredListingByQuery = filteredListingStreamByQuery.collect(Collectors.toList());

        PortfolioLog.info(filteredListingByQuery.size() + " items found");

        Iterator<IndiceListing> filteredListingIterator = filteredListingByQuery.iterator();

        /*
         * @formatter:off
         * @api https://api.tase.co.il/api/content/searchentities?lang=1
         * @array
         * [
         *  {
            "Id": "777037",
            "Name": "SHUFERSAL",
            "Smb": "SAE",
            "ISIN": "IL0007770378",
            "Type": 1,
            "SubType": "1",
            "SubTypeDesc": "Shares",
            "SubId": "000777",
            "ETFType": null
        },
         * ]
         * @formatter:on
         */
        while (filteredListingIterator.hasNext())
        {
            IndiceListing listing = filteredListingIterator.next();

            String isin = listing.getISIN() == null ? "N\\A" : listing.getISIN(); //$NON-NLS-1$
            String id = listing.getId() == null ? "N\\A" : listing.getId(); //$NON-NLS-1$
            String sym = listing.getSmb() == null ? "N\\A" : listing.getSmb(); //$NON-NLS-1$
            String name = (String) listing.getName();
            TaseType type = listing.getTaseType();
            // TODO - fix
            // var currencyCode = (String) "ILA";
            var exchange = "TLV";
            
            PortfolioLog.info("TLV iterating " + name + " " + isin + " " + id + " " + sym);

            // if (sym.equals(query) || id.equals(query) || isin.equals(query))
            // {
                // we found an entry either by symbol, id or isin
                Optional<String> currencyCode = this.getCurrencyType(listing);
                if (currencyCode.isPresent()) 
                {
                    Result a = Result.from(listing);
                    a.setCurrentCode(currencyCode.get());
                    PortfolioLog.info(a.toString());
                    answer.add(Result.from(listing));
                }
                
                // }


        }

        return answer;
    }

    private Optional<String> getCurrencyType(IndiceListing listing)
    {
        TaseType type = listing.getTaseType();
        // Get the type
        if (type == TaseType.FUND)
        { //
            return Optional.of("ILS"); //$NON-NLS-1$
        }
        if (type == TaseType.SECURITY)
        { //
            return Optional.of("ILA"); //$NON-NLS-1$
        }
        return Optional.of("ILA");//$NON-NLS-1$
    }

    // private void addStocksearchPage(List<ResultItem> answer, String query)
    // throws IOException
    // {
    // // Filter out MUTUAL FUNDS
    // // Filter out SECURITIES
    //
    // // Filter out STOCK
    // // Add to answer
    //
    // var array = new WebAccess(URL, PATH) //
    // .addParameter("id", query)
    // .addParameter("lang", "1") // //$NON-NLS-1$ //$NON-NLS-2$
    // .addUserAgent("Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; FSL
    // 7.0.6.01001") //$NON-NLS-1$
    // .addHeader("Accept", "*/*") //$NON-NLS-1$ //$NON-NLS-2$
    // .addHeader("referer", "https://www.tase.co.il/") //$NON-NLS-1$
    // //$NON-NLS-2$
    // .addHeader("Cache-Control", "no-cache") //$NON-NLS-1$ //$NON-NLS-2$
    // .addHeader("Content-Type", "application/json") //$NON-NLS-1$
    // //$NON-NLS-2$
    // .get();
    //
    // var result = extract(array);
    //
    // for (var item : result)
    // {
    // var isin = item.getIsin();
    // answer.add(item);
    // }
    // }

    // private List<Result> extract(String array)
    // {
    // var jsonArray = (JSONArray) JSONValue.parse(array);
    //
    // if (jsonArray.isEmpty())
    // return Collections.emptyList();
    //
    // List<Result> answer = new ArrayList<>();
    // for (Object element : jsonArray)
    // {
    // var item = (JSONObject) element;
    // if (item.get("Smb") != null)
    // answer.add(Result.from(item));
    // }
    // return answer;
    // }


    private void populateEntitiesList()
    {
        TASEQuoteFeed feed = new TASEQuoteFeed();
        this.tlvEntities = feed.getTaseEntities();

        PortfolioLog.info("TLV has " + this.tlvEntities.size() + " entries"); //$NON-NLS-1$ //$NON-NLS-2$
        PortfolioLog.info(this.tlvEntities.get(0).toString());
        PortfolioLog.info(this.tlvEntities.get(1).toString());
        PortfolioLog.info(this.tlvEntities.get(2).toString());
        Stream<IndiceListing> tlvFilteredEntities = this.tlvEntities.stream()
                        .filter(a -> a.getTaseType().equals(TaseType.FUND) || //
                                        a.getTaseType().equals(TaseType.SECURITY));
        this.tlvEntities = tlvFilteredEntities.collect(Collectors.toList());
        PortfolioLog.info("TLVI has " + this.tlvEntities.size() + " filtered entries"); //$NON-NLS-1$ //$NON-NLS-2$
        PortfolioLog.info(this.tlvEntities.get(0).toString());
        PortfolioLog.info(this.tlvEntities.get(1).toString());
        PortfolioLog.info(this.tlvEntities.get(2).toString());
    }
}
