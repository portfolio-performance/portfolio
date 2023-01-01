package name.abuchen.portfolio.junit;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Random;
import java.util.UUID;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.money.CurrencyUnit;

public class SecurityBuilder
{
	private static final Random RANDOM = new Random();

    private Security security;

    public SecurityBuilder()
    {
        this(CurrencyUnit.EUR);
    }

    public SecurityBuilder(String currencyCode)
    {
        this.security = new Security(UUID.randomUUID().toString(), currencyCode);
        this.security.setIsin("DE0001"); //$NON-NLS-1$
        this.security.setTickerSymbol("DAX.DE"); //$NON-NLS-1$
    }

    public SecurityBuilder addPrice(String date, long price)
    {
        SecurityPrice p = new SecurityPrice(LocalDate.parse(date), price);
        security.addPrice(p);
        return this;
    }

    public SecurityBuilder assign(Taxonomy taxonomy, String id)
    {
        return assign(taxonomy, id, Classification.ONE_HUNDRED_PERCENT);
    }

    public SecurityBuilder assign(Taxonomy taxonomy, String id, int weight)
    {
        Classification classification = taxonomy.getClassificationById(id);
        classification.addAssignment(new Assignment(security, weight));
        return this;
    }

    public SecurityBuilder generatePrices(long startPrice, LocalDate start, LocalDate end)
    {
        security.addPrice(new SecurityPrice(start, startPrice));

        LocalDate date = start;
        long price = startPrice;
        while (date.compareTo(end) < 0)
        {
            date = date.plusDays(1);

            if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY)
                continue;

            price = (long) (price * ((RANDOM.nextDouble() * 0.2 - 0.1d) + 1));
            security.addPrice(new SecurityPrice(date, price));
        }

        return this;
    }

    public Security addTo(Client client)
    {
        client.addSecurity(security);
        return security;
    }

    public Security get()
    {
        return security;
    }

}
