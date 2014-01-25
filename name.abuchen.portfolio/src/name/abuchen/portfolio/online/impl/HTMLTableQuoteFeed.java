package name.abuchen.portfolio.online.impl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.online.QuoteFeed;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HTMLTableQuoteFeed implements QuoteFeed
{
    private abstract static class Column
    {
        private DecimalFormat decimalFormat = new DecimalFormat("#,##0.###", new DecimalFormatSymbols(Locale.GERMAN)); //$NON-NLS-1$
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

        abstract void read(Element value, LatestSecurityPrice price) throws ParseException;

        protected long asQuote(Element value) throws ParseException
        {
            String text = value.text();
            double quote = decimalFormat.parse(text).doubleValue();
            return Math.round(quote * 100);
        }
    }

    private static class DateColumn extends Column
    {
        public DateColumn()
        {
            super(new String[] { "Datum" }); //$NON-NLS-1$
        }

        @Override
        void read(Element value, LatestSecurityPrice price) throws ParseException
        {
            String text = value.text();
            try
            {
                Date date = new SimpleDateFormat("dd.MM.yy").parse(text); //$NON-NLS-1$
                price.setTime(date);
            }
            catch (ParseException e)
            {
                Date date = new SimpleDateFormat("dd. MMM yyyy").parse(text); //$NON-NLS-1$
                price.setTime(date);
            }
        }
    }

    private static class CloseColumn extends Column
    {
        public CloseColumn()
        {
            super(new String[] { "Schluss.*", "Rücknahmepreis.*" }); //$NON-NLS-1$ //$NON-NLS-2$
        }

        @Override
        void read(Element value, LatestSecurityPrice price) throws ParseException
        {
            price.setValue(asQuote(value));
        }
    }

    private static class HighColumn extends Column
    {
        public HighColumn()
        {
            super(new String[] { "Hoch.*", "Tageshoch.*", "Max.*" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }

        @Override
        void read(Element value, LatestSecurityPrice price) throws ParseException
        {
            price.setHigh(asQuote(value));
        }
    }

    private static class LowColumn extends Column
    {
        public LowColumn()
        {
            super(new String[] { "Tief.*", "Tagestief.*" }); //$NON-NLS-1$ //$NON-NLS-2$
        }

        @Override
        void read(Element value, LatestSecurityPrice price) throws ParseException
        {
            price.setLow(asQuote(value));
        }
    }

    private static class Spec
    {
        public Spec(Column column, int index)
        {
            this.column = column;
            this.index = index;
        }

        private final Column column;
        private final int index;
    }

    private final Column[] columns = new Column[] { new DateColumn(), new CloseColumn(), new HighColumn(),
                    new LowColumn() };

    @Override
    public String getId()
    {
        return "GENERIC_HTML_TABLE"; //$NON-NLS-1$
    }

    @Override
    public String getName()
    {
        return Messages.LabelHTMLTable;
    }

    @Override
    public void updateLatestQuotes(List<Security> securities, List<Exception> errors) throws IOException
    {
        for (Security security : securities)
        {
            try
            {
                updateHistoricalQuotes(security, errors);
            }
            catch (IOException e)
            {
                errors.add(e);
            }
        }
    }

    @Override
    public void updateHistoricalQuotes(Security security, List<Exception> errors) throws IOException
    {
        List<LatestSecurityPrice> quotes = getHistoricalQuotes(security, null, errors);

        for (LatestSecurityPrice quote : quotes)
            security.addPrice(new SecurityPrice(quote.getTime(), quote.getValue()));
    }

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(Security security, Date start, List<Exception> errors)
                    throws IOException
    {
        if (security.getFeedURL() == null || security.getFeedURL().length() == 0)
            throw new IOException(MessageFormat.format(Messages.MsgMissingFeedURL, security.getName()));

        return parse(Jsoup.connect(security.getFeedURL()).get(), errors);
    }

    @Override
    public List<Exchange> getExchanges(Security subject, List<Exception> errors) throws IOException
    {
        return null;
    }

    protected List<LatestSecurityPrice> parseFromURL(String url, List<Exception> errors) throws IOException
    {
        return parse(Jsoup.connect(url).get(), errors);
    }

    protected List<LatestSecurityPrice> parseFromHTML(String html, List<Exception> errors) throws IOException
    {
        return parse(Jsoup.parse(html), errors);
    }

    private List<LatestSecurityPrice> parse(Document document, List<Exception> errors) throws IOException
    {
        List<LatestSecurityPrice> prices = new ArrayList<LatestSecurityPrice>();

        // first: find tables
        Elements tables = document.getElementsByTag("table"); //$NON-NLS-1$
        for (Element table : tables)
        {
            List<Spec> specs = new ArrayList<Spec>();

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

        return prices;
    }

    @SuppressWarnings("nls")
    private int buildSpecFromTable(Element table, List<Spec> specs)
    {
        // check if thead exists
        Elements header = table.select("> thead > tr > th");
        if (header.size() > 0)
        {
            buildSpecFromRow(header, specs);
            return 0;
        }

        // check if th exist in body
        header = table.select("> tbody > tr > th");
        if (header.size() > 0)
        {
            buildSpecFromRow(header, specs);
            return 0;
        }

        // then check first two regular rows
        int rowIndex = 0;

        Elements rows = table.select("> tbody > tr");
        if (rows.size() > 0)
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
        Set<Column> available = new HashSet<Column>();
        for (Column column : columns)
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
        if (cells.size() == 0)
            return null;

        LatestSecurityPrice price = new LatestSecurityPrice();

        for (Spec spec : specs)
            spec.column.read(cells.get(spec.index), price);

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
        PrintWriter writer = new PrintWriter(System.out);
        for (String arg : args)
            doLoad(arg, writer);
        writer.flush();
    }

    @SuppressWarnings("nls")
    private static void doLoad(String source, PrintWriter writer) throws IOException
    {
        writer.println("--------");
        writer.println(source);
        writer.println("--------");

        List<LatestSecurityPrice> prices = null;
        List<Exception> errors = new ArrayList<Exception>();

        if (source.startsWith("http"))
        {
            prices = new HTMLTableQuoteFeed().parseFromURL(source, errors);
        }
        else
        {
            String html = new Scanner(new File(source), "UTF-8").useDelimiter("\\A").next();
            prices = new HTMLTableQuoteFeed().parseFromHTML(html, errors);
        }

        for (Exception error : errors)
            error.printStackTrace(writer);

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
