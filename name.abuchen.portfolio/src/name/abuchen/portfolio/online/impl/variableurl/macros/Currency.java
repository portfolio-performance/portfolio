package name.abuchen.portfolio.online.impl.variableurl.macros;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.impl.variableurl.VariableURLConstructor;

public class Currency implements Macro
{
    public Currency(CharSequence input)
    {
        if (!"CURRENCY".equals(input))
            throw new IllegalArgumentException("Bad currency macro: " + input);
    }

    @Override
    public CharSequence resolve(Security security)
    {
        return security.getCurrencyCode();
    }

    @Override
    public VariableURLConstructor getVariableURLConstructor()
    {
        return null;
    }
}
