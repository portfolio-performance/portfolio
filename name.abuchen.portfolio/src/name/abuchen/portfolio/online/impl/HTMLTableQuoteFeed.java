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
import java.time.LocalTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.UncheckedIOException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.impl.variableurl.Factory;
import name.abuchen.portfolio.online.impl.variableurl.urls.VariableURL;
import name.abuchen.portfolio.util.OnlineHelper;
import name.abuchen.portfolio.util.Pair;
import name.abuchen.portfolio.util.TextUtil;
import name.abuchen.portfolio.util.WebAccess;

public class HTMLTableQuoteFeed implements QuoteFeed
{
    private static class ExtractedPrice extends LatestSecurityPrice
    {
        private LocalTime time;

        public LocalTime getTime()
        {
            return time;
        }

        public void setTime(LocalTime time)
        {
            this.time = time;
        }

        public LatestSecurityPrice toLatestSecurityPrice()
        {
            return new LatestSecurityPrice(getDate(), getValue(), getHigh(), getLow(), getVolume());
        }

        @Override
        public boolean equals(Object obj)
        {
            if (!super.equals(obj))
                return false;

            ExtractedPrice other = (ExtractedPrice) obj;
            return Objects.equals(this.time, other.time);
        }

        @Override
        public int hashCode()
        {
            return 31 * super.hashCode() + Objects.hash(time);
        }
    }

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

        abstract void setValue(Element value, ExtractedPrice price, String languageHint) throws ParseException;

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
                            // https://stackoverflow.com/a/29496149/1158146
                            new DateTimeFormatterBuilder().appendPattern("d.M.")
                                            .appendValueReduced(ChronoField.YEAR, 2, 2, Year.now().getValue() - 80)
                                            .toFormatter(),
                            new DateTimeFormatterBuilder().appendPattern("M/d/")
                                            .appendValueReduced(ChronoField.YEAR, 2, 2, Year.now().getValue() - 80)
                                            .toFormatter(),
                            DateTimeFormatter.ofPattern("d.M.y"), //$NON-NLS-1$
                            DateTimeFormatter.ofPattern("d. MMM y"), //$NON-NLS-1$
                            DateTimeFormatter.ofPattern("d. MMMM y"), //$NON-NLS-1$
                            DateTimeFormatter.ofPattern("d. MMM. y"), //$NON-NLS-1$
                            DateTimeFormatter.ofPattern("MMM d, y", Locale.ENGLISH), //$NON-NLS-1$
                            DateTimeFormatter.ofPattern("MMM dd, y", Locale.ENGLISH), //$NON-NLS-1$
                            DateTimeFormatter.ofPattern("MMM dd y", Locale.ENGLISH), //$NON-NLS-1$
                            DateTimeFormatter.ofPattern("d MMM y", Locale.ENGLISH), //$NON-NLS-1$
                            DateTimeFormatter.ofPattern("EEEE, MMMM dd, yEEE, MMM dd, y", Locale.ENGLISH) //$NON-NLS-1$
            };
        }

        @Override
        void setValue(Element value, ExtractedPrice price, String languageHint) throws ParseException
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

    protected static class TimeColumn extends Column
    {
        private DateTimeFormatter[] formatters = new DateTimeFormatter[] { DateTimeFormatter.ISO_LOCAL_TIME };

        @SuppressWarnings("nls")
        public TimeColumn()
        {
            super(new String[] { "Zeit.*" });
        }

        @Override
        void setValue(Element value, ExtractedPrice price, String languageHint) throws ParseException
        {
            String text = TextUtil.strip(value.text());
            for (DateTimeFormatter formatter : formatters)
            {
                try
                {
                    LocalTime time = LocalTime.parse(text, formatter);
                    price.setTime(time);
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
                            "akt. Kurs", "Dernier", "Kurs" });
        }

        public CloseColumn(String[] patterns)
        {
            super(patterns);
        }

        @Override
        void setValue(Element value, ExtractedPrice price, String languageHint) throws ParseException
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
        void setValue(Element value, ExtractedPrice price, String languageHint) throws ParseException
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
        void setValue(Element value, ExtractedPrice price, String languageHint) throws ParseException
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

    private static final Column[] COLUMNS = new Column[] { new DateColumn(), new TimeColumn(), new CloseColumn(),
                    new HighColumn(), new LowColumn() };

    private final PageCache<Pair<String, List<LatestSecurityPrice>>> cache = new PageCache<>();

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
    public Optional<LatestSecurityPrice> getLatestQuote(Security security)
    {
        // if latestFeed is null, then the policy is 'use same configuration
        // as historic quotes'
        String feedURL = security.getLatestFeed() == null ? security.getFeedURL() : security.getLatestFeedURL();

        QuoteFeedData data = internalGetQuotes(security, feedURL, false, false);

        if (!data.getErrors().isEmpty())
            PortfolioLog.error(data.getErrors());

        List<LatestSecurityPrice> prices = data.getLatestPrices();
        if (prices.isEmpty())
            return Optional.empty();

        Collections.sort(prices, new SecurityPrice.ByDate());

        return Optional.of(prices.get(prices.size() - 1));
    }

    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse)
    {
        return internalGetQuotes(security, security.getFeedURL(), collectRawResponse, false);
    }

    public QuoteFeedData getHistoricalQuotes(String html)
    {
        QuoteFeedData data = new QuoteFeedData();
        data.addAllPrices(parseFromHTML(html, data));
        return data;
    }

    @Override
    public QuoteFeedData previewHistoricalQuotes(Security security)
    {
        return internalGetQuotes(security, security.getFeedURL(), true, true);
    }

    private QuoteFeedData internalGetQuotes(Security security, String feedURL, boolean collectRawResponse,
                    boolean isPreview)
    {
        if (feedURL == null || feedURL.length() == 0)
        {
            return QuoteFeedData.withError(
                            new IOException(MessageFormat.format(Messages.MsgMissingFeedURL, security.getName())));
        }

        QuoteFeedData data = new QuoteFeedData();

        VariableURL variableURL = Factory.fromString(feedURL);
        variableURL.setSecurity(security);

        SortedSet<LatestSecurityPrice> newPricesByDate = new TreeSet<>(new SecurityPrice.ByDate());
        long failedAttempts = 0;
        long maxFailedAttempts = variableURL.getMaxFailedAttempts();

        for (String url : variableURL) // NOSONAR
        {
            Pair<String, List<LatestSecurityPrice>> answer = cache.lookup(url);

            if (answer == null || (collectRawResponse && answer.getLeft().isEmpty()))
            {
                answer = parseFromURL(url, collectRawResponse, data);

                if (!answer.getRight().isEmpty())
                    cache.put(url, answer);
            }

            if (collectRawResponse)
                data.addResponse(url, answer.getLeft());

            int sizeBefore = newPricesByDate.size();
            newPricesByDate.addAll(answer.getRight());

            if (newPricesByDate.size() > sizeBefore)
                failedAttempts = 0;
            else if (++failedAttempts > maxFailedAttempts)
                break;

            if (isPreview && newPricesByDate.size() >= 100)
                break;
        }

        data.addAllPrices(newPricesByDate);

        return data;
    }

    protected String getUserAgent()
    {
        return OnlineHelper.getUserAgent();
    }

    protected Pair<String, List<LatestSecurityPrice>> parseFromURL(String url, boolean collectRawResponse,
                    QuoteFeedData data)
    {
        try
        {
            String html = getHtml(url);

            Document document = Jsoup.parse(html);
            List<LatestSecurityPrice> prices = parse(url, document, data);

            return new Pair<>(collectRawResponse ? html : "", prices); //$NON-NLS-1$
        }
        catch (URISyntaxException | IOException | UncheckedIOException e)
        {
            data.addError(new IOException(url + '\n' + e.getMessage(), e));
            return new Pair<>(String.valueOf(e.getMessage()), Collections.emptyList());
        }
    }

    String getHtml(String url) throws IOException, URISyntaxException
    {
        return new WebAccess(url) //
                        .addUserAgent(getUserAgent()) //
                        .get();
    }

    protected List<LatestSecurityPrice> parseFromHTML(String html, QuoteFeedData data)
    {
        return parse("n/a", Jsoup.parse(html), data); //$NON-NLS-1$
    }

    private List<LatestSecurityPrice> parse(String url, Document document, QuoteFeedData data)
    {
        // check if language is provided
        String language = document.select("html").attr("lang"); //$NON-NLS-1$ //$NON-NLS-2$

        List<ExtractedPrice> prices = new ArrayList<>();

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
                            ExtractedPrice price = extractPrice(row, specs, language, headerInfo.numberOfHeaderColumns);
                            if (price != null)
                                prices.add(price);
                        }
                        catch (Exception e)
                        {
                            data.addError(new IOException(url + '\n' + e.getMessage(), e));
                        }
                    }

                    // skip all other tables
                    break;
                }
            }
        }

        // if no quotes could be extracted, log HTML for further analysis
        if (prices.isEmpty())
        {
            data.addError(new IOException(MessageFormat.format(Messages.MsgNoQuotesFoundInHTML, url,
                            Jsoup.clean(document.html(), Whitelist.relaxed()))));
            return Collections.emptyList();
        }

        // sort by date and, if available, by time

        Collections.sort(prices, (r, l) -> {
            int compare = l.getDate().compareTo(r.getDate());
            if (compare != 0)
                return compare;

            if (r.getTime() == null || l.getTime() == null)
                return 0;

            return l.getTime().compareTo(r.getTime());
        });

        // remove the duplicates with the same date (keep the one with the
        // oldest time)

        List<LatestSecurityPrice> answer = new ArrayList<>(prices.size());

        LocalDate last = null;
        for (ExtractedPrice p : prices)
        {
            if (p.getDate().equals(last))
                continue;

            answer.add(p.toLatestSecurityPrice());

            last = p.getDate();
        }

        return answer;
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
            if (element.hasAttr("colspan")) //$NON-NLS-1$
            {
                int colspan = Integer.valueOf(element.attr("colspan")); //$NON-NLS-1$
                // remove attribute
                element.removeAttr("colspan"); //$NON-NLS-1$

                // add copies of this column to the header to align header with
                // columns in table
                for (int c = 1; c < colspan; c++)
                {
                    row.add(ii, element);
                }
            }

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
            hasDate = hasDate || spec.column instanceof DateColumn || spec.column instanceof TimeColumn;
            hasClose = hasClose || spec.column instanceof CloseColumn;
        }

        return hasDate && hasClose;
    }

    private ExtractedPrice extractPrice(Element row, List<Spec> specs, String languageHint, int numberOfHeaderColumns)
                    throws ParseException
    {
        Elements cells = row.select("> td"); //$NON-NLS-1$

        // we're only looking at rows having the same size as the header row
        if (cells.size() != numberOfHeaderColumns)
            return null;

        ExtractedPrice price = new ExtractedPrice();

        for (Spec spec : specs)
            spec.column.setValue(cells.get(spec.index), price, languageHint);

        if (price.getDate() == null)
            price.setDate(LocalDate.now());

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

        Pair<String, List<LatestSecurityPrice>> result;
        QuoteFeedData data = new QuoteFeedData();

        if (source.startsWith("http"))
        {
            result = parseFromURL(source, false, data);
        }
        else
        {
            try (Scanner scanner = new Scanner(new File(source), StandardCharsets.UTF_8.name()))
            {
                String html = scanner.useDelimiter("\\A").next();
                result = new Pair<>(html, new HTMLTableQuoteFeed().parseFromHTML(html, data));
            }
        }

        for (Exception error : data.getErrors())
            error.printStackTrace(writer); // NOSONAR

        for (LatestSecurityPrice p : result.getRight())
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
