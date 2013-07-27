package name.abuchen.portfolio;

import java.util.Random;
import java.util.UUID;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Classification.Assignment;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.online.QuoteFeed;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

public class SecurityBuilder
{
    private Security security;

    public SecurityBuilder()
    {
        this.security = new Security(UUID.randomUUID().toString(), //
                        "DE0001", //$NON-NLS-1$
                        "DAX.DE", //$NON-NLS-1$
                        QuoteFeed.MANUAL);
    }

    public SecurityBuilder addPrice(String date, long price)
    {
        SecurityPrice p = new SecurityPrice(new DateTime(date).toDate(), price);
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

    public SecurityBuilder generatePrices(long startPrice, DateMidnight start, DateMidnight end)
    {
        security.addPrice(new SecurityPrice(start.toDate(), startPrice));

        Random random = new Random();

        DateMidnight date = start;
        long price = startPrice;
        while (date.compareTo(end) < 0)
        {
            date = date.plusDays(1);

            if (date.getDayOfWeek() > DateTimeConstants.SATURDAY)
                continue;

            price = (long) ((double) price * ((random.nextDouble() * 0.2 - 0.1d) + 1));
            security.addPrice(new SecurityPrice(date.toDate(), price));
        }

        return this;
    }

    public Security addTo(Client client)
    {
        client.addSecurity(security);
        return security;
    }

}
