package name.abuchen.portfolio.rest.internal;

import com.google.gson.JsonElement;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;

public final class PortfoliosHandler
{
    private PortfoliosHandler()
    {
    }

    public static JsonElement list(Client client)
    {
        return EntityJson.envelope(client.getPortfolios(), EntityJson::toJson);
    }

    public static JsonElement get(Client client, String uuid)
    {
        return EntityJson.toJson(Entities.byUuid(client.getPortfolios(), Portfolio::getUUID, uuid));
    }
}
