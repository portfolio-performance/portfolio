package name.abuchen.portfolio.model;

import name.abuchen.portfolio.Messages;

public enum SecurityNameConfig
{
    NONE("-"), //$NON-NLS-1$
    ISIN(Messages.CSVColumn_ISIN), //
    TICKER_SYMBOL(Messages.CSVColumn_TickerSymbol), //
    WKN(Messages.CSVColumn_WKN);

    private final String label;

    private SecurityNameConfig(String label)
    {
        this.label = label;
    }

    public String getLabel()
    {
        return label;
    }
}
