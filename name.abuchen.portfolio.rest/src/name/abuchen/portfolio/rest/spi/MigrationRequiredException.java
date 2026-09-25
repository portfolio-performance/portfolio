package name.abuchen.portfolio.rest.spi;

import java.io.IOException;

/**
 * The file uses a format from before currency support. Migrating it requires
 * the user to choose the base currency in the application.
 */
public class MigrationRequiredException extends IOException
{
    private static final long serialVersionUID = 1L;

    public MigrationRequiredException(String path)
    {
        super(path);
    }
}
