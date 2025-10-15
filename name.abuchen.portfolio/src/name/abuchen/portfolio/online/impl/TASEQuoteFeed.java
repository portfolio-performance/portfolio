package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
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
import name.abuchen.portfolio.online.impl.TASE.TASEEntities;
import name.abuchen.portfolio.online.impl.TASE.TASEFund;
import name.abuchen.portfolio.online.impl.TASE.TASESecurity;
import name.abuchen.portfolio.online.impl.TASE.jsondata.IndiceListing;
import name.abuchen.portfolio.online.impl.TASE.utils.TASEHelper.Language;
import name.abuchen.portfolio.online.impl.TASE.utils.TASEHelper.TaseSecuritySubType;
import name.abuchen.portfolio.online.impl.TASE.utils.TASEHelper.TaseSecurityType;
import name.abuchen.portfolio.online.impl.TASE.utils.TASEHelper.TaseType;

/*
 * @see Developer portal: https://datahubapi.tase.co.il/ List of indices -
 * https://datahubapi.tase.co.il/spec/8149ecc8-ca0f-4391-92bf-79f69587919d/
 * 4301d3e3-3dae-4844-bae4-6cf4d103bba8 Securities basic -
 * https://datahubapi.tase.co.il/spec/089150ca-f932-4a79-8eef-9e1dc0619e75/
 * 3e88b5e7-739f-48d0-a04f-fda8dae28971 Mutual Funds basics -
 * https://datahubapi.tase.co.il/spec/a957e6f2-855c-4e5c-a199-033c0e1edf1e/
 * 0b795b36-2f7a-458b-81b9-7b5432daef0c
 */
public class TASEQuoteFeed implements QuoteFeed
{

    public static final String ID = "TASE"; // Tel Aviv Stock //$NON-NLS-1$
                                            // Exchange
    private TASESecurity TASESecurities;
    private TASEFund TASEFunds;
    private Boolean ismapped;
    private List<IndiceListing> mappedEntities;

    
    
    public TASEQuoteFeed()
    {
        this.TASESecurities = new TASESecurity();
        this.TASEFunds = new TASEFund();
        this.ismapped = false;


        try
        {
            this.mapEntities();
            this.ismapped = true;
        }
        catch (IOException e)
        {
            PortfolioLog.error("Could not get Tel Aviv Stock Exchange Entities"); //$NON-NLS-1$
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
    public List<IndiceListing> getTaseEntities()
    {
        if (this.mappedEntities == null)
            return Collections.emptyList();
        else
            return this.mappedEntities;
    }


    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse)
    {

        Optional<QuoteFeedData> historicalprices = Optional.of(new QuoteFeedData());

        TaseType securityType = this.getSecurityType(security.getWkn());

        if (securityType == TaseType.NONE)
            return new QuoteFeedData();

        if (securityType == TaseType.FUND)
        {

            historicalprices = this.TASEFunds.getHistoricalQuotes(security, false);
            if (historicalprices.isEmpty())
                return new QuoteFeedData();
            return historicalprices.get();
        }
        if (securityType == TaseType.SECURITY)
        {
            historicalprices = this.TASESecurities.getHistoricalQuotes(security, false);
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

        TaseType type = getSecurityType(security.getWkn());
        Optional<LatestSecurityPrice> priceOpt = Optional.empty();

        try
        {
            if (type == TaseType.FUND)
            {
                priceOpt = this.TASEFunds.getLatestQuote(security);
            }
            if (type == TaseType.SECURITY)
                priceOpt = this.TASESecurities.getLatestQuote(security);

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
        TASEEntities entities = new TASEEntities();

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
                listing.setTaseType(TaseType.NONE);

                if (type == TaseSecurityType.MUTUAL_FUND.getValue() && subtype == null) // $NON-NLS-1$
                {
                    listing.setTaseType(TaseType.FUND);
                }
                if (type == TaseSecurityType.SECURITY.getValue() && subtype != TaseSecuritySubType.WARRENTS.toString())
                {
                    listing.setTaseType(TaseType.SECURITY);
                }
            }
        }
        else
        {
            PortfolioLog.error("Could not get TLV Stock Exchange Entities"); //$NON-NLS-1$
        }
    }

    private TaseType getSecurityType(String securityId)
    {
        if (!this.ismapped || this.mappedEntities == null)
            return TaseType.NONE;

        IndiceListing foundIndice = this.mappedEntities.stream().filter(p -> p.getId().equals(securityId)).findFirst()
                        .orElse(null);
        if (foundIndice != null)
            return foundIndice.getTaseType();

        return TaseType.NONE;
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

        TaseType type = getSecurityType(security.getWkn());
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




}

