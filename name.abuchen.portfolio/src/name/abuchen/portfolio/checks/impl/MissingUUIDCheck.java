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
        client.getSecurities().stream().filter(s -> s.getUUID() == null).forEach(s -> s.fixMissingUUID());
        return Collections.emptyList();
    }

}
