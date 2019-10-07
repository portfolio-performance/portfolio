package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.online.SplitHTMLWrap;
import name.abuchen.portfolio.util.WebAccess;

public class FileTSVQuoteFeed extends HTMLTableQuoteFeed
{
    public static final String ID = "TSV_FILE_TABLE"; //$NON-NLS-1$

    public static final char splitChar = '\t';

    public FileTSVQuoteFeed()
    {
        // EMPTY
    }

    @Override
    public String getName()
    {
        return Messages.LabelTSVFile;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    protected List<LatestSecurityPrice> parseFromURL(String url, List<Exception> errors)
    {
        try
        {
            Document document = Jsoup.parse(SplitHTMLWrap.getSplitHTML(new WebAccess(url).get(), splitChar));
            return parse(url, document, errors);
        }
        catch (URISyntaxException | IOException e)
        {
            errors.add(new IOException(url + '\n' + e.getMessage(), e));
            return Collections.emptyList();
        }
    }
}
