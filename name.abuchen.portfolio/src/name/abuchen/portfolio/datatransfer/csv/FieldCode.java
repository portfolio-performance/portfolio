package name.abuchen.portfolio.datatransfer.csv;

/**
 * Stable, non-localized identifiers ("codes") of the CSV importer fields. These
 * are persisted in a saved {@link CSVConfig} and users rely on them, so a value
 * here must never change. They are the key of the field-to-column map and are
 * shared across the extractors and the shared helpers in {@link BaseCSVExtractor},
 * which is why they live in one place rather than being repeated as literals.
 */
/* package */ final class FieldCode
{
    private FieldCode()
    {
    }

    public static final String ISIN = "isin";
    public static final String WKN = "wkn";
    public static final String TICKER = "ticker";
    public static final String NAME = "name";
    public static final String CURRENCY = "currency";
    public static final String CURRENCY_GROSS = "currencyGross";
    public static final String NOTE = "note";
    public static final String DATE = "date";
    public static final String DATE_QUOTE = "date-quote";
    public static final String QUOTE = "quote";
    public static final String TIME = "time";
    public static final String TYPE = "type";
    public static final String VALUE = "value";
    public static final String SHARES = "shares";
    public static final String TAXES = "taxes";
    public static final String FEES = "fees";
    public static final String GROSS = "gross";
    public static final String EXCHANGE_RATE = "exchangeRate";
    public static final String ACCOUNT = "account";
    public static final String ACCOUNT_2ND = "account2nd";
    public static final String PORTFOLIO = "portfolio";
    public static final String PORTFOLIO_2ND = "portfolio2nd";
}
