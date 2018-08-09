package name.abuchen.portfolio.online.impl.variableurl.urls;

import name.abuchen.portfolio.model.Security;

public interface VariableURL extends Iterable<String>
{
    void setSecurity(Security security);

    long getMaxFailedAttempts();
}
