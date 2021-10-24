package name.abuchen.portfolio.server.resources;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import name.abuchen.portfolio.json.JClient;
import name.abuchen.portfolio.json.JSecurity;
import name.abuchen.portfolio.json.JTransaction;
import name.abuchen.portfolio.model.Client;

@Path("/securities")
public class SecuritiesResource
{
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<JSecurity> listSecurities(@Context Client client)
    {
        var securities = client.getSecurities().stream().map(JSecurity::from);


        return securities.collect(Collectors.toList());
    }
}
