package name.abuchen.portfolio.rest.internal;

import com.google.gson.JsonElement;

import name.abuchen.portfolio.model.Client;

public final class TaxonomiesHandler
{
    private TaxonomiesHandler()
    {
    }

    /**
     * The taxonomies the file defines, with their category trees.
     * <p>
     * This is the key to the {@code classifications} object on a holding, which is
     * keyed by taxonomy id and names its categories by path: without this list a
     * client has an id it cannot label and no way to know which categories exist but
     * hold nothing. The tree is what lets it show an empty category as empty rather
     * than not at all.
     */
    public static JsonElement list(Client client)
    {
        return EntityJson.envelope(client.getTaxonomies(), EntityJson::toJson);
    }
}
