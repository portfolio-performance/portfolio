package name.abuchen.portfolio.online.impl.variableurl.urls;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.impl.variableurl.macros.Macro;

import java.util.List;

public abstract class BaseURL implements VariableURL
{
    protected List<Macro> macros;
    protected Security security;

    public BaseURL(List<Macro> macros)
    {
        this.macros = macros;
    }

    public List<Macro> getMacros()
    {
        return macros;
    }

    @Override
    public void setSecurity(Security security)
    {
        this.security = security;
    }

    public Security getSecurity()
    {
        return security;
    }
}
