package name.abuchen.portfolio.online;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.SecurityElement;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.impl.HTMLTableQuoteParser;

public abstract class QuoteFeed extends Feed
{
    public static String ID = "QUOTE"; //$NON-NLS-1$

    abstract public boolean updateLatestQuotes(Security security, List<Exception> errors);

    abstract public boolean updateHistoricalQuotes(Security security, List<Exception> errors);

    abstract public List<LatestSecurityPrice> getHistoricalQuotes(Security security, LocalDate start, List<Exception> errors);

    abstract public List<LatestSecurityPrice> getHistoricalQuotes(String response, List<Exception> errors);

    abstract public List<Exchange> getExchanges(Security subject, List<Exception> errors);

    @Override
    public boolean updateLatest(Security security, List<Exception> errors)
    {
        return updateLatestQuotes(security, errors);
    }

    @Override
    public boolean updateHistorical(Security security, List<Exception> errors)
    {
        return updateHistoricalQuotes(security, errors);
    }

    @Override
    public final List<SecurityElement> get(Security security, LocalDate start, List<Exception> errors)
    {
        return SecurityElement.cast2ElementList(getHistoricalQuotes(security, start, errors));
    }

    public final List<SecurityElement> get(String response, List<Exception> errors)
    {
        return SecurityElement.cast2ElementList(getHistoricalQuotes(response, errors));
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
            prices = new HTMLTableQuoteParser().parseFromURL(source, errors);
        }
        else
        {
            try (Scanner scanner = new Scanner(new File(source), StandardCharsets.UTF_8.name()))
            {
                String html = scanner.useDelimiter("\\A").next();
                prices = new HTMLTableQuoteParser().parseFromHTML(html, errors);
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
