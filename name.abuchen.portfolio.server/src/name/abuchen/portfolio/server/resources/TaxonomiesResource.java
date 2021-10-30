package name.abuchen.portfolio.server.resources;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import name.abuchen.portfolio.json.JTaxonomy;
import name.abuchen.portfolio.model.Client;

@Path("/taxonomies")
public class TaxonomiesResource
{
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<JTaxonomy> listTaxonomies(@Context Client client)
    {
        var taxonomies = client.getTaxonomies().stream().map(JTaxonomy::from).collect(Collectors.toList());
        
        return taxonomies;
    }
}
