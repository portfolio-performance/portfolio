package name.abuchen.portfolio.rest.spi;

/** What an online price update fetches, see {@link HostApplication#startPriceUpdate} */
public enum PriceUpdateTarget
{
    /** the latest quote */
    LATEST,
    /** the historical quotes */
    HISTORIC
}
