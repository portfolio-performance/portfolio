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
        String DISCREET_MODE = "discreet-mode"; //$NON-NLS-1$
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

    /**
     * Returns true if the file was last saved with the discreet mode (the
     * hidden amounts) activated. Used to reopen the file in the discreet mode.
     */
    public boolean isDiscreetMode()
    {
        return Boolean.parseBoolean(client.getProperty(Keys.DISCREET_MODE));
    }

    /**
     * Returns true if the file explicitly stores a value for the discreet mode,
     * that is if the user has toggled the discreet mode for this file. Older
     * files have no such property: the discreet mode of the running
     * application is kept when opening them.
     */
    public boolean hasDiscreetMode()
    {
        return client.hasProperty(Keys.DISCREET_MODE);
    }

    /**
     * Stores whether the discreet mode is active so that the file is reopened
     * in the very same mode.
     */
    public void setDiscreetMode(boolean discreetMode)
    {
        client.setProperty(Keys.DISCREET_MODE, Boolean.toString(discreetMode));
    }
}
