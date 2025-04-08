package name.abuchen.portfolio.model;

/**
 * Helper class to access properties stored in the client in a "typed" way.
 */
public class ClientProperties
{
    public interface Keys
    {
        String RISK_FREE_RATE_OF_RETURN = "risk-free-rate-of-return"; //$NON-NLS-1$
        String SECURITY_NAME_CONFIG = "security-name-config"; //$NON-NLS-1$
    }

    private final Client client;

    public ClientProperties(Client client)
    {
        this.client = client;
    }

    /**
     * Returns the risk-free rate of return used in the calculation of the
     * sharpe ratio.
     */
    public double getRiskFreeRateOfReturn()
    {
        try
        {
            var v = client.getProperty(Keys.RISK_FREE_RATE_OF_RETURN);
            return v == null ? 0 : Double.parseDouble(v);
        }
        catch (NumberFormatException e)
        {
            return 0;
        }
    }

    /**
     * Sets the risk-free rate of return used in the calculation of the sharpe
     * ratio.
     */
    public void setRiskFreeRateOfReturn(double riskFreeRateOfReturn)
    {
        client.setProperty(Keys.RISK_FREE_RATE_OF_RETURN, Double.toString(riskFreeRateOfReturn));
    }

    public SecurityNameConfig getSecurityNameConfig()
    {
        var v = client.getProperty(Keys.SECURITY_NAME_CONFIG);
        if (v == null)
            return SecurityNameConfig.NONE;

        try
        {
            return SecurityNameConfig.valueOf(v);
        }
        catch (IllegalArgumentException e)
        {
            return SecurityNameConfig.NONE;
        }
    }

    public void setSecurityNameConfig(SecurityNameConfig config)
    {
        client.setProperty(Keys.SECURITY_NAME_CONFIG, config.name());
    }
}
