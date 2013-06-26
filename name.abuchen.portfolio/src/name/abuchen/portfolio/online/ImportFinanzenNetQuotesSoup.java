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

public class ImportFinanzenNetQuotesSoup
{

    public List<LatestSecurityPrice> extract(Document doc) throws IOException
    {
        List<LatestSecurityPrice> result = new ArrayList<LatestSecurityPrice>();
        Elements tableDiv = doc.select("div");
        Element contentDiv = null;
        for (Element el : tableDiv)
        {
            if (el.className().startsWith("content_box table_quotes"))
            {
                contentDiv = el;
                break;
            }
        }
        boolean header = true;
        for (Element tr : contentDiv.select("div.content").select("table").select("tr"))
        {
            if (header)
            {
                header = false;
                continue;
            }
            LatestSecurityPrice current = new LatestSecurityPrice();
            current.setHigh(parsePrice(tr.select("td").get(3).ownText()));
            current.setLow(parsePrice(tr.select("td").get(4).ownText()));
            current.setTime(parseDate(tr.select("td").get(0).ownText()));
            current.setValue(parsePrice(tr.select("td").get(2).ownText()));
            current.setPreviousClose(parsePrice(tr.select("td").get(1).ownText()));
            current.setVolume(parseVolume(tr.select("td").get(5).ownText()));
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

    public static void main(String[] args) throws IOException
    {
        System.out.println(new ImportFinanzenNetQuotesSoup().extract(Jsoup.connect(
                        "http://www.finanzen.net/kurse/kurse_historisch.asp?pkAktieNr=938&strBoerse=FSE").get()));
    }

}
