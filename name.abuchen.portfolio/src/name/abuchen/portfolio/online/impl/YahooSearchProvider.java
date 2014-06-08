package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.online.SecuritySearchProvider;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class YahooSearchProvider implements SecuritySearchProvider
{
    private static final String SEARCH_URL = "https://de.finance.yahoo.com/lookup?s=%s&t=A&b=0&m=ALL"; //$NON-NLS-1$

    private static final ThreadLocal<DecimalFormat> FMT_INDEX = new ThreadLocal<DecimalFormat>()
    {
        protected DecimalFormat initialValue()
        {
            return new DecimalFormat("#,##0.##", new DecimalFormatSymbols(Locale.GERMANY)); //$NON-NLS-1$
        }
    };

    public static class YahooResultItem extends ResultItem
    {
        @Override
        public void applyTo(Security security)
        {
            super.applyTo(security);
            security.setFeed(YahooFinanceQuoteFeed.ID);
        }
    }

    @Override
    public String getName()
    {
        return Messages.LabelYahooFinance;
    }

    @Override
    public List<ResultItem> search(String query) throws IOException
    {
        String url = String.format(SEARCH_URL, URLEncoder.encode(query + "*", "UTF-8")); //$NON-NLS-1$ //$NON-NLS-2$
        Document document = Jsoup.connect(url).get();

        List<ResultItem> answer = extractFrom(document);

        if (answer.isEmpty())
        {
            ResultItem item = new YahooResultItem();
            item.setName(String.format(Messages.MsgNoResults, query));
            answer.add(item);
        }
        else if (answer.size() == 20)
        {
            ResultItem item = new YahooResultItem();
            item.setName(Messages.MsgMoreResultsAvailable);
            answer.add(item);
        }

        return answer;
    }

    /* protected */List<ResultItem> extractFrom(Document document) throws IOException
    {
        List<ResultItem> answer = new ArrayList<ResultItem>();

        Elements tables = document.getElementsByAttribute("SUMMARY"); //$NON-NLS-1$

        for (Element table : tables)
        {
            if (!"YFT_SL_TABLE_SUMMARY".equals(table.attr("SUMMARY"))) //$NON-NLS-1$ //$NON-NLS-2$
                continue;

            Elements rows = table.select("> tbody > tr"); //$NON-NLS-1$

            for (Element row : rows)
            {
                Elements cells = row.select("> td"); //$NON-NLS-1$

                if (cells.size() != 6)
                    continue;

                ResultItem item = new YahooResultItem();

                item.setSymbol(cells.get(0).text());
                item.setName(cells.get(1).text());
                item.setIsin(cells.get(2).text());

                // last trace
                String lastTrade = cells.get(3).text();
                if (!"NaN".equals(lastTrade)) //$NON-NLS-1$
                    item.setLastTrade(parseIndex(lastTrade));

                item.setType(cells.get(4).text());
                item.setExchange(cells.get(5).text());

                answer.add(item);
            }
        }

        return answer;
    }

    private long parseIndex(String text) throws IOException
    {
        try
        {
            Number q = FMT_INDEX.get().parse(text);
            return (long) (q.doubleValue() * Values.Quote.factor());
        }
        catch (ParseException e)
        {
            throw new IOException(e);
        }
    }
}
