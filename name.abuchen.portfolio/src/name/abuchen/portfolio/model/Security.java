package name.abuchen.portfolio.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

public final class Security
{
    public static final class ByName implements Comparator<Security>, Serializable
    {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(Security s1, Security s2)
        {
            if (s1 == null)
                return s2 == null ? 0 : -1;
            return s1.name.compareTo(s2.name);
        }
    }

    public enum AssetClass
    {
        CASH, DEBT, EQUITY, REAL_ESTATE, COMMODITY;

        private static final ResourceBundle RESOURCES = ResourceBundle.getBundle("name.abuchen.portfolio.model.labels"); //$NON-NLS-1$

        public String toString()
        {
            return RESOURCES.getString("asset." + name()); //$NON-NLS-1$
        }
    }

    private String uuid;

    private String name;

    private String isin;
    private String tickerSymbol;
    private String wkn;

    private AssetClass type;
    private String industryClassification;

    private String feed;
    private List<SecurityPrice> prices = new ArrayList<SecurityPrice>();
    private LatestSecurityPrice latest;
    private String finanzenFeedURL;

    private boolean isRetired = false;

    public Security()
    {
        this.uuid = UUID.randomUUID().toString();
    }

    public Security(String name, String isin, String tickerSymbol, AssetClass type, String feed)
    {
        this();
        this.name = name;
        this.isin = isin;
        this.tickerSymbol = tickerSymbol;
        this.type = type;
        this.feed = feed;
    }

    public String getUUID()
    {
        return uuid;
    }

    /* package */void generateUUID()
    {
        // needed to assign UUIDs when loading older versions from XML
        uuid = UUID.randomUUID().toString();
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getIsin()
    {
        return isin;
    }

    public void setIsin(String isin)
    {
        this.isin = isin;
    }

    public String getTickerSymbol()
    {
        return tickerSymbol;
    }

    public void setTickerSymbol(String tickerSymbol)
    {
        this.tickerSymbol = tickerSymbol;
    }

    public String getWkn()
    {
        return wkn;
    }

    public void setWkn(String wkn)
    {
        this.wkn = wkn;
    }

    /**
     * Returns ISIN, Ticker or WKN - whatever is available.
     */
    public String getExternalIdentifier()
    {
        if (isin != null && isin.length() > 0)
            return isin;
        else if (tickerSymbol != null && tickerSymbol.length() > 0)
            return tickerSymbol;
        else if (wkn != null && wkn.length() > 0)
            return wkn;
        else
            return name;
    }

    public String getIndustryClassification()
    {
        return industryClassification;
    }

    public void setIndustryClassification(String industryClassification)
    {
        this.industryClassification = industryClassification;
    }

    public AssetClass getType()
    {
        return type;
    }

    public void setType(AssetClass type)
    {
        this.type = type;
    }

    public String getFeed()
    {
        return feed;
    }

    public void setFeed(String feed)
    {
        this.feed = feed;
    }

    public List<SecurityPrice> getPrices()
    {
        return Collections.unmodifiableList(prices);
    }

    public void addPrice(SecurityPrice price)
    {
        int index = Collections.binarySearch(prices, price);

        if (index < 0)
        {
            prices.add(price);
            Collections.sort(prices);
        }
        else
        {
            prices.set(index, price);
        }
    }

    public void removePrice(SecurityPrice price)
    {
        prices.remove(price);
    }

    public void removeAllPrices()
    {
        prices.clear();
    }

    public SecurityPrice getSecurityPrice(Date time)
    {
        if (prices.isEmpty())
        {
            if (latest != null)
                return latest;
            else
                return new SecurityPrice(time, 0);
        }

        // prefer latest quotes
        if (latest != null)
        {
            SecurityPrice last = prices.get(prices.size() - 1);

            // if 'last' younger than 'requested'
            if (last.getTime().getTime() < time.getTime())
            {
                // if 'latest' older than 'last' -> 'latest' (else 'last')
                if (latest.getTime().getTime() >= last.getTime().getTime())
                    return latest;
                else
                    return last;
            }
        }

        SecurityPrice p = new SecurityPrice(time, 0);
        int index = Collections.binarySearch(prices, p);

        if (index >= 0)
            return prices.get(index);
        else
            return prices.get(Math.max(-index - 2, 0));
    }

    public LatestSecurityPrice getLatest()
    {
        return latest;
    }

    public void setLatest(LatestSecurityPrice latest)
    {
        this.latest = latest;
    }

    public boolean isRetired()
    {
        return isRetired;
    }

    public void setRetired(boolean isRetired)
    {
        this.isRetired = isRetired;
    }

    public List<Transaction> getTransactions(Client client)
    {
        List<Transaction> answer = new ArrayList<Transaction>();

        for (Account account : client.getAccounts())
        {
            for (AccountTransaction t : account.getTransactions())
            {
                if (t.getSecurity() == null || !t.getSecurity().equals(this))
                    continue;

                switch (t.getType())
                {
                    case INTEREST:
                    case DIVIDENDS:
                        answer.add(t);
                        break;
                    case FEES:
                    case TAXES:
                    case DEPOSIT:
                    case REMOVAL:
                    case BUY:
                    case SELL:
                    case TRANSFER_IN:
                    case TRANSFER_OUT:
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            }
        }

        for (Portfolio portfolio : client.getPortfolios())
        {
            for (PortfolioTransaction t : portfolio.getTransactions())
            {
                if (!t.getSecurity().equals(this))
                    continue;

                switch (t.getType())
                {
                    case TRANSFER_IN:
                    case TRANSFER_OUT:
                    case BUY:
                    case SELL:
                    case DELIVERY_INBOUND:
                    case DELIVERY_OUTBOUND:
                        answer.add(t);
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            }
        }

        return answer;
    }

    public Security deepCopy()
    {
        Security answer = new Security();

        answer.name = name;
        answer.isin = isin;
        answer.tickerSymbol = tickerSymbol;
        answer.wkn = wkn;
        answer.type = type;
        answer.industryClassification = industryClassification;

        answer.feed = feed;
        answer.prices = new ArrayList<SecurityPrice>(prices);
        answer.latest = latest;

        answer.isRetired = isRetired;

        return answer;
    }

    @Override
    public String toString()
    {
        return getName();
    }

    public String getFinanzenFeedURL()
    {
        System.out.println("returning " + finanzenFeedURL);
        return finanzenFeedURL;
    }

    public void setFinanzenFeedURL(String finanzenFeedURL)
    {
        this.finanzenFeedURL = finanzenFeedURL;
        System.out.println("Set: " + this.finanzenFeedURL);
    }

}
