package name.abuchen.portfolio.checks.impl;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.checks.Check;
import name.abuchen.portfolio.checks.Issue;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

/**
 * Adds securities which are only present in transactions to the list of
 * securities. Before #602 it could happen that the user imports only the
 * transaction from PDF but explicitly does not import the newly created
 * security. That created orphaned securities.
 */
public class FixOrphanedSecurtiesCheck implements Check
{

    @Override
    public List<Issue> execute(Client client)
    {
        Set<Security> missing = client.getPortfolios().stream() //
                        .flatMap(p -> p.getTransactions().stream()) //
                        .map(t -> t.getSecurity()) //
                        .filter(s -> !client.getSecurities().contains(s)) //
                        .collect(Collectors.toSet());

        missing.stream().forEach(security -> {
            security.setName(security.getName() + Messages.LabelSuffixEntryCorrected);
            client.addSecurity(security);
        });

        return Collections.emptyList();
    }

}
