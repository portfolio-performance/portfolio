package name.abuchen.portfolio.ui.editor;

import name.abuchen.portfolio.ui.Messages;

public enum DomainElement
{
    INVESTMENT_VEHICLE(Messages.ColumnSecurity, Messages.SecurityMenuNewSecurity), //
    CRYPTO_CURRENCY(name.abuchen.portfolio.Messages.LabelCryptocurrency, Messages.SecurityMenuNewCryptocurrency), //
    EXCHANGE_RATE(Messages.ColumnExchangeRate, Messages.SecurityMenuNewExchangeRate), //
    CONSUMER_PRICE_INDEX(Messages.PerformanceChartLabelCPI, Messages.SecurityMenuNewHICP), //
    TAXONOMY(Messages.ColumnTaxonomy, Messages.LabelNewTaxonomy + "..."), //$NON-NLS-1$
    WATCHLIST(Messages.LabelWatchlist, Messages.WatchlistNewLabel + "..."); //$NON-NLS-1$

    private final String menuLabel;
    private final String paletteLabel;

    private DomainElement(String menuLabel, String paletteLabel)
    {
        this.menuLabel = menuLabel;
        this.paletteLabel = paletteLabel;
    }

    public String getMenuLabel()
    {
        return menuLabel;
    }

    public String getPaletteLabel()
    {
        return paletteLabel;
    }
}
