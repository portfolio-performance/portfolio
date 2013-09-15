package name.abuchen.portfolio.online;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import name.abuchen.portfolio.model.LatestSecurityPrice;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ArivaHistQuotesSoup
{
    public List<LatestSecurityPrice> extractFromURL(String url) throws Exception
    {
        return extract(Jsoup.connect(url).get());
    }

    public List<LatestSecurityPrice> extractFromString(String html) throws IOException
    {
        return extract(Jsoup.parse(html));
    }

    public List<LatestSecurityPrice> extract(Document doc) throws IOException
    {
        List<LatestSecurityPrice> result = new ArrayList<LatestSecurityPrice>();
        Elements quoteTable = doc.select("table.line");
        for (Element line : quoteTable.get(0).select("tr.arrow0"))
        {
            Elements cells = line.select("td");
            String dateString = cells.get(0).text();
            Date date = parseDate(dateString);
            int volume = parseVolume(cells.get(7).text());
            long closePrice = parsePrice(cells.get(4).text());
            LatestSecurityPrice temp = new LatestSecurityPrice(date, closePrice);
            temp.setVolume(volume);
            temp.setHigh(parsePrice(cells.get(2).text()));
            temp.setLow(parsePrice(cells.get(3).text()));
            temp.setPreviousClose(parsePrice(cells.get(1).text()));
            result.add(temp);
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
            SimpleDateFormat fmtTradeDate = new SimpleDateFormat("dd.MM.yy"); //$NON-NLS-1$
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

        if (text.indexOf("T") > -1)
        {
            text = text.substring(0, text.indexOf("T") - 1) + "000";
        }
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

}
