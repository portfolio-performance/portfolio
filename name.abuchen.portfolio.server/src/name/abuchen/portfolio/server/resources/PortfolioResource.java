package name.abuchen.portfolio.server.resources;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import name.abuchen.portfolio.json.JPortfolio;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.ui.views.StatementOfAssetsViewer;
import name.abuchen.portfolio.util.Interval;

@Path("/portfolios")
public class PortfolioResource
{
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<JPortfolio> listPortfolios(@Context Client client)
    {
        var erFactory = new ExchangeRateProviderFactory(client);
        var cc = new CurrencyConverterImpl(erFactory, client.getBaseCurrency());
       
        var portfolios = client.getPortfolios().stream().map(portfolio -> {
            return JPortfolio.from(portfolio, PortfolioSnapshot.create(portfolio, cc, LocalDate.now()));
        });
        
        return portfolios.collect(Collectors.toList());
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}/assets")
    public List<JAssetElement> getPortfolioSnapshot(
                    @PathParam("id") String id,
                    @QueryParam("groupBy") String groupBy,
                    @Context Client client) 
    {
        var oPortfolio = client.getPortfolios().stream().filter(portfolio -> portfolio.getUUID().equals(id)).findFirst();
        
        if(oPortfolio.isEmpty())
        {
            throw new NotFoundException();
        }
        
        var portfolio = oPortfolio.get();
        
        var erFactory = new ExchangeRateProviderFactory(client);
        var cc = new CurrencyConverterImpl(erFactory, client.getBaseCurrency());
        
        var date = LocalDate.now();

        Taxonomy taxonomy = null;
        
        if(groupBy != null)
        {
             taxonomy = client.getTaxonomy(groupBy);
        }
        
        var allTime = Interval.of(LocalDate.MIN, LocalDate.now()); 
        
        var model = new StatementOfAssetsViewer.Model(client, new PortfolioClientFilter(portfolio), cc, date, taxonomy);
        model.calculatePerformanceAndInjectIntoElements(client.getBaseCurrency(), allTime);
                
        var elements = model.getElements().stream().map(JAssetElement::from).collect(Collectors.toList());
        
        return elements;
    }
}
