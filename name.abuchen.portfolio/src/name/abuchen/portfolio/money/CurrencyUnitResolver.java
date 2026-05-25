package name.abuchen.portfolio.money;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.CustomCurrency;
import name.abuchen.portfolio.util.Pair;

public final class CurrencyUnitResolver
{
    private CurrencyUnitResolver()
    {
    }

    public static CurrencyUnit resolve(Client client, String currencyCode)
    {
        if (currencyCode == null)
            return null;

        CurrencyUnit standard = CurrencyUnit.getInstance(currencyCode);
        if (standard != null)
            return standard;

        if (client == null)
            return null;

        for (CustomCurrency customCurrency : client.getCustomCurrencies())
        {
            if (currencyCode.equals(customCurrency.getCurrencyCode()))
            {
                return CurrencyUnit.of(customCurrency.getCurrencyCode(), customCurrency.getDisplayName(),
                                customCurrency.getCurrencySymbol());
            }
        }

        return null;
    }

    public static boolean contains(Client client, String currencyCode)
    {
        return resolve(client, currencyCode) != null;
    }

    public static List<CurrencyUnit> getAvailableCurrencyUnits(Client client)
    {
        List<CurrencyUnit> currencies = new ArrayList<>(CurrencyUnit.getAvailableCurrencyUnits());

        if (client != null)
        {
            for (CustomCurrency customCurrency : client.getCustomCurrencies())
            {
                currencies.add(CurrencyUnit.of(customCurrency.getCurrencyCode(), customCurrency.getDisplayName(),
                                customCurrency.getCurrencySymbol()));
            }
        }

        return Collections.unmodifiableList(currencies);
    }

    public static void validateNewCustomCurrency(Client client, CustomCurrency currency)
    {
        Objects.requireNonNull(currency);

        if (CurrencyUnit.containsCurrencyCode(currency.getCurrencyCode()))
            throw new IllegalArgumentException("Currency code already exists: " + currency.getCurrencyCode()); //$NON-NLS-1$

        if (client == null)
            return;

        for (CustomCurrency existing : client.getCustomCurrencies())
        {
            if (Objects.equals(existing.getCurrencyCode(), currency.getCurrencyCode()))
                throw new IllegalArgumentException("Currency code already exists: " + currency.getCurrencyCode()); //$NON-NLS-1$
        }
    }

    public static List<Pair<String, List<CurrencyUnit>>> getAvailableCurrencyUnitsGrouped(Client client)
    {
        NavigableMap<Integer, Pair<String, List<CurrencyUnit>>> sublists = new TreeMap<>();
        sublists.put(0, new Pair<>("A - D", new ArrayList<>())); //$NON-NLS-1$
        sublists.put(4, new Pair<>("E - I", new ArrayList<>())); //$NON-NLS-1$
        sublists.put(9, new Pair<>("J - M", new ArrayList<>())); //$NON-NLS-1$
        sublists.put(13, new Pair<>("N - R", new ArrayList<>())); //$NON-NLS-1$
        sublists.put(18, new Pair<>("S - V", new ArrayList<>())); //$NON-NLS-1$
        sublists.put(22, new Pair<>("W - Z", new ArrayList<>())); //$NON-NLS-1$

        for (CurrencyUnit unit : getAvailableCurrencyUnits(client))
        {
            var letter = unit.getCurrencyCode().charAt(0) - 'A';
            sublists.floorEntry(letter).getValue().getRight().add(unit);
        }

        List<Pair<String, List<CurrencyUnit>>> answer = new ArrayList<>(sublists.values());

        for (Pair<String, List<CurrencyUnit>> pair : answer)
            Collections.sort(pair.getRight());

        return answer;
    }
}
