package name.abuchen.portfolio.cli.currency;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.cli.command.Declaration;
import name.abuchen.portfolio.cli.command.Facet;
import name.abuchen.portfolio.cli.command.MasterData;
import name.abuchen.portfolio.cli.command.Warning;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;

/**
 * The {@link Prices#STALE_PRICE} warnings of a command that values many
 * instruments: one per instrument, however often it is met, in the order of
 * instrument name, then UUID, whatever order they are met in.
 */
public final class StalePrices
{
    /**
     * The collector as a facet: the handler notes the latest prices it values
     * at, and the facet warns of the stale ones after it.
     */
    public static final Facet<StalePrices> FACET = Facet.of(Declaration.empty().warnings(Prices.STALE_PRICE),
                    run -> new StalePrices(), (stalePrices, out) -> stalePrices.warnings().forEach(out::warn));

    private final Map<Security, Warning> warnings = new HashMap<>();

    private StalePrices()
    {
    }

    /** Notes the instrument's latest price on the day it values at; warns only if it is stale. */
    public void note(Security security, SecurityPrice latest, LocalDate asOf)
    {
        if (!warnings.containsKey(security))
            Prices.staleWarning(security, latest, asOf).ifPresent(warning -> warnings.put(security, warning));
    }

    private List<Warning> warnings()
    {
        return warnings.keySet().stream().sorted(MasterData.byNameThenUuid(Security::getName, Security::getUUID))
                        .map(warnings::get).toList();
    }
}
