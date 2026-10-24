package name.abuchen.portfolio.cli;

import name.abuchen.portfolio.model.Client;

/** What core the CLI is built from, as reported by <code>info</code> and the global <code>--help</code>. */
final class CoreVersion
{
    /**
     * The upstream release the bundled core is built from. Bump it together
     * with the core when updating the CLI.
     */
    static final String PINNED_UPSTREAM_TAG = "0.89.0";

    /** The newest client file version this core can read and write. */
    static final int MAX_SUPPORTED_FILE_VERSION = Client.CURRENT_VERSION;

    private CoreVersion()
    {
    }
}
