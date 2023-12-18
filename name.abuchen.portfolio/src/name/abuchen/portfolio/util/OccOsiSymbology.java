package name.abuchen.portfolio.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Creates symbols for easier handling of option contracts based on Options
 * Symbology Initiative (OSI) of Options Clearing Corporation (OCC)
 */
public class OccOsiSymbology
{
    private String symbol;
    private Date expiration;
    private OptionType type = OptionType.CALL;
    private Double strike;

    private enum OptionType
    {
        CALL, PUT
    };

    /**
     * Set expiration date of option contract.
     * 
     * @param dateString
     *            date in pattern M/d/y
     */
    public void setExpiration(final String dateString)
    {
        final SimpleDateFormat sdf = new SimpleDateFormat("M/d/y", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("America/Chicago"));
        try
        {
            expiration = sdf.parse(dateString);
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * @param symbol
     *            ticker symbol of underlying security
     * @param expiration
     *            date of expiration in pattern M/d/y
     * @param optionType
     *            PUT or CALL
     * @param s
     *            strike price
     */
    public OccOsiSymbology(final String symbol, final String expiration, final String optionType, final Double s)
    {
        super();
        this.symbol = symbol;
        if (optionType.trim().equals("PUT"))
            type = OptionType.PUT;
        strike = s;
        setExpiration(expiration);
    }

    public String getOccKey()
    {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("America/Chicago"));
        return symbol + sdf.format(expiration) + (type == OptionType.PUT ? "P" : "C")
                        + String.format(Locale.US, "%08.0f", strike * 1000.);
    }

    public String getName()
    {
        final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("America/Chicago"));
        return symbol + " " + sdf.format(expiration) + " " + String.format(Locale.US, "%.3f", strike)
                        + (type == OptionType.PUT ? " Put" : " Call");
    }
}
