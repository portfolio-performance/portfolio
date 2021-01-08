package name.abuchen.portfolio.online;

public enum TaxonomySource
{
    ETF_DATA_COUNTRY_ALLOCATION("etf-data.com$country-allocation", "Country Allocation (etf-data.com)"), //$NON-NLS-1$ //$NON-NLS-2$
    ETF_DATA_SECTOR_ALLOCATION("etf-data.com$sector-allocation", "Sector Allocation (etf-data.com)"); //$NON-NLS-1$ //$NON-NLS-2$

    private final String identifier;
    private final String label;

    private TaxonomySource(String identifier, String label)
    {
        this.identifier = identifier;
        this.label = label;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public String getLabel()
    {
        return label;
    }
}