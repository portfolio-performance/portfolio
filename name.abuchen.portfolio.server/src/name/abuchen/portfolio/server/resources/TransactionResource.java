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
import name.abuchen.portfolio.json.JTransaction;
import name.abuchen.portfolio.model.Client;

@Path("/transactions")
public class TransactionResource
{

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<JTransaction> listTransactions(@Context Client client, @QueryParam("from") LocalDate from,
                    @QueryParam("to") LocalDate to)
    {
        Stream<JTransaction> transactions = JClient.from(client.getAllTransactions()).getTransactions();

        if (from != null)
            transactions = transactions.filter(t -> t.getDate().isAfter(from));

        if (to != null)
            transactions = transactions.filter(t -> !t.getDate().isAfter(to));

        return transactions.collect(Collectors.toList());
    }
}
