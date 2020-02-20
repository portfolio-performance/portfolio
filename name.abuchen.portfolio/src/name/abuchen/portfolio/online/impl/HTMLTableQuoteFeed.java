package name.abuchen.portfolio.online.impl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.impl.variableurl.Factory;
import name.abuchen.portfolio.online.impl.variableurl.urls.VariableURL;
import name.abuchen.portfolio.util.OnlineHelper;
import name.abuchen.portfolio.util.TextUtil;
import name.abuchen.portfolio.util.WebAccess;

public class HTMLTableQuoteFeed implements QuoteFeed
{
    protected abstract static class Column
    {
        static final ThreadLocal<DecimalFormat> DECIMAL_FORMAT_GERMAN = ThreadLocal
                        .withInitial(() -> new DecimalFormat("#,##0.###", new DecimalFormatSymbols(Locale.GERMAN))); //$NON-NLS-1$

        static final ThreadLocal<DecimalFormat> DECIMAL_FORMAT_ENGLISH = ThreadLocal
                        .withInitial(() -> new DecimalFormat("#,##0.###", new DecimalFormatSymbols(Locale.ENGLISH))); //$NON-NLS-1$

        static final ThreadLocal<DecimalFormat> DECIMAL_FORMAT_APOSTROPHE = ThreadLocal.withInitial(() -> {
            DecimalFormatSymbols unusualSymbols = new DecimalFormatSymbols(Locale.US);
            unusualSymbols.setGroupingSeparator('\'');
            return new DecimalFormat("#,##0.##", unusualSymbols); //$NON-NLS-1$
        });

        private final Pattern[] patterns;

        protected Column(String[] strings)
        {
            this.patterns = new Pattern[strings.length];
            for (int ii = 0; ii < strings.length; ii++)
                this.patterns[ii] = Pattern.compile(strings[ii]);
        }

        protected boolean matches(Element header)
        {
            String text = TextUtil.strip(header.text());
            for (Pattern pattern : patterns)
            {
                if (pattern.matcher(text).matches())
                    return true;
            }
            return false;
        }

        abstract void setValue(Element value, LatestSecurityPrice price, String languageHint) throws ParseException;

        protected long asQuote(Element value, String languageHint) throws ParseException
        {
            String text = value.text().trim();

            DecimalFormat format = null;

            if ("de".equals(languageHint)) //$NON-NLS-1$
                format = DECIMAL_FORMAT_GERMAN.get();
            else if ("en".equals(languageHint)) //$NON-NLS-1$
                format = DECIMAL_FORMAT_ENGLISH.get();

            if (format == null)
            {
                // check first for apostrophe

                int apostrophe = text.indexOf('\'');
                if (apostrophe >= 0)
                    format = DECIMAL_FORMAT_APOSTROPHE.get();
            }

            if (format == null)
            {
                // determine format based on the relative location of the last
                // comma and dot, e.g. the last comma indicates a German number
                // format
                int lastDot = text.lastIndexOf('.');
                int lastComma = text.lastIndexOf(',');
                format = Math.max(lastDot, lastComma) == lastComma ? DECIMAL_FORMAT_GERMAN.get()
                                : DECIMAL_FORMAT_ENGLISH.get();
            }

            double quote = format.parse(text).doubleValue();
            return Math.round(quote * Values.Quote.factor());
        }
    }

    protected static class DateColumn extends Column
    {
        private DateTimeFormatter[] formatters;

        @SuppressWarnings("nls")
        public DateColumn()
        {
            this(new String[] { "Datum.*", "Date.*" });
        }

        @SuppressWarnings("nls")
        public DateColumn(String[] patterns)
        {
            super(patterns);

            formatters = new DateTimeFormatter[] { DateTimeFormatter.ofPattern("y-M-d"),
                            DateTimeFormatter.ofPattern("d.M.yy"), //$NON-NLS-1$
                            DateTimeFormatter.ofPattern("d.M.y"), //$NON-NLS-1$
                            DateTimeFormatter.ofPattern("d. MMM y"), //$NON-NLS-1$
                            DateTimeFormatter.ofPattern("d. MMMM y"), //$NON-NLS-1$
                            DateTimeFormatter.ofPattern("d. MMM. y"), //$NON-NLS-1$
                            DateTimeFormatter.ofPattern("MMM d, y", Locale.ENGLISH), //$NON-NLS-1$
                            DateTimeFormatter.ofPattern("MMM dd, y", Locale.ENGLISH), //$NON-NLS-1$
                            DateTimeFormatter.ofPattern("EEEE, MMMM dd, yEEE, MMM dd, y", Locale.ENGLISH) //$NON-NLS-1$
            };
        }

        @Override
        void setValue(Element value, LatestSecurityPrice price, String languageHint) throws ParseException
        {
            String text = TextUtil.strip(value.text());
            for (int ii = 0; ii < formatters.length; ii++)
            {
                try
                {
                    LocalDate date = LocalDate.parse(text, formatters[ii]);
                    price.setDate(date);
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

    protected static class CloseColumn extends Column
    {
        @SuppressWarnings("nls")
        public CloseColumn()
        {
            super(new String[] { "Schluss.*", "Schluß.*", "Rücknahmepreis.*", "Close.*", "Zuletzt", "Price",
                            "akt. Kurs" });
        }

        public CloseColumn(String[] patterns)
        {
            super(patterns);
        }

        @Override
        void setValue(Element value, LatestSecurityPrice price, String languageHint) throws ParseException
        {
            price.setValue(asQuote(value, languageHint));
        }
    }

    protected static class HighColumn extends Column
    {
        @SuppressWarnings("nls")
        public HighColumn()
        {
            super(new String[] { "Hoch.*", "Tageshoch.*", "Max.*", "High.*" });
        }

        public HighColumn(String[] patterns)
        {
            super(patterns);
        }

        @Override
        void setValue(Element value, LatestSecurityPrice price, String languageHint) throws ParseException
        {
            if ("-".equals(value.text().trim())) //$NON-NLS-1$
                price.setHigh(LatestSecurityPrice.NOT_AVAILABLE);
            else
                price.setHigh(asQuote(value, languageHint));
        }
    }

    protected static class LowColumn extends Column
    {
        @SuppressWarnings("nls")
        public LowColumn()
        {
            super(new String[] { "Tief.*", "Tagestief.*", "Low.*" });
        }

        public LowColumn(String[] patterns)
        {
            super(patterns);
        }

        @Override
        void setValue(Element value, LatestSecurityPrice price, String languageHint) throws ParseException
        {
            if ("-".equals(value.text().trim())) //$NON-NLS-1$
                price.setLow(LatestSecurityPrice.NOT_AVAILABLE);
            else
                price.setLow(asQuote(value, languageHint));
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

    private static class HeaderInfo
    {
        private final int rowIndex;
        private final int numberOfHeaderColumns;

        public HeaderInfo(int rowIndex, int numberOfHeaderColumns)
        {
            this.rowIndex = rowIndex;
            this.numberOfHeaderColumns = numberOfHeaderColumns;
        }
    }

    public static final String ID = "GENERIC_HTML_TABLE"; //$NON-NLS-1$

    private static final Column[] COLUMNS = new Column[] { new DateColumn(), new CloseColumn(), new HighColumn(),
                    new LowColumn() };

    private final PageCache<List<LatestSecurityPrice>> cache = new PageCache<>();

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
    public Optional<String> getHelpURL()
    {
        return Optional.of("https://help.portfolio-performance.info/kursdaten_laden/#tabelle-auf-einer-webseite"); //$NON-NLS-1$
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

        VariableURL variableURL = Factory.fromString(feedURL);
        variableURL.setSecurity(security);

        SortedSet<LatestSecurityPrice> newPricesByDate = new TreeSet<>(new SecurityPrice.ByDate());
        long failedAttempts = 0;
        long maxFailedAttempts = variableURL.getMaxFailedAttempts();

        for (String url : variableURL)
        {
            List<LatestSecurityPrice> answer = cache.lookup(url);

            if (answer == null)
            {
                answer = parseFromURL(url, errors);

                if (!answer.isEmpty())
                    cache.put(url, answer);
            }

            int sizeBefore = newPricesByDate.size();
            newPricesByDate.addAll(answer);

            if (newPricesByDate.size() > sizeBefore)
                failedAttempts = 0;
            else if (++failedAttempts > maxFailedAttempts)
                break;
        }

        return new ArrayList<>(newPricesByDate);
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

    protected String getUserAgent()
    {
        return OnlineHelper.getUserAgent();
    }

    protected List<LatestSecurityPrice> parseFromURL(String url, List<Exception> errors)
    {
        try
        {
            Document document = Jsoup.parse(new WebAccess(url) //
                            .addUserAgent(getUserAgent()) //
                            .get());
            return parse(url, document, errors);
        }
        catch (URISyntaxException | IOException e)
        {
            errors.add(new IOException(url + '\n' + e.getMessage(), e));
            return Collections.emptyList();
        }
    }

    protected List<LatestSecurityPrice> parseFromHTML(String html, List<Exception> errors)
    {
        return parse("n/a", Jsoup.parse(html), errors); //$NON-NLS-1$
    }

    private List<LatestSecurityPrice> parse(String url, Document document, List<Exception> errors)
    {
        // check if language is provided
        String language = document.select("html").attr("lang"); //$NON-NLS-1$ //$NON-NLS-2$

        List<LatestSecurityPrice> prices = new ArrayList<>();

        // first: find tables
        Elements tables = document.getElementsByTag("table"); //$NON-NLS-1$
        for (Element table : tables)
        {
            List<Spec> specs = new ArrayList<>();

            HeaderInfo headerInfo = buildSpecFromTable(table, specs);
            int rowIndex = headerInfo.rowIndex;

            if (isSpecValid(specs))
            {
                Elements rows = table.select("> tbody > tr"); //$NON-NLS-1$

                int size = rows.size();
                if (size != 0)
                {
                    for (; rowIndex < size; rowIndex++)
                    {
                        Element row = rows.get(rowIndex);

                        try
                        {
                            LatestSecurityPrice price = extractPrice(row, specs, language,
                                            headerInfo.numberOfHeaderColumns);
                            if (price != null)
                                prices.add(price);
                        }
                        catch (Exception e)
                        {
                            errors.add(new IOException(url + '\n' + e.getMessage(), e));
                        }
                    }

                    // skip all other tables
                    break;
                }
            }
        }

        // if no quotes could be extract, log HTML for further analysis
        if (prices.isEmpty())
            errors.add(new IOException(MessageFormat.format(Messages.MsgNoQuotesFoundInHTML, url,
                            Jsoup.clean(document.html(), Whitelist.relaxed()))));

        return prices;
    }

    @SuppressWarnings("nls")
    private HeaderInfo buildSpecFromTable(Element table, List<Spec> specs)
    {
        // check if thead exists
        Elements header = table.select("> thead > tr > th");
        if (!header.isEmpty())
        {
            buildSpecFromRow(header, specs);
            if (!specs.isEmpty())
                return new HeaderInfo(0, header.size());
        }

        header = table.select("> thead > tr > td");
        if (!header.isEmpty())
        {
            buildSpecFromRow(header, specs);
            if (!specs.isEmpty())
                return new HeaderInfo(0, header.size());
        }

        // check if th exist in body
        header = table.select("> tbody > tr > th");
        if (!header.isEmpty())
        {
            buildSpecFromRow(header, specs);
            if (!specs.isEmpty())
                return new HeaderInfo(0, header.size());
        }

        // then check first two regular rows
        int rowIndex = 0;

        Elements rows = table.select("> tbody > tr");
        Elements headerRow = null;

        if (!rows.isEmpty())
        {
            Element firstRow = rows.get(0);
            headerRow = firstRow.select("> td");
            buildSpecFromRow(headerRow, specs);
            rowIndex++;
        }

        if (specs.isEmpty() && rows.size() > 1)
        {
            Element secondRow = rows.get(1);
            headerRow = secondRow.select("> td");
            buildSpecFromRow(headerRow, specs);
            rowIndex++;
        }

        return new HeaderInfo(rowIndex, headerRow != null ? headerRow.size() : 0);
    }

    protected Column[] getColumns()
    {
        return COLUMNS;
    }

    private void buildSpecFromRow(Elements row, List<Spec> specs)
    {
        Set<Column> available = new HashSet<>();
        Collections.addAll(available, getColumns());

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

    private LatestSecurityPrice extractPrice(Element row, List<Spec> specs, String languageHint,
                    int numberOfHeaderColumns) throws ParseException
    {
        Elements cells = row.select("> td"); //$NON-NLS-1$

        // we're only looking at rows having the same size as the header row
        if (cells.size() != numberOfHeaderColumns)
            return null;

        LatestSecurityPrice price = new LatestSecurityPrice();

        for (Spec spec : specs)
            spec.column.setValue(cells.get(spec.index), price, languageHint);

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
                new HTMLTableQuoteFeed().doLoad(arg, writer);
        writer.flush();
    }

    @SuppressWarnings("nls")
    protected void doLoad(String source, PrintWriter writer) throws IOException
    {
        writer.println("--------");
        writer.println(source);
        writer.println("--------");

        List<LatestSecurityPrice> prices;
        List<Exception> errors = new ArrayList<>();

        if (source.startsWith("http"))
        {
            prices = parseFromURL(source, errors);
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
            writer.print(Values.Date.format(p.getDate()));
            writer.print("\t");
            writer.print(Values.Quote.format(p.getValue()));
            writer.print("\t");
            writer.print(Values.Quote.format(p.getLow()));
            writer.print("\t");
            writer.println(Values.Quote.format(p.getHigh()));
        }
    }
}
