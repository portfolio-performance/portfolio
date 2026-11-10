package name.abuchen.portfolio.checks.impl;

import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.checks.Check;
import name.abuchen.portfolio.checks.Issue;
import name.abuchen.portfolio.model.Client;

/**
 * As per issue #571 some securities are missing UUIDs.
 */
public class MissingUUIDCheck implements Check
{

    @Override
    public List<Issue> execute(Client client)
    {
        var securities = client.getSecurities().stream().filter(s -> s.getUUID() == null).toList();
        securities.forEach(s -> s.fixMissingUUID());

        if (!securities.isEmpty())
            client.markDirty();

        return Collections.emptyList();
    }

}
