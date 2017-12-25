package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.online.QuoteFeed;

public class HTMLTableQuoteFeed extends QuoteFeed
{

    public static final String ID = HTML; //$NON-NLS-1$

    private final PageCache cache = new PageCache();
    private final HTMLTableQuoteParser Parser = new HTMLTableQuoteParser();

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return Messages.LabelHTMLTable;
    }

    @Override
    public boolean updateLatestQuotes(Security security, List<Exception> errors)
    {
        boolean isUpdated = false;

        // if latestFeed is null, then the policy is 'use same configuration
        // as historic quotes'
        String feedURL = security.getLatestFeed() == null ? security.getFeedURL() : security.getLatestFeedURL();

        List<LatestSecurityPrice> quotes = internalGetQuotes(security, feedURL, errors);
        int size = quotes.size();
        if (size > 0)
        {
            Collections.sort(quotes);

            LatestSecurityPrice latest = quotes.get(size - 1);
            LatestSecurityPrice previous = size > 1 ? quotes.get(size - 2) : null;
            latest.setPreviousClose(previous != null ? previous.getValue() : latest.getValue());
            latest.setVolume(LatestSecurityPrice.NOT_AVAILABLE);

            isUpdated = security.setLatest(latest);
        }

        return isUpdated;
    }

    @Override
    public boolean updateHistoricalQuotes(Security security, List<Exception> errors)
    {
        List<LatestSecurityPrice> quotes = internalGetQuotes(security, security.getFeedURL(), errors);

        boolean isUpdated = false;
        for (LatestSecurityPrice quote : quotes)
        {
            boolean isAdded = security.addPrice(new SecurityPrice(quote.getDate(), quote.getValue()));
            isUpdated = isUpdated || isAdded;
        }

        return isUpdated;
    }

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(Security security, LocalDate start, List<Exception> errors)
    {
        return internalGetQuotes(security, security.getFeedURL(), errors);
    }

    private List<LatestSecurityPrice> internalGetQuotes(Security security, String feedURL, List<Exception> errors)
    {
        if (feedURL == null || feedURL.length() == 0)
        {
            errors.add(new IOException(MessageFormat.format(Messages.MsgMissingFeedURL, security.getName())));
            return Collections.emptyList();
        }

        List<LatestSecurityPrice> answer = cache.lookup(feedURL);
        if (answer != null)
            return answer;

        answer = Parser.parseFromURL(feedURL, errors);

        if (!answer.isEmpty())
            cache.put(feedURL, answer);

        return answer;
    }

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(String response, List<Exception> errors)
    {
        return Parser.parseFromHTML(response, errors);
    }

    @Override
    public List<Exchange> getExchanges(Security subject, List<Exception> errors)
    {
        return Collections.emptyList();
    }

    protected List<LatestSecurityPrice> parseFromURL(String url, List<Exception> errors)
    {
        try
        {
            String escapedUrl = new URI(url).toASCIIString();
            return parse(Jsoup.connect(escapedUrl).userAgent(OnlineHelper.getUserAgent()).timeout(30000).get(), errors);
        }
        catch (URISyntaxException | IOException e)
        {
            errors.add(e);
            return Collections.emptyList();
        }
    }

    protected List<LatestSecurityPrice> parseFromHTML(String html, List<Exception> errors)
    {
        return parse(Jsoup.parse(html), errors);
    }

    private List<LatestSecurityPrice> parse(Document document, List<Exception> errors)
    {
        // check if language is provided
        String language = document.select("html").attr("lang"); //$NON-NLS-1$ //$NON-NLS-2$

        List<LatestSecurityPrice> prices = new ArrayList<>();

        // first: find tables
        Elements tables = document.getElementsByTag("table"); //$NON-NLS-1$
        for (Element table : tables)
        {
            List<Spec> specs = new ArrayList<>();

            int rowIndex = buildSpecFromTable(table, specs);

            if (isSpecValid(specs))
            {
                Elements rows = table.select("> tbody > tr"); //$NON-NLS-1$

                int size = rows.size();
                for (; rowIndex < size; rowIndex++)
                {
                    Element row = rows.get(rowIndex);

                    try
                    {
                        LatestSecurityPrice price = extractPrice(row, specs, language);
                        if (price != null)
                            prices.add(price);
                    }
                    catch (Exception e)
                    {
                        errors.add(e);
                    }
                }

                // skip all other tables
                break;
            }
        }

        // if no quotes could be extract, log HTML for further analysis
        if (prices.isEmpty())
            errors.add(new IOException(MessageFormat.format(Messages.MsgNoQuotesFoundInHTML, document.html())));

        return prices;
    }

    @SuppressWarnings("nls")
    private int buildSpecFromTable(Element table, List<Spec> specs)
    {
        // check if thead exists
        Elements header = table.select("> thead > tr > th");
        if (!header.isEmpty())
        {
            buildSpecFromRow(header, specs);
            return 0;
        }

        header = table.select("> thead > tr > td");
        if (!header.isEmpty())
        {
            buildSpecFromRow(header, specs);
            return 0;
        }

        // check if th exist in body
        header = table.select("> tbody > tr > th");
        if (!header.isEmpty())
        {
            buildSpecFromRow(header, specs);
            return 0;
        }

        // then check first two regular rows
        int rowIndex = 0;

        Elements rows = table.select("> tbody > tr");
        if (!rows.isEmpty())
        {
            Element firstRow = rows.get(0);
            buildSpecFromRow(firstRow.select("> td"), specs);
            rowIndex++;
        }

        if (specs.isEmpty() && rows.size() > 1)
        {
            Element secondRow = rows.get(1);
            buildSpecFromRow(secondRow.select("> td"), specs);
            rowIndex++;
        }

        return rowIndex;
    }

    private void buildSpecFromRow(Elements row, List<Spec> specs)
    {
        Set<Column> available = new HashSet<>();
        for (Column column : COLUMNS)
            available.add(column);

        for (int ii = 0; ii < row.size(); ii++)
        {
            Element element = row.get(ii);

            for (Column column : available)
            {
                if (column.matches(element))
                {
                    specs.add(new Spec(column, ii));
                    available.remove(column);
                    break;
                }
            }
        }
    }

    private LatestSecurityPrice extractPrice(Element row, List<Spec> specs, String languageHint) throws ParseException
    {
        Elements cells = row.select("> td"); //$NON-NLS-1$

        // row can be empty if it contains only 'th' elements
        if (cells.size() <= 1)
            return null;

        LatestSecurityPrice price = new LatestSecurityPrice();

        for (Spec spec : specs)
            spec.column.setValue(cells.get(spec.index), price, languageHint);

        return price;
    }

   
}
