package name.abuchen.portfolio.server.resources;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import name.abuchen.portfolio.json.JPortfolio;
import name.abuchen.portfolio.json.JPortfolioSnapshot;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceIndicator;
import name.abuchen.portfolio.snapshot.security.SecurityPerformanceSnapshot;
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
    public JPortfolioSnapshot getPortfolioSnapshot(@PathParam("id") String id, @Context Client client) 
    {
        var oPortfolio = client.getPortfolios().stream().filter(portfolio -> portfolio.getUUID().equals(id)).findFirst();
        
        if(oPortfolio.isEmpty())
        {
            throw new NotFoundException();
        }
        
        var portfolio = oPortfolio.get();

        var filteredClient = new PortfolioClientFilter(portfolio).filter(client);
        
        var erFactory = new ExchangeRateProviderFactory(filteredClient);
        var cc = new CurrencyConverterImpl(erFactory, filteredClient.getBaseCurrency());
        
        var date = LocalDate.now();
        
        var clientSnapshot = ClientSnapshot.create(filteredClient, cc, date);

        var performanceSnapshot = SecurityPerformanceSnapshot.create(filteredClient, cc, Interval.of(LocalDate.MIN, date), SecurityPerformanceIndicator.Costs.class);
        
        var snapshot = JPortfolioSnapshot.from(clientSnapshot.getPortfolios().get(0), performanceSnapshot);
        
        return snapshot;
    }
}
