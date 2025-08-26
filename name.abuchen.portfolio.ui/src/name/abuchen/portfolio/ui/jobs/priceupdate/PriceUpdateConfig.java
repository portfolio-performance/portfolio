package name.abuchen.portfolio.ui.jobs.priceupdate;

import java.time.LocalDate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.Messages;

public class PriceUpdateConfig
{
    private final PriceUpdateStrategy strategy;
    private final String watchlist;

    public PriceUpdateConfig(PriceUpdateStrategy strategy, String watchlist)
    {
        this.strategy = strategy;
        this.watchlist = watchlist;
    }

    public PriceUpdateConfig(PriceUpdateStrategy strategy)
    {
        this(strategy, null);
    }

    public PriceUpdateStrategy getStrategy()
    {
        return strategy;
    }

    public String getWatchlist()
    {
        return watchlist;
    }

    public Predicate<Security> getPredicate(CurrencyConverter converter, Client client)
    {
        if (strategy == null || strategy == PriceUpdateStrategy.ACTIVE)
            return s -> !s.isRetired();

        // calculate holdings

        var snapshot = ClientSnapshot.create(client, converter, LocalDate.now());
        var securities = snapshot.getJointPortfolio().getPositions().stream().map(p -> p.getSecurity())
                        .collect(Collectors.toSet());

        if (strategy == PriceUpdateStrategy.HOLDINGS_AND_WATCHLIST)
        {
            client.getWatchlists().stream() //
                            .filter(w -> w.getName().equals(watchlist)) //
                            .findFirst().ifPresent(w -> securities.addAll(w.getSecurities()));
        }

        return securities::contains;
    }

    public String getLabel()
    {
        return switch (strategy)
        {
            case ACTIVE -> Messages.LabelActiveSecurities;
            case HOLDINGS -> Messages.LabelHeldSecurities;
            case HOLDINGS_AND_WATCHLIST -> watchlist;
        };
    }

    public String getCode()
    {
        return switch (strategy)
        {
            case ACTIVE -> "X"; //$NON-NLS-1$
            case HOLDINGS -> "H"; //$NON-NLS-1$
            case HOLDINGS_AND_WATCHLIST -> "W" + watchlist; //$NON-NLS-1$
        };
    }

    public static PriceUpdateConfig fromCode(String code)
    {
        if (code == null || code.isBlank())
            return new PriceUpdateConfig(PriceUpdateStrategy.ACTIVE);

        return switch (code.charAt(0))
        {
            case 'X' -> new PriceUpdateConfig(PriceUpdateStrategy.ACTIVE);
            case 'H' -> new PriceUpdateConfig(PriceUpdateStrategy.HOLDINGS);
            case 'W' -> new PriceUpdateConfig(PriceUpdateStrategy.HOLDINGS_AND_WATCHLIST, code.substring(1));
            default -> new PriceUpdateConfig(PriceUpdateStrategy.ACTIVE);
        };
    }
}
