package name.abuchen.portfolio.cli.currency;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.cli.MasterData;
import name.abuchen.portfolio.cli.Warning;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;

/**
 * The {@link Prices#STALE_PRICE} warnings of a command that values many
 * instruments on one day: one per instrument, however often it is met, in the
 * order of instrument name, then UUID, whatever order they are met in.
 */
public final class StalePrices
{
    private final LocalDate asOf;
    private final Map<Security, Warning> warnings = new HashMap<>();

    public StalePrices(LocalDate asOf)
    {
        this.asOf = asOf;
    }

    /** Notes the instrument's latest price; warns only if it is stale. */
    public void note(Security security, SecurityPrice latest)
    {
        if (!warnings.containsKey(security))
            Prices.staleWarning(security, latest, asOf).ifPresent(warning -> warnings.put(security, warning));
    }

    public List<Warning> warnings()
    {
        return warnings.keySet().stream().sorted(MasterData.byNameThenUuid(Security::getName, Security::getUUID))
                        .map(warnings::get).toList();
    }
}
