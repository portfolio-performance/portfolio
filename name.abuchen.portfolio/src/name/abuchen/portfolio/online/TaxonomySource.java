package name.abuchen.portfolio.online;

public enum TaxonomySource
{
    EOD_HISTORICAL_DATA_COUNTRY_ALLOCATION("eodhistoricaldata.com$country-allocation", //$NON-NLS-1$
                    "Country Allocation (eodhistoricaldata.com)"), //$NON-NLS-1$
    EOD_HISTORICAL_DATA_SECTOR_ALLOCATION("eodhistoricaldata.com$sector-allocation", //$NON-NLS-1$
                    "Sector Allocation (eodhistoricaldata.com)"), //$NON-NLS-1$
    
    BINT_EE_COUNTRY_ALLOCATION("bint.ee$country-allocation", //$NON-NLS-1$
                    "Country Allocation (BINT.ee)"), //$NON-NLS-1$
    BINT_EE_SECTOR_ALLOCATION("bint.ee$sector-allocation", //$NON-NLS-1$
                    "Sector Allocation (BINT.ee)"); //$NON-NLS-1$
    

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
