package name.abuchen.portfolio.online;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import name.abuchen.portfolio.model.LatestSecurityPrice;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ImportFinanzenNetQuotes
{

    private boolean insideDiv = false;
    private boolean insideContentDiv = false;
    private boolean insideTable = false;
    private boolean insideRow = false;
    private boolean insideColumn = false;
    private int columnIndex = -1;

    private void div(Tag tag) throws IOException

    @SuppressWarnings("nls")
    public List<LatestSecurityPrice> extract(String htmlSource) throws IOException
    {
        Document doc = Jsoup.parse(htmlSource);

        Elements tableDiv = doc.select("div");
        Element contentDiv = null;
        for (Element el : tableDiv)
        {
            if ("content_box table_quotes".equals(tag.getAttribute("CLASS"))) {//$NON-NLS-1$ //$NON-NLS-2$
                insideDiv = true;
                return;
            }
            if (insideDiv && "content".equals(tag.getAttribute("CLASS")))
            {
                insideContentDiv = true;
                return;
            }
        }
        else if (insideContentDiv)
        {
            insideContentDiv = insideDiv = insideTable = insideRow = insideColumn = false;
            columnIndex = -1;
        }

    }

    private void table(Tag tag) throws IOException
    {
        if (insideContentDiv && !tag.isEndTag())
            insideTable = true;
    }
            if (el.className().startsWith("content_box table_quotes"))
            {
                contentDiv = el;
                break;
            }
        }
        if (contentDiv == null)
            throw new IOException();

        Map<String, Integer> indices = new HashMap<String, Integer>();
        String[] keys = new String[] { "Schluss", "Eröffnung", "Tageshoch", "Tagestief", "Volumen", "Datum" };

        List<LatestSecurityPrice> result = new ArrayList<LatestSecurityPrice>();

        boolean header = true;
        for (Element tr : contentDiv.select("div.content").select("table").select("tr"))
        {
            // use the first line to sniff the header of the table
            if (header)
            {
                Elements trs = tr.select("th");
                for (Element th : trs)
                {
                    Element compare = th;
                    // time is inside another tag
                    if (th.children().size() == 1)
                        compare = th.child(0);

                    for (String key : keys)
                    {
                        if (compare.ownText().startsWith(key))
                        {
                            indices.put(key, trs.indexOf(th));
                            break;
                        }
                    }
                }
                header = false;
                continue;
            }

            Elements tds = tr.select("td");
            // the last line has only one column, skip that
            if (tds.size() == 1)
                continue;

            // time and value are mandatory
            if (!indices.containsKey(keys[0]) || !indices.containsKey(keys[5]))
                continue;

            LatestSecurityPrice current = new LatestSecurityPrice();

            current.setTime(parseDate(tds.get(indices.get(keys[5])).ownText()));
            current.setValue(parsePrice(tds.get(indices.get(keys[0])).ownText()));

            if (indices.containsKey(keys[2]))
                current.setHigh(parsePrice(tds.get(indices.get(keys[2])).ownText()));

            if (indices.containsKey(keys[3]))
                current.setLow(parsePrice(tds.get(indices.get(keys[3])).ownText()));

            if (indices.containsKey(keys[1]))
                current.setPreviousClose(parsePrice(tds.get(indices.get(keys[1])).ownText()));

            if (indices.containsKey(keys[4]))
                current.setVolume(parseVolume(tds.get(indices.get(keys[4])).ownText()));

            result.add(current);
        }

        return result;
    }

    private long parsePrice(String text) throws IOException
    {
        try
        {
            DecimalFormat fmt = new DecimalFormat("#,##0.###", new DecimalFormatSymbols(Locale.GERMANY)); //$NON-NLS-1$
            Number q = fmt.parse(text);
            return (long) (q.doubleValue() * 100);
        }
        catch (ParseException e)
        {
            throw new IOException(e);
        }
    }

    private Date parseDate(String text) throws IOException
    {
        try
        {
            SimpleDateFormat fmtTradeDate = new SimpleDateFormat("dd.MM.yyyy"); //$NON-NLS-1$
            return fmtTradeDate.parse(text);
        }
        catch (ParseException e)
        {
            throw new IOException(e);
        }
    }

    private int parseVolume(String text) throws IOException
    {
        if (text == null || text.trim().length() == 0)
            return 0;

        try
        {
            DecimalFormat fmt = new DecimalFormat("#,##0", new DecimalFormatSymbols(Locale.GERMANY)); //$NON-NLS-1$
            Number q = fmt.parse(text);
            return q.intValue();
        }
        catch (ParseException e)
        {
            throw new IOException(e);
        }
    }

    public List<LatestSecurityPrice> extract(String htmlSource) throws IOException
    {
        if (htmlSource.indexOf("finanzen.net") < 0) //$NON-NLS-1$
            return Collections.emptyList();

        this.items = new ArrayList<LatestSecurityPrice>();
        this.item = null;

        insideContentDiv = insideDiv = insideTable = insideRow = insideColumn = false;
        columnIndex = -1;

        try
        {
            Lexer lexer = new Lexer(htmlSource);

            Node node = lexer.nextNode();
            while (node != null)
            {
                if (node instanceof Tag)
                {
                    Tag tag = (Tag) node;
                    String tagName = tag.getTagName();

                    if ("DIV".equals(tagName)) //$NON-NLS-1$
                        div(tag);
                    else if ("TABLE".equals(tagName)) //$NON-NLS-1$
                        table(tag);
                    else if ("TR".equals(tagName)) //$NON-NLS-1$
                        tr(tag);
                    else if ("TD".equals(tagName)) //$NON-NLS-1$
                        td(tag);
                }
                else if (node instanceof Text)
                {
                    text((Text) node);
                }
                node = lexer.nextNode();
            }
            return this.items;
        }
        catch (ParserException e)
        {
            throw new IOException(e);
        }
    }

}
