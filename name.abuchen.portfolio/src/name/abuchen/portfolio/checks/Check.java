package name.abuchen.portfolio.checks;

import java.util.List;

import name.abuchen.portfolio.model.Client;

public interface Check
{
    List<Issue> execute(Client client);
}
