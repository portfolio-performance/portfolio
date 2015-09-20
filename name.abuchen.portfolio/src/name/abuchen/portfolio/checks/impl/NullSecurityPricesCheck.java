package name.abuchen.portfolio.checks.impl;

import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.checks.Check;
import name.abuchen.portfolio.checks.Issue;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;

/**
 * Removes any null values from the security price list. This check fixes a
 * NullPointerException reported in the forum although it is unclear how null
 * values have been added to the security price list in the first place.
 */
public class NullSecurityPricesCheck implements Check
{

    @Override
    public List<Issue> execute(Client client)
    {
        for (Security security : client.getSecurities())
        {
            for (SecurityPrice price : security.getPrices())
            {
                if (price == null)
                {
                    security.removePrice(null);

                    // multiple null values cannot exist due to the binary
                    // search / replacement logic that fails when adding the
                    // second null value

                    break;
                }
            }
        }

        return Collections.emptyList();
    }
}
