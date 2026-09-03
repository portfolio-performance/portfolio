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

    /** collection path of the interactive pairing requests; exempt from bearer auth */
    public static final String PAIRING_ENDPOINT = "/v1/auth/requests"; //$NON-NLS-1$

    /** the API's own OpenAPI description; exempt from bearer auth so it is discoverable before pairing */
    public static final String OPENAPI_ENDPOINT = "/v1/openapi.yaml"; //$NON-NLS-1$

    /** the API contract version; exempt from bearer auth, like the description it summarises */
    public static final String VERSION_ENDPOINT = "/v1/version"; //$NON-NLS-1$

    /**
     * The version of the {@code /v1} contract. Additive changes within v1 bump
     * it; a change that would break a client means /v2 at a new path, never a
     * major bump here. Kept in lockstep with {@code info.version} in
     * openapi.yaml by {@code OpenApiSpecDriftTest} rather than parsed at
     * runtime, so the server needs no YAML reader to answer /v1/version.
     */
    public static final String API_VERSION = "1.0.0"; //$NON-NLS-1$

    private RestApiConstants()
    {
    }
}
