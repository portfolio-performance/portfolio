package name.abuchen.portfolio.checks;

import java.util.List;

import name.abuchen.portfolio.model.Client;

public interface Check
{
    /**
     * Execute a consistency check on the given client.
     * 
     * @return list of issues; empty list if no issues are found
     */
    List<Issue> execute(Client client);
}
