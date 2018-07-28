package name.abuchen.portfolio.online.impl.variableurl.macros;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.impl.variableurl.VariableURLConstructor;

public class WKN implements Macro
{
    public WKN(CharSequence input)
    {
        if (!"WKN".equals(input))
            throw new IllegalArgumentException("Bad WKN macro: " + input);
    }

    @Override
    public CharSequence resolve(Security security)
    {
        return security.getWkn();
    }

    @Override
    public VariableURLConstructor getVariableURLConstructor()
    {
        return null;
    }
}
