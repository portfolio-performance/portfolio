package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
 * Implementation of Search Provider for TASE
 * 
 * @api https://api.tase.co.il/api/content/searchentities?lang=1
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



        public static Result from(IndiceListing listing)
        {
            String id = (String) listing.getId();
            String name = (String) listing.getName();
            String smb = (String) listing.getSmb();
            String isin = (String) listing.getISIN();
            String type = String.valueOf(listing.getSubTypeDesc());
            
            var currencyCode = (String) "ILA"; //$NON-NLS-1$
            var exchange = "TASE"; //$NON-NLS-1$

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

            String symbol = getSymbol();
            if (symbol == null)
                return null;

            int p = symbol.indexOf('.');
            if (p >= 0)
            {
                String d = symbol.substring(p, symbol.length());

                return d.equals("TLV") ? symbol.substring(0, p) : symbol; //$NON-NLS-1$
            }
            return symbol;
        }

        @Override
        public Security create(Client client)
        {
            Security security = new Security(name, currencyCode);
            security.setTickerSymbol(symbol);
            security.setWkn(Id);
            security.setFeed(TASEQuoteFeed.ID);
            security.setName(name);
            return security;
        }

        @SuppressWarnings("nls")
        @Override
        public String toString()
        {
            return "Result [Id=" + Id + ", name=" + name + ", symbol=" + symbol + ", isin=" + isin + ", exchange="
                            + exchange + ", type=" + type + ", currencyCode=" + currencyCode + "]";
        }
    }

    private static final String NAME = "Tel Aviv Exchange"; //$NON-NLS-1$
    private List<IndiceListing> tlvEntities;
    private Boolean ismapped;

    


    public TASESearchProvider()
    {
        super();
        /**
         * TASE API has a search by ISIN only. To support other searches, cache
         * all tlvEntities upfront so we can search on them
         */
        // this.tlvEntities = getFeedEntities();
        this.ismapped = false;

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

        if (!this.ismapped)
        {
            this.tlvEntities = this.getFeedEntities();
        }
        // Exit if the cached list of entities is empty
        if (this.tlvEntities == null || this.tlvEntities.isEmpty())
        {
            PortfolioLog.info("TLV Listing returned empty"); //$NON-NLS-1$
            return answer;
        }

        String _query = query.trim().toUpperCase();
        Stream<IndiceListing> filteredListingStreamByQuery = this.tlvEntities.stream()
                        .filter(a -> a.getId().equals(_query) || //
                                        a.getSmb().equals(_query) || //
                                        a.getId().equals(_query) || //
                                        a.getISIN().equals(_query));

        List<IndiceListing> filteredListingByQuery = filteredListingStreamByQuery.collect(Collectors.toList());


        Iterator<IndiceListing> filteredListingIterator = filteredListingByQuery.iterator();


        while (filteredListingIterator.hasNext())
        {
            IndiceListing listing = filteredListingIterator.next();



            // we found an entry either by symbol, id or isin
            Optional<String> currencyCode = this.getCurrencyType(listing);
            if (currencyCode.isPresent())
            {
                Result a = Result.from(listing);
                a.setCurrentCode(currencyCode.get());
                answer.add(Result.from(listing));
            }
                
        }

        return answer;
    }

    /**
     * Funds and Securities are stored differently in TASE API. Return
     * Currencytype based on Security type
     * 
     * @param security
     * @return String
     */
    private Optional<String> getCurrencyType(IndiceListing listing)
    {
        TaseType type = listing.getTaseType();

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


    /*
     * Gets all Entities from the TASE API, then filters the Mutual Funds and
     * Securities. Not interested in disclosures, indices and companies
     */
    private List<IndiceListing> getFeedEntities()
    {
        TASEQuoteFeed feed = new TASEQuoteFeed();
        List<IndiceListing> unfilteredEntities = feed.getTaseEntities();

        if (unfilteredEntities != null && unfilteredEntities.size() > 0)
        {
            Stream<IndiceListing> tlvFilteredEntities = unfilteredEntities.stream()
                            .filter(a -> a.getTaseType().equals(TaseType.FUND) || //
                                            a.getTaseType().equals(TaseType.SECURITY));
            List<IndiceListing> filtered = tlvFilteredEntities.collect(Collectors.toList());
            return filtered;
        }
        return Collections.emptyList();
    }

}
