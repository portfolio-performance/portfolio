package name.abuchen.portfolio.tests;

/**
 * Simple DTO for reporting structural validation errors from meta-tests.
 * Kept Java-8+ compatible (no records).
 */
public final class StructureError
{
    private final String file;
    private final int line;
    private final String message;

    public StructureError(String file, int line, String message)
    {
        this.file = file;
        this.line = line;
        this.message = message;
    }

    public String file()
    {
        return file;
    }

    public int line()
    {
        return line;
    }

    public String message()
    {
        return message;
    }
}
