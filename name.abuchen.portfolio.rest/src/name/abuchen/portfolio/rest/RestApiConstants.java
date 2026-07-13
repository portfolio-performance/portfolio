package name.abuchen.portfolio.rest;

public final class RestApiConstants
{
    public static final String PLUGIN_ID = "name.abuchen.portfolio.rest"; //$NON-NLS-1$

    /** instance-scope preference keys under node {@link #PLUGIN_ID} */
    public static final String PREF_ENABLED = "enabled"; //$NON-NLS-1$
    public static final String PREF_PORT = "port"; //$NON-NLS-1$

    /** child node under {@link #PLUGIN_ID} holding the per-file access records */
    public static final String PREF_NODE_FILES = "files"; //$NON-NLS-1$

    public static final int DEFAULT_PORT = 5712;

    private RestApiConstants()
    {
    }
}
