package name.abuchen.portfolio.online;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;

import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.SecurityElement;
import name.abuchen.portfolio.model.Security;

public abstract class Feed
{
    public static String ID = "FEED"; //$NON-NLS-1$    
    
    public static final String MANUAL = "MANUAL"; //$NON-NLS-1$
    public static final String YAHOO = "YAHOO"; //$NON-NLS-1$
    public static final String HTML = "GENERIC_HTML_TABLE"; //$NON-NLS-1$

    /**
     * Returns the technical identifier of the feed.
     */
    public String getId()
    {
        return ID;
    }
    

    /**
     * Returns the display name of the feed.
     */
    abstract public String getName();

    /**
     * Update the latest data of the given security.
     * 
     * @param security
     *            the securities to be updated with the latest quote.
     * @param errors
     *            any errors that occur during the update of the quotes are
     *            added to this list.
     * @return true if at least one quote was updated.
     */
    abstract public boolean updateLatest(Security security, List<Exception> errors);

    abstract public boolean updateHistorical(Security security, List<Exception> errors);
    
    abstract public List<SecurityElement> get(Security security, LocalDate start, List<Exception> errors);

    abstract public List<SecurityElement> get(String response, List<Exception> errors);

    abstract public List<Exchange> getExchanges(Security subject, List<Exception> errors);

    /**
     * Test method to parse HTML tables
     * 
     * @param args
     *            list of URLs and/or local files
     */
    public void main(String[] args) throws IOException
    {
        PrintWriter writer = new PrintWriter(System.out); // NOSONAR
        for (String arg : args)
            if (arg.charAt(0) != '#')
                doLoad(arg, writer);
        writer.flush();
    }

    abstract protected void doLoad(String source, PrintWriter writer) throws IOException;
    
}