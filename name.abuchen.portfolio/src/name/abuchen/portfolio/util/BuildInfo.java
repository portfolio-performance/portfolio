package name.abuchen.portfolio.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import name.abuchen.portfolio.PortfolioLog;

public enum BuildInfo
{
    INSTANCE;

    private final LocalDateTime buildTime;

    private BuildInfo()
    {
        this.buildTime = readBuildTime();
    }

    public LocalDateTime getBuildTime()
    {
        return buildTime;
    }

    private LocalDateTime readBuildTime()
    {
        try
        {
            // timestamp is written into build-info.properties by Maven
            ResourceBundle bundle = ResourceBundle.getBundle("build-info"); //$NON-NLS-1$
            return Instant.parse(bundle.getString("build.timestamp")).atZone(ZoneId.systemDefault()).toLocalDateTime(); //$NON-NLS-1$
        }
        catch (MissingResourceException | DateTimeParseException e)
        {
            PortfolioLog.error(e);
            return LocalDateTime.now();
        }
    }

}
