package name.abuchen.portfolio.online.impl;

import static name.abuchen.portfolio.online.impl.YahooHelper.asPrice;
import static name.abuchen.portfolio.online.impl.YahooHelper.stripQuotes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.SecuritySearchProvider;
import name.abuchen.portfolio.online.impl.YahooSymbolSearch.Result;

public class YahooSearchProvider implements SecuritySearchProvider
{
    private static final String SEARCH_URL = "https://de.finance.yahoo.com/lookup?s=%s&t=A&b=0&m=ALL"; //$NON-NLS-1$
    private static final String LOOKUP_URL = "https://download.finance.yahoo.com/d/quotes.csv?s=%s&f=snl1"; //$NON-NLS-1$

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

        public static ResultItem from(Result r)
        {
            YahooResultItem item = new YahooResultItem();
            item.setSymbol(r.getSymbol());
            item.setName(r.getName());
            item.setExchange(r.getExchange());
            item.setType(r.getType());
            return item;
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
        // search both the HTML page as well as the symbol search
        String url = String.format(SEARCH_URL, URLEncoder.encode(query, StandardCharsets.UTF_8.name()));
        Document document = Jsoup.connect(url).get();

        List<ResultItem> answer = extractFrom(document);
        addSymbolSearchResults(answer, query);

        if (answer.isEmpty())
        {
            ResultItem item = searchCSV(query);

            if (item == null)
            {
                item = new YahooResultItem();
                item.setName(String.format(Messages.MsgNoResults, query));
            }

            answer.add(item);
        }
        else if (answer.size() >= 20)
        {
            ResultItem item = new YahooResultItem();
            item.setName(Messages.MsgMoreResultsAvailable);
            answer.add(item);
        }

        return answer;
    }

    private void addSymbolSearchResults(List<ResultItem> answer, String query) throws IOException
    {
        Set<String> existingSymbols = answer.stream().map(r -> r.getSymbol()).collect(Collectors.toSet());

        new YahooSymbolSearch().search(query)//
                        .filter(r -> !existingSymbols.contains(r.getSymbol()))
                        .forEach(r -> answer.add(YahooResultItem.from(r)));
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

    /* protected */ResultItem searchCSV(String query) throws IOException
    {
        String csv = String.format(LOOKUP_URL, URLEncoder.encode(query.toUpperCase(), StandardCharsets.UTF_8.name()));

        BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(csv).openStream()));

        String line = reader.readLine();

        if (line != null)
        {
            String[] values = line.split(","); //$NON-NLS-1$

            // result must have 3 values -> otherwise error message
            if (values.length != 3)
                return null;

            // Yahoo always returns a value if query is a syntactically correct
            // symbol even if it does not exist -> filter
            String symbol = stripQuotes(values[0]);
            String name = stripQuotes(values[1]);
            if (symbol.equals(name))
                return null;

            try
            {
                ResultItem answer = new ResultItem();
                answer.setSymbol(symbol);
                answer.setName(name);
                answer.setLastTrade(asPrice(values[2]));
                return answer;
            }
            catch (ParseException e)
            {
                throw new IOException(e);
            }
        }
        else
        {
            return null;
        }
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
