package name.abuchen.portfolio.build.rbe;

public final class BundleEntry
{
    private final String key;
    private final String value;
    private final String comment;
    private final boolean commented;

    public BundleEntry(String key, String value, String comment)
    {
        this(key, value, comment, false);
    }

    public BundleEntry(String key, String value, String comment, boolean commented)
    {
        this.key = key == null ? "" : key; //$NON-NLS-1$
        this.value = value == null ? "" : value; //$NON-NLS-1$
        this.comment = comment;
        this.commented = commented;
    }

    public String getComment()
    {
        return comment;
    }

    public String getKey()
    {
        return key;
    }

    public String getValue()
    {
        return value;
    }

    public boolean isCommented()
    {
        return commented;
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof BundleEntry entry))
            return false;

        if (!key.equals(entry.key) || commented != entry.commented || !value.equals(entry.value))
            return false;

        if (comment == null)
            return entry.comment == null;

        return comment.equals(entry.comment);
    }

    @Override
    public int hashCode()
    {
        int result = key.hashCode();
        result = 31 * result + value.hashCode();
        result = 31 * result + (comment == null ? 0 : comment.hashCode());
        result = 31 * result + Boolean.hashCode(commented);
        return result;
    }
}
