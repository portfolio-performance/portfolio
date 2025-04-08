package name.abuchen.portfolio.datatransfer;

public class DuplicateSecurityException extends IllegalArgumentException
{
    private static final long serialVersionUID = 1L;

    public DuplicateSecurityException(String s)
    {
        super(s);
    }
}
