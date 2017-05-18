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
     * Update the latest events of the given securities.
     * 
     * @param securities
     *            the securities to be updated with the latest quote.
     * @param errors
     *            any errors that occur during the update of the quotes are
     *            added to this list.
     * @return true if at least one quote was updated.
     */
    abstract public boolean update(List<Security> securities, List<Exception> errors);

    /**
     * Update the latest events of the given securities.
     * 
     * @param securities
     *            the securities to be updated with the latest quote.
     * @param errors
     *            any errors that occur during the update of the quotes are
     *            added to this list.
     * @return true if at least one quote was updated.
     */
    abstract public boolean update(Security security, List<Exception> errors);

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
