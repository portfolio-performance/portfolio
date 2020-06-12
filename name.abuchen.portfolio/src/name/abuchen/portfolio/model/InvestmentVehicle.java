package name.abuchen.portfolio.model;

public interface InvestmentVehicle extends Named
{
    String getUUID();

    String getCurrencyCode();

    void setCurrencyCode(String currencyCode);
    
    boolean isRetired();
    
    void setRetired(boolean isRetired);
}