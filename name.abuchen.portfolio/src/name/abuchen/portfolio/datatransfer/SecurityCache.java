package name.abuchen.portfolio.datatransfer;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor.Item;
import name.abuchen.portfolio.datatransfer.Extractor.SecurityItem;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

public class SecurityCache
{
    private final Client client;

    private final Map<String, Security> isin2security;
    private final Map<String, Security> wkn2security;
    private final Map<String, Security> ticker2security;

    public SecurityCache(Client client)
    {
        this.client = client;

        this.isin2security = client.getSecurities().stream().filter(s -> s.getIsin() != null && !s.getIsin().isEmpty())
                        .collect(Collectors.toMap(Security::getIsin, s -> s,
                                        (l, r) -> failWith(Messages.MsgErrorDuplicateISIN, l.getIsin())));

        this.wkn2security = client.getSecurities().stream().filter(s -> s.getWkn() != null && !s.getWkn().isEmpty())
                        .collect(Collectors.toMap(Security::getWkn, s -> s,
                                        (l, r) -> failWith(Messages.MsgErrorDuplicateWKN, l.getWkn())));

        this.ticker2security = client.getSecurities().stream()
                        .filter(s -> s.getTickerSymbol() != null && !s.getTickerSymbol().isEmpty())
                        .collect(Collectors.toMap(Security::getTickerSymbol, s -> s,
                                        (l, r) -> failWith(Messages.MsgErrorDuplicateTicker, l.getTickerSymbol())));
    }

    private Security failWith(String message, String parameter)
    {
        throw new IllegalArgumentException(MessageFormat.format(message, parameter));
    }

    public Security lookup(String isin, String tickerSymbol, String wkn, Supplier<Security> creationFunction)
    {
        Security security = null;
        if (isin != null)
            security = isin2security.get(isin);
        if (security != null)
            return security;

        if (wkn != null)
            security = wkn2security.get(wkn);
        if (security != null)
            return security;

        if (tickerSymbol != null)
            security = ticker2security.get(tickerSymbol);
        if (security != null)
            return security;

        security = creationFunction.get();
        security.setIsin(isin);
        security.setWkn(wkn);
        security.setTickerSymbol(tickerSymbol);

        if (security.getIsin() != null)
            isin2security.put(security.getIsin(), security);
        if (security.getWkn() != null)
            wkn2security.put(security.getWkn(), security);
        if (security.getTickerSymbol() != null)
            wkn2security.put(security.getTickerSymbol(), security);

        return security;
    }

    /**
     * Returns a list of {@link SecurityItem} that are implicitly created when
     * extracting transactions. Do not add all newly created securities as they
     * might be created out of erroneous transactions.
     */
    public Collection<Item> createMissingSecurityItems(List<Item> items)
    {
        List<Item> answer = new ArrayList<>();

        Set<Security> available = new HashSet<>();
        available.addAll(client.getSecurities());
        items.stream().filter(i -> i instanceof SecurityItem).map(Item::getSecurity).forEach(s -> available.add(s));

        for (Item item : items)
        {
            if (item instanceof SecurityItem || item.getSecurity() == null)
                continue;

            if (!available.contains(item.getSecurity()))
            {
                answer.add(new SecurityItem(item.getSecurity()));
                available.add(item.getSecurity());
            }
        }

        return answer;
    }
}
