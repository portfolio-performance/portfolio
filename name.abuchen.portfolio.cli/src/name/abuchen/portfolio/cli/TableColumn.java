package name.abuchen.portfolio.cli;

/**
 * One column of a table: which member of a rendered JSON item it shows and
 * under which header. Values longer than <code>maxLength</code> are cut with
 * an ellipsis; {@link #UNBOUNDED} never cuts.
 */
record TableColumn(String key, String header, int maxLength)
{
    static final int UNBOUNDED = 0;

    static TableColumn of(String key, String header)
    {
        return new TableColumn(key, header, UNBOUNDED);
    }

    static TableColumn capped(String key, String header, int maxLength)
    {
        return new TableColumn(key, header, maxLength);
    }
}
