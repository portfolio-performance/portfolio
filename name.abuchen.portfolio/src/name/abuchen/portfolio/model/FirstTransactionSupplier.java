package name.abuchen.portfolio.model;

import java.util.Optional;
import java.util.function.Supplier;

import name.abuchen.portfolio.model.Transaction.ByDate;

public final class FirstTransactionSupplier implements Supplier<Optional<PortfolioTransaction>>
{

    private final Client client;
    private final ByDate byDateComparator;
    
    public FirstTransactionSupplier(Client client)
    {
        this.client = client;
        this.byDateComparator = new ByDate();
    }


    @Override
    public Optional<PortfolioTransaction> get()
    {
        if (client == null) return Optional.empty();
        return client.getPortfolios().stream().flatMap(p -> p.getTransactions().stream()).min(byDateComparator);
    }

}
