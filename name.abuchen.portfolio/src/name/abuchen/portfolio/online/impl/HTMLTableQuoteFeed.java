package name.abuchen.portfolio.online.impl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.util.Strings;

public class HTMLTableQuoteFeed implements QuoteFeed
{
    private abstract static class Column
    {
        static final ThreadLocal<DecimalFormat> DECIMAL_FORMAT_GERMAN = new ThreadLocal<DecimalFormat>()
        {
            @Override
            protected DecimalFormat initialValue()
            {
                return new DecimalFormat("#,##0.###", new DecimalFormatSymbols(Locale.GERMAN)); //$NON-NLS-1$
            }
        };

        static final ThreadLocal<DecimalFormat> DECIMAL_FORMAT_ENGLISH = new ThreadLocal<DecimalFormat>()
        {
            @Override
            protected DecimalFormat initialValue()
            {
                return new DecimalFormat("#,##0.###", new DecimalFormatSymbols(Locale.ENGLISH)); //$NON-NLS-1$
            }
        };

        private final Pattern[] patterns;

        protected Column(String[] strings)
        {
            this.patterns = new Pattern[strings.length];
            for (int ii = 0; ii < strings.length; ii++)
                this.patterns[ii] = Pattern.compile(strings[ii]);
        }

        protected boolean matches(Element header)
        {
            String text = header.text();
            for (Pattern pattern : patterns)
            {
                if (pattern.matcher(text).matches())
                    return true;
            }
            return false;
        }

        abstract void setValue(Element value, LatestSecurityPrice price) throws ParseException;

        protected long asQuote(Element value) throws ParseException
        {
            String text = value.text().trim();

            // determine format based on the relative location of the last comma
            // and dot, e.g. the last comma indicates a German number format
            int lastDot = text.lastIndexOf('.');
            int lastComma = text.lastIndexOf(',');
            DecimalFormat format = Math.max(lastDot, lastComma) == lastComma ? DECIMAL_FORMAT_GERMAN.get()
                            : DECIMAL_FORMAT_ENGLISH.get();

            double quote = format.parse(text).doubleValue();
            return Math.round(quote * Values.Quote.factor());
        }
    }

    private static class DateColumn extends Column
    {
        private DateTimeFormatter[] formatters;

        @SuppressWarnings("nls")
        public DateColumn()
        {
            super(new String[] { "Datum" });

            formatters = new DateTimeFormatter[] { DateTimeFormatter.ofPattern("d.M.yy"), //$NON-NLS-1$
                            DateTimeFormatter.ofPattern("d.M.y"), //$NON-NLS-1$
                            DateTimeFormatter.ofPattern("d. MMM y"), //$NON-NLS-1$
                            DateTimeFormatter.ofPattern("d. MMMM y"), //$NON-NLS-1$
                            DateTimeFormatter.ofPattern("d. MMM. y") //$NON-NLS-1$
            };
        }

        @Override
        void setValue(Element value, LatestSecurityPrice price) throws ParseException
        {
            String text = Strings.strip(value.text());
            for (int ii = 0; ii < formatters.length; ii++)
            {
                try
                {
                    LocalDate date = LocalDate.parse(text, formatters[ii]);
                    price.setTime(date);
                    return;
                }
                catch (DateTimeParseException e) // NOSONAR
                {
                    // continue with next pattern
                }
            }

            throw new ParseException(text, 0);
        }
    }

    private static class CloseColumn extends Column
    {
        @SuppressWarnings("nls")
        public CloseColumn()
        {
            super(new String[] { "Schluss.*", "Schluß.*", "Rücknahmepreis.*", "Close.*" });
        }

        @Override
        void setValue(Element value, LatestSecurityPrice price) throws ParseException
        {
            price.setValue(asQuote(value));
        }
    }

    private static class HighColumn extends Column
    {
        @SuppressWarnings("nls")
        public HighColumn()
        {
            super(new String[] { "Hoch.*", "Tageshoch.*", "Max.*", "High.*" });
        }

        @Override
        void setValue(Element value, LatestSecurityPrice price) throws ParseException
        {
            price.setHigh(asQuote(value));
        }
    }

    private static class LowColumn extends Column
    {
        @SuppressWarnings("nls")
        public LowColumn()
        {
            super(new String[] { "Tief.*", "Tagestief.*", "Low.*" });
        }

        @Override
        void setValue(Element value, LatestSecurityPrice price) throws ParseException
        {
            price.setLow(asQuote(value));
        }
    }

    private static class Spec
    {
        private final Column column;
        private final int index;

        public Spec(Column column, int index)
        {
            this.column = column;
            this.index = index;
        }
    }

    public static final String ID = "GENERIC_HTML_TABLE"; //$NON-NLS-1$

    private static final Column[] COLUMNS = new Column[] { new DateColumn(), new CloseColumn(), new HighColumn(),
                    new LowColumn() };

    private final PageCache cache = new PageCache();

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
    public boolean updateLatestQuotes(List<Security> securities, List<Exception> errors)
    {
        boolean isUpdated = false;

        for (Security security : securities)
        {
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

                boolean isAdded = security.setLatest(latest);
                isUpdated = isUpdated || isAdded;
            }
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
            boolean isAdded = security.addPrice(new SecurityPrice(quote.getTime(), quote.getValue()));
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

        answer = parseFromURL(feedURL, errors);

        if (!answer.isEmpty())
            cache.put(feedURL, answer);

        return answer;
    }

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(String response, List<Exception> errors)
    {
        return parseFromHTML(response, errors);
    }

    @Override
    public List<Exchange> getExchanges(Security subject, List<Exception> errors)
    {
        return Collections.emptyList();
    }

    @SuppressWarnings("nls")
    protected List<LatestSecurityPrice> parseFromURL(String url, List<Exception> errors)
    {
        // without a user agent, some sites serve a mobile/alternative version
        String userAgent;

        String os = System.getProperty("os.name", "unknown").toLowerCase();
        if (os.startsWith("windows"))
            userAgent = "Mozilla/5.0 (Windows NT 6.2; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.77 Safari/537.36";
        else if (os.startsWith("mac"))
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_1) AppleWebKit/537.73.11 (KHTML, like Gecko) Version/7.0.1 Safari/537.73.11";
        else
            userAgent = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:25.0) Gecko/20100101 Firefox/25.0";

        try
        {
            String escapedUrl = new URI(url).toASCIIString();
            return parse(Jsoup.connect(escapedUrl).userAgent(userAgent).timeout(30000).get(), errors);
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
                        LatestSecurityPrice price = extractPrice(row, specs);
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

    private boolean isSpecValid(List<Spec> specs)
    {
        if (specs == null || specs.isEmpty())
            return false;

        boolean hasDate = false;
        boolean hasClose = false;

        for (Spec spec : specs)
        {
            hasDate = hasDate || spec.column instanceof DateColumn;
            hasClose = hasClose || spec.column instanceof CloseColumn;
        }

        return hasDate && hasClose;
    }

    private LatestSecurityPrice extractPrice(Element row, List<Spec> specs) throws ParseException
    {
        Elements cells = row.select("> td"); //$NON-NLS-1$

        // row can be empty if it contains only 'th' elements
        if (cells.size() <= 1)
            return null;

        LatestSecurityPrice price = new LatestSecurityPrice();

        for (Spec spec : specs)
            spec.column.setValue(cells.get(spec.index), price);

        return price;
    }

    /**
     * Test method to parse HTML tables
     * 
     * @param args
     *            list of URLs and/or local files
     */
    public static void main(String[] args) throws IOException
    {
        PrintWriter writer = new PrintWriter(System.out); // NOSONAR
        for (String arg : args)
            if (arg.charAt(0) != '#')
                doLoad(arg, writer);
        writer.flush();
    }

    @SuppressWarnings("nls")
    private static void doLoad(String source, PrintWriter writer) throws IOException
    {
        writer.println("--------");
        writer.println(source);
        writer.println("--------");

        List<LatestSecurityPrice> prices;
        List<Exception> errors = new ArrayList<>();

        if (source.startsWith("http"))
        {
            prices = new HTMLTableQuoteFeed().parseFromURL(source, errors);
        }
        else
        {
            try (Scanner scanner = new Scanner(new File(source), StandardCharsets.UTF_8.name()))
            {
                String html = scanner.useDelimiter("\\A").next();
                prices = new HTMLTableQuoteFeed().parseFromHTML(html, errors);
            }
        }

        for (Exception error : errors)
            error.printStackTrace(writer); // NOSONAR

        for (LatestSecurityPrice p : prices)
        {
            writer.print(Values.Date.format(p.getTime()));
            writer.print("\t");
            writer.print(Values.Quote.format(p.getValue()));
            writer.print("\t");
            writer.print(Values.Quote.format(p.getLow()));
            writer.print("\t");
            writer.println(Values.Quote.format(p.getHigh()));
        }
    }
}
