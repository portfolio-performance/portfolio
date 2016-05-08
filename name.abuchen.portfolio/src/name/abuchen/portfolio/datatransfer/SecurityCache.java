package name.abuchen.portfolio.datatransfer;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
    private static final Security DUPLICATE_SECURITY_MARKER = new Security();

    private static final List<String> MESSAGES = Arrays.asList(Messages.MsgErrorDuplicateISIN,
                    Messages.MsgErrorDuplicateTicker, Messages.MsgErrorDuplicateWKN, Messages.MsgErrorDuplicateName);

    private final Client client;

    private final List<Map<String, Security>> localMaps = new ArrayList<>();

    public SecurityCache(Client client)
    {
        this.client = client;

        this.localMaps.add(client.getSecurities().stream().filter(s -> s.getIsin() != null && !s.getIsin().isEmpty())
                        .collect(Collectors.toMap(Security::getIsin, s -> s, (l, r) -> DUPLICATE_SECURITY_MARKER)));

        this.localMaps.add(client.getSecurities().stream()
                        .filter(s -> s.getTickerSymbol() != null && !s.getTickerSymbol().isEmpty()) //
                        .collect(Collectors.toMap(Security::getTickerSymbol, s -> s,
                                        (l, r) -> DUPLICATE_SECURITY_MARKER)));

        this.localMaps.add(client.getSecurities().stream().filter(s -> s.getWkn() != null && !s.getWkn().isEmpty())
                        .collect(Collectors.toMap(Security::getWkn, s -> s, (l, r) -> DUPLICATE_SECURITY_MARKER)));

        this.localMaps.add(client.getSecurities().stream().filter(s -> s.getName() != null && !s.getName().isEmpty())
                        .collect(Collectors.toMap(Security::getName, s -> s, (l, r) -> DUPLICATE_SECURITY_MARKER)));

    }

    public Security lookup(String isin, String tickerSymbol, String wkn, String name,
                    Supplier<Security> creationFunction)
    {
        List<String> attributes = Arrays.asList(isin, tickerSymbol, wkn, name);

        for (int ii = 0; ii < localMaps.size(); ii++)
        {
            String attribute = attributes.get(ii);

            Security security = localMaps.get(ii).get(attribute);
            if (security == DUPLICATE_SECURITY_MARKER)
                throw new IllegalArgumentException(MessageFormat.format(MESSAGES.get(ii), attribute));
            if (security != null)
                return security;
        }

        Security security = creationFunction.get();
        security.setIsin(isin);
        security.setWkn(wkn);
        security.setTickerSymbol(tickerSymbol);
        security.setName(name);

        for (int ii = 0; ii < localMaps.size(); ii++)
        {
            String attribute = attributes.get(ii);
            if (attribute != null)
                localMaps.get(ii).put(attribute, security);
        }

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
        items.stream().filter(i -> i instanceof SecurityItem).map(Item::getSecurity).forEach(available::add);

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
