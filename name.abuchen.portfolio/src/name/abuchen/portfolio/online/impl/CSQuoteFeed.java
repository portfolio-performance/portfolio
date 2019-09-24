/**
 * 
 */
package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.util.WebAccess;

/**
 * This class provides a feed for Credit Suisse Quotes. Probably all quotes
 * provided by Credit Suisse can be downloaded but it is only tested with Credit
 * Suisse institutional funds which are normally not offered to the general
 * public except in special scenarios like via the Swiss third pillar provider
 * VIAC (third pillar = "Säule 3a", a tax-exempt retirement saving scheme). The
 * challenge of downloading these quotes are: 1) there is a HTML page where the
 * user has to state his/her country of residence plus investor status (private,
 * professional). This page can be avoided by using "curl" as user agent. -
 * there are header titles that are specific to Credit Suisse and 2) Credit
 * Suisse returns the quotes with Excel mime-type which are in fact HTML tables
 * but would be converted upon opening Excel. This is a little bit of a hack on
 * CS' part.
 * 
 * For testing ==>
 * https://amfunds.credit-suisse.com/ch/de/institutional/fund/history/CH0209106761?currency=USD
 */
public class CSQuoteFeed extends HTMLTableQuoteFeed
{
    public static final String ID = "CREDITSUISSE_HTML_TABLE"; //$NON-NLS-1$
    public static final String USERAGENT = "curl/7.58.0"; //$NON-NLS-1$

    protected static class CSDateColumn extends DateColumn
    {
        public CSDateColumn()
        {
            super(new String[] { "NAV Date" }); //$NON-NLS-1$
        }
    }

    protected static class CSCloseColumn extends CloseColumn
    {
        public CSCloseColumn()
        {
            super(new String[] { "NAV" }); //$NON-NLS-1$
        }
    }

    private static final Column[] COLUMNS = new Column[] { new CSDateColumn(), new CSCloseColumn() };

    public CSQuoteFeed()
    {
        // EMPTY
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return Messages.LabelCreditSuisseHTMLTable;
    }

    @Override
    protected Column[] getColumns()
    {
        return COLUMNS;
    }

    protected String getUserAgent()
    {
        return USERAGENT;
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
                new CSQuoteFeed().doLoad(arg, writer);
        writer.flush();
    }

    @Override
    protected List<LatestSecurityPrice> parseFromURL(String url, List<Exception> errors)
    {
        try
        {
            Document document = Jsoup.parse(new WebAccess(url) //
                            .addUserAgent(getUserAgent())
                            .ignoreContentType(true)
                            .get());
            return parse(url, document, errors);
        }
        catch (IOException e)
        {
            errors.add(new IOException(url + '\n' + e.getMessage(), e));
            return Collections.emptyList();
        }
    }
}
