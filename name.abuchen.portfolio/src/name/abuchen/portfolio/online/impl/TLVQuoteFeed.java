package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.impl.TLVMarket.TLVEntities;
import name.abuchen.portfolio.online.impl.TLVMarket.TLVFund;
import name.abuchen.portfolio.online.impl.TLVMarket.TLVSecurity;
import name.abuchen.portfolio.online.impl.TLVMarket.jsondata.IndiceListing;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.Language;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.SecuritySubType;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.SecurityType;
import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.TLVType;

/*
 * @see Developer portal: https://datahubapi.tase.co.il/ List of indices -
 * https://datahubapi.tase.co.il/spec/8149ecc8-ca0f-4391-92bf-79f69587919d/
 * 4301d3e3-3dae-4844-bae4-6cf4d103bba8 Securities basic -
 * https://datahubapi.tase.co.il/spec/089150ca-f932-4a79-8eef-9e1dc0619e75/
 * 3e88b5e7-739f-48d0-a04f-fda8dae28971 Mutual Funds basics -
 * https://datahubapi.tase.co.il/spec/a957e6f2-855c-4e5c-a199-033c0e1edf1e/
 * 0b795b36-2f7a-458b-81b9-7b5432daef0c
 */
public class TLVQuoteFeed implements QuoteFeed
{

    public static final String ID = "TLV"; //$NON-NLS-1$
    private TLVSecurity TLVSecurities;
    private TLVFund TLVFunds;
    private Boolean ismapped;
    private List<IndiceListing> mappedEntities;

    
    
    public TLVQuoteFeed()
    {
        this.TLVSecurities = new TLVSecurity();
        this.TLVFunds = new TLVFund();
        this.ismapped = false;


        try
        {
            this.mapEntities();
            this.ismapped = true;
        }
        catch (IOException e)
        {
            PortfolioLog.error("Could not get TLV Stock Exchange Entities"); //$NON-NLS-1$
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
            PortfolioLog.error(e);
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
            PortfolioLog.error("Could not get TLV Stock Exchange Entities"); //$NON-NLS-1$
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




    /**
     * Calculate the first date to request historical quotes for.
     */
    public LocalDate caculateStart(Security security)
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




}

