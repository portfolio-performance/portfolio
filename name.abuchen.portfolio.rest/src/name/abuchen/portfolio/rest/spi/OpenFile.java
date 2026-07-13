package name.abuchen.portfolio.rest.spi;

import name.abuchen.portfolio.model.Client;

/**
 * A portfolio file currently open in the application. Implemented by the UI
 * plugin, backed by ClientInput.
 */
public interface OpenFile
{
    /** absolute file path; unique per machine and the identity key */
    String getPath();

    String getLabel();

    Client getClient();
}
