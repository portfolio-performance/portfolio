package name.abuchen.portfolio.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import name.abuchen.portfolio.money.CurrencyUnit;

public final class Security implements Attributable, InvestmentVehicle
{
    public static final class ByName implements Comparator<Security>, Serializable
    {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(Security s1, Security s2)
        {
            if (s1 == null)
                return s2 == null ? 0 : -1;
            return s1.name.compareToIgnoreCase(s2.name);
        }
    }

    private String uuid;

    private String name;
    private String currencyCode = CurrencyUnit.EUR;

    private String note;

    private String isin;
    private String tickerSymbol;
    private String wkn;

    // feed and feedURL are used to update historical prices
    private String feed;
    private String feedURL;
    private List<SecurityPrice> prices = new ArrayList<>();

    // latestFeed and latestFeedURL are used to update the latest (current)
    // quote. If null, the values from feed and feedURL are used instead.
    private String latestFeed;
    private String latestFeedURL;
    private LatestSecurityPrice latest;

    private Attributes attributes;

    private List<SecurityEvent> events;

    private boolean isRetired = false;

    @Deprecated
    private String type;

    @Deprecated
    private String industryClassification;

    public Security()
    {
        this.uuid = UUID.randomUUID().toString();
    }

    public Security(String name, String currencyCode)
    {
        this();
        this.name = name;
        this.currencyCode = currencyCode;
    }

    public Security(String name, String isin, String tickerSymbol, String feed)
    {
        this();
        this.name = name;
        this.isin = isin;
        this.tickerSymbol = tickerSymbol;
        this.feed = feed;
    }

    @Override
    public String getUUID()
    {
        return uuid;
    }

    /* package */void generateUUID()
    {
        // needed to assign UUIDs when loading older versions from XML
        uuid = UUID.randomUUID().toString();
    }

    /**
     * Generates a UUID only if no UUID exists. For not yet known reasons, some
     * securities do miss the UUID. However, we do not want to make the
     * #generateUUID function public.
     */
    public void fixMissingUUID()
    {
        if (uuid == null)
            generateUUID();
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public String getCurrencyCode()
    {
        return currencyCode;
    }

    @Override
    public void setCurrencyCode(String currencyCode)
    {
        this.currencyCode = currencyCode;
    }

    @Override
    public String getNote()
    {
        return note;
    }

    @Override
    public void setNote(String note)
    {
        this.note = note;
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

    @Deprecated
    /* package */String getIndustryClassification()
    {
        return industryClassification;
    }

    @Deprecated
    /* package */void setIndustryClassification(String industryClassification)
    {
        this.industryClassification = industryClassification;
    }

    @Deprecated
    /* package */String getType()
    {
        return type;
    }

    @Deprecated
    /* package */void setType(String type)
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

    public String getFeedURL()
    {
        return feedURL;
    }

    public void setFeedURL(String feedURL)
    {
        this.feedURL = feedURL;
    }

    public List<SecurityPrice> getPrices()
    {
        return Collections.unmodifiableList(prices);
    }

    /**
     * Returns a list of historical security prices that includes the latest
     * security price if no history price exists for that date
     */
    public List<SecurityPrice> getPricesIncludingLatest()
    {
        if (latest == null)
            return getPrices();

        int index = Collections.binarySearch(prices, new SecurityPrice(latest.getDate(), latest.getValue()));

        if (index >= 0) // historic quote exists -> use it
            return getPrices();

        List<SecurityPrice> copy = new ArrayList<>(prices);
        copy.add(~index, latest);
        return copy;
    }

    /**
     * Adds security price to historical quotes.
     * 
     * @return true if the historical quote was updated.
     */
    public boolean addPrice(SecurityPrice price)
    {
        Objects.requireNonNull(price);

        int index = Collections.binarySearch(prices, price);

        if (index < 0)
        {
            prices.add(~index, price);
            return true;
        }
        else
        {
            SecurityPrice replaced = prices.get(index);

            if (!replaced.equals(price))
            {
                // only replace if necessary -> UI might keep reference!
                prices.set(index, price);
                return true;
            }
            else
            {
                return false;
            }
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

    public SecurityPrice getSecurityPrice(LocalDate requestedDate)
    {
        // assumption: prefer historic quote over latest if there are more
        // up-to-date historic quotes

        SecurityPrice lastHistoric = prices.isEmpty() ? null : prices.get(prices.size() - 1);

        // use latest quote only
        // * if one exists
        // * and if either no historic quotes exist
        // * or
        // ** if the requested time is after the latest quote
        // ** and the historic quotes are older than the latest quote

        if (latest != null //
                        && (lastHistoric == null //
                                        || (!requestedDate.isBefore(latest.getDate()) && //
                                                        !latest.getDate().isBefore(lastHistoric.getDate()) //
                                        )))
            return latest;

        if (lastHistoric == null)
            return new SecurityPrice(requestedDate, 0);

        // avoid binary search if last historic quote <= requested date
        if (!lastHistoric.getDate().isAfter(requestedDate))
            return lastHistoric;

        SecurityPrice p = new SecurityPrice(requestedDate, 0);
        int index = Collections.binarySearch(prices, p);

        if (index >= 0)
            return prices.get(index);
        else if (index == -1) // requested is date before first historic quote
            return prices.get(0);
        else
            return prices.get(-index - 2);
    }

    public String getLatestFeed()
    {
        return latestFeed;
    }

    public void setLatestFeed(String latestFeed)
    {
        this.latestFeed = latestFeed;
    }

    public String getLatestFeedURL()
    {
        return latestFeedURL;
    }

    public void setLatestFeedURL(String latestFeedURL)
    {
        this.latestFeedURL = latestFeedURL;
    }

    public LatestSecurityPrice getLatest()
    {
        return latest;
    }

    /**
     * Sets the latest security price.
     * 
     * @return true if the latest security price was updated.
     */
    public boolean setLatest(LatestSecurityPrice latest)
    {
        // only replace if necessary -> UI might keep reference!
        if ((this.latest != null && !this.latest.equals(latest)) || (this.latest == null && latest != null))
        {
            this.latest = latest;
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public boolean isRetired()
    {
        return isRetired;
    }

    @Override
    public void setRetired(boolean isRetired)
    {
        this.isRetired = isRetired;
    }

    public List<SecurityEvent> getEvents()
    {
        if (this.events == null)
            this.events = new ArrayList<>();
        return events;
    }

    public void addEvent(SecurityEvent event)
    {
        if (this.events == null)
            this.events = new ArrayList<>();
        this.events.add(event);
    }

    @Override
    public Attributes getAttributes()
    {
        if (attributes == null)
            attributes = new Attributes();
        return attributes;
    }

    @Override
    public void setAttributes(Attributes attributes)
    {
        this.attributes = attributes;
    }

    public List<TransactionPair<?>> getTransactions(Client client)
    {
        List<TransactionPair<?>> answer = new ArrayList<>();

        for (Account account : client.getAccounts())
        {
            account.getTransactions().stream() //
                            .filter(t -> this.equals(t.getSecurity()))
                            .filter(t -> t.getType() == AccountTransaction.Type.INTEREST
                                            || t.getType() == AccountTransaction.Type.DIVIDENDS
                                            || t.getType() == AccountTransaction.Type.TAXES
                                            || t.getType() == AccountTransaction.Type.TAX_REFUND
                                            || t.getType() == AccountTransaction.Type.FEES
                                            || t.getType() == AccountTransaction.Type.FEES_REFUND)
                            .map(t -> new TransactionPair<AccountTransaction>(account, t)) //
                            .forEach(answer::add);
        }

        for (Portfolio portfolio : client.getPortfolios())
        {
            portfolio.getTransactions().stream() //
                            .filter(t -> this.equals(t.getSecurity()))
                            .map(t -> new TransactionPair<PortfolioTransaction>(portfolio, t)) //
                            .forEach(answer::add);
        }

        return answer;
    }

    public boolean hasTransactions(Client client)
    {
        for (Portfolio portfolio : client.getPortfolios())
        {
            Optional<PortfolioTransaction> transaction = portfolio.getTransactions().stream()
                            .filter(t -> this.equals(t.getSecurity())).findAny();

            if (transaction.isPresent())
                return true;
        }

        for (Account account : client.getAccounts())
        {
            Optional<AccountTransaction> transaction = account.getTransactions().stream()
                            .filter(t -> this.equals(t.getSecurity())).findAny();

            if (transaction.isPresent())
                return true;
        }

        return false;
    }

    public Security deepCopy()
    {
        Security answer = new Security();

        answer.name = name;
        answer.currencyCode = currencyCode;

        answer.note = note;
        answer.isin = isin;
        answer.tickerSymbol = tickerSymbol;
        answer.wkn = wkn;

        answer.feed = feed;
        answer.feedURL = feedURL;
        answer.prices = new ArrayList<>(prices);

        answer.latestFeed = latestFeed;
        answer.latestFeedURL = latestFeedURL;
        answer.latest = latest;

        answer.events = new ArrayList<>(getEvents());

        answer.isRetired = isRetired;

        return answer;
    }

    @Override
    public String toString()
    {
        return getName();
    }

    public String toInfoString()
    {
        StringBuilder b = new StringBuilder();
        b.append(name);

        if (notEmpty(isin))
            b.append('\n').append(isin);
        if (notEmpty(wkn))
            b.append('\n').append(wkn);
        if (notEmpty(tickerSymbol))
            b.append('\n').append(tickerSymbol);

        if (notEmpty(note))
            b.append("\n\n").append(note); //$NON-NLS-1$

        return b.toString();
    }

    private boolean notEmpty(String s)
    {
        return s != null && s.length() > 0;
    }
}
