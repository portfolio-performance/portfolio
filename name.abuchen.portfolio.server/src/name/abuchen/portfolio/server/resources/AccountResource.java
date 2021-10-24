package name.abuchen.portfolio.server.resources;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import name.abuchen.portfolio.json.JAccount;
import name.abuchen.portfolio.json.JSecurity;
import name.abuchen.portfolio.model.Client;

@Path("/accounts")
public class AccountResource
{
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<JAccount> listAccounts(@Context Client client)
    {
        var accounts = client.getAccounts().stream().map(JAccount::from);

        return accounts.collect(Collectors.toList());
    }
}
