package name.abuchen.portfolio.rest.spi;

import java.io.IOException;

/**
 * The file is encrypted, so opening it requires the user to enter the password
 * in the application.
 */
public class PasswordRequiredException extends IOException
{
    private static final long serialVersionUID = 1L;

    public PasswordRequiredException(String path)
    {
        super(path);
    }
}
