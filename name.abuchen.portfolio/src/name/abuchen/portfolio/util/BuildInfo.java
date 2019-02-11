package name.abuchen.portfolio.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

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
            return LocalDateTime.parse(bundle.getString("build.timestamp")); //$NON-NLS-1$
        }
        catch (MissingResourceException | DateTimeParseException e)
        {
            return LocalDateTime.now();
        }
    }

}
