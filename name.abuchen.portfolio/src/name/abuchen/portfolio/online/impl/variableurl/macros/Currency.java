package name.abuchen.portfolio.online.impl.variableurl.macros;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.impl.variableurl.VariableURLConstructor;

public class Currency implements Macro
{
    public Currency(CharSequence input)
    {
        if (!"CURRENCY".equals(input)) //$NON-NLS-1$
            throw new IllegalArgumentException();
    }

    @Override
    public CharSequence resolve(Security security)
    {
        return security != null ? security.getCurrencyCode() : null;
    }

    @Override
    public VariableURLConstructor getVariableURLConstructor()
    {
        return null;
    }
}
