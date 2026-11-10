package name.abuchen.portfolio.snapshot.trades;

public class TradeCollectorException extends Exception
{
    /**
     * Identifies why the collector gave up on a security.
     * <p>
     * The reason is carried separately from the message because callers outside
     * the application need to report the failure in their own words: the message
     * comes from {@code messages.properties} and is localized to the running
     * application, whereas the REST API emits English only and would otherwise
     * change language with the user's Eclipse locale. Consumers switch on this
     * enum instead of comparing the formatted message against the
     * {@code Messages} constants, which would break silently the first time
     * anyone rewords a translation.
     */
    public enum Reason
    {
        NO_HOLDINGS_FOR_SELL, MISSING_HOLDINGS_FOR_SELL, NO_HOLDINGS_FOR_TRANSFER, MISSING_HOLDINGS_FOR_TRANSFER
    }

    private static final long serialVersionUID = 1L;

    private final Reason reason;

    public TradeCollectorException(Reason reason, String message)
    {
        super(message);
        this.reason = reason;
    }

    public Reason getReason()
    {
        return reason;
    }
}
