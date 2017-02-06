package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.SecurityElement;

abstract class HTMLTableParser
{
 
    protected static class Spec
    {
        private final Column column;
        private final int index;

        public Spec(Column column, int index)
        {
            this.column = column;
            this.index = index;
        }
        
        public Column getColumn()
        {
            return column;
        }
    }

    public HTMLTableParser()
    {        
    }
    
    public abstract Object newRowObject();
    
    protected Column[] COLUMNS = new Column[] {}; 
    
    @SuppressWarnings("nls")
    protected List<Object> _parseFromURL(String url, List<Exception> errors)
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

    protected List<Object> _parseFromHTML(String html, List<Exception> errors)
    {
        return parse(Jsoup.parse(html), errors);
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

    private List<Object> parse(Document document, List<Exception> errors)
    {
        // check if language is provided
        String language = document.select("html").attr("lang"); //$NON-NLS-1$ //$NON-NLS-2$

        List<Object> elementList = new ArrayList<>();

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
                        Object element = extractData(row, specs, language);
                        if (element != null)
                            elementList.add(element);
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
        if (elementList.isEmpty())
            errors.add(new IOException(MessageFormat.format(Messages.MsgNoQuotesFoundInHTML, document.html())));
        return elementList;
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

    protected boolean isSpecValid(List<Spec> specs)
    {
        return false;
    }
    
    private Object extractData(Element row, List<Spec> specs, String languageHint) throws ParseException
    {
        Elements cells = row.select("> td"); //$NON-NLS-1$

        // row can be empty if it contains only 'th' elements
        if (cells.size() <= 1)
            return null;

        Object obj = newRowObject();

        for (Spec spec : specs)
            spec.column.setValue(cells.get(spec.index), obj, languageHint);
        
        return obj;
    }

    
}