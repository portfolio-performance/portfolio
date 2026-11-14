package name.abuchen.portfolio.rest.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Client;

/**
 * The file itself: the currency it counts in, the currencies it holds, the
 * version its XML was read at, and how much of each kind of thing it contains.
 * Nothing here is computed from the transactions, so the answer is the cheapest
 * a file can give - and the right one to read before choosing which of the
 * larger reports to ask for.
 */
public final class FileInfoHandler
{
    private FileInfoHandler()
    {
    }

    public static JsonObject info(Client client)
    {
        var json = new JsonObject();
        json.addProperty("baseCurrency", client.getBaseCurrency()); //$NON-NLS-1$
        json.addProperty("fileVersion", client.getFileVersionAfterRead()); //$NON-NLS-1$

        var currencies = new JsonArray();
        for (var unit : client.getUsedCurrencies())
            currencies.add(unit.getCurrencyCode());
        json.add("currencies", currencies); //$NON-NLS-1$

        var taxonomies = client.getTaxonomies();
        var classes = 0;
        for (var taxonomy : taxonomies)
            classes += taxonomy.getAllClassifications().size();

        var counts = new JsonObject();
        counts.addProperty("instruments", client.getSecurities().size()); //$NON-NLS-1$
        counts.addProperty("cashAccounts", client.getAccounts().size()); //$NON-NLS-1$
        counts.addProperty("investmentAccounts", client.getPortfolios().size()); //$NON-NLS-1$
        counts.addProperty("investmentPlans", client.getPlans().size()); //$NON-NLS-1$
        counts.addProperty("watchlists", client.getWatchlists().size()); //$NON-NLS-1$
        counts.addProperty("taxonomies", taxonomies.size()); //$NON-NLS-1$
        counts.addProperty("classes", classes); //$NON-NLS-1$
        json.add("counts", counts); //$NON-NLS-1$

        return json;
    }
}
