package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.impl.TLVMarket.TLVEntities;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.IndiceListing;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.SecurityListing;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.Language;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.SecuritySubType;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.SecurityType;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.TLVType;

public class TLVSearchProvider implements SecuritySearchProvider
{
    List<SecurityListing> allsecurities = new ArrayList<>();
    // private Map<String, Set<String>> mappedSecurities;
    private List<IndiceListing> mappedEntities;
    private Boolean mapped = false;

    public TLVSearchProvider()
    {
        try
        {
            this.mapEntities();
            this.mapped = true;
        }
        catch (IOException e)
        {
            System.out.print("Could not resolved TLV securities from TLV API"); //$NON-NLS-1$
            PortfolioLog.abbreviated(e);
            this.mapped = false;
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
                String id = listing.getId();

                int type = listing.getType();
                String subtype = listing.getSubType();
                TLVType tlvType = TLVType.NONE;

                if (type == SecurityType.MUTUAL_FUND.getValue() && subtype.equals("")) //$NON-NLS-1$
                {
                    listing.setTLVType(TLVType.FUND);
                }
                if (type == SecurityType.SECURITY.getValue() && subtype != SecuritySubType.WARRENTS.toString())
                {
                    listing.setTLVType(TLVType.SECURITY);
                }
            }
        }

    }

    @Override
    public String getName()
    {
        return Messages.LabelYahooFinance;
    }



    /*
     * Return a list of ResultItems ResultItems is an interface defined in
     * SecurityProvider
     */
    @Override
    public List<ResultItem> search(String query) throws IOException
    {
        List<ResultItem> answer = new ArrayList<>();

        // Take the mappedSecurities and search on the query.
        // Return the matching Result Entries.
        // addSearchPage(answer, query);
        // addSymbolSearchResults(answer, query);

        if (answer.size() >= 10)
        {
            YahooSymbolSearch.Result item = new YahooSymbolSearch.Result(Messages.MsgMoreResultsAvailable);
            answer.add(item);
        }

        return answer;
    }

    // private void addSearchPage(List<ResultItem> answer, String query) throws
    // IOException
    // {
    // @SuppressWarnings("nls")
    // String html = new WebAccess("query2.finance.yahoo.com",
    // "/v1/finance/lookup") //
    // .addUserAgent(OnlineHelper.getYahooFinanceUserAgent()) //
    // .addParameter("formatted", "true") //
    // .addParameter("lang", "de-DE").addParameter("region", "DE") //
    // .addParameter("query", query) //
    // .addParameter("type", "all") //
    // .addParameter("count", "25") //
    // .addParameter("start", "0") //
    // .addParameter("corsDomain", "de.finance.yahoo.com") //
    // .get();
    //
    // extractFrom(answer, html);
    // }

    // private void mapSecurities() throws Exception
    // {
    // TLVEntities indices = new TLVEntities();
    // List<IndiceListing> allSecurityList =
    // indices.getAllListings(Language.ENGLISH);
    //
    // }


}
