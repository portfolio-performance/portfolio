package name.abuchen.portfolio.rest.internal;

import com.google.gson.JsonElement;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;

public final class AccountsHandler
{
    private AccountsHandler()
    {
    }

    public static JsonElement list(Client client)
    {
        return EntityJson.envelope(client.getAccounts(), EntityJson::toJson);
    }

    public static JsonElement get(Client client, String uuid)
    {
        return EntityJson.toJson(Entities.byUuid(client.getAccounts(), Account::getUUID, uuid));
    }
}
