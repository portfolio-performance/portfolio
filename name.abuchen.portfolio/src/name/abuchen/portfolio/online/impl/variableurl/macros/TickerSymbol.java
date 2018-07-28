package name.abuchen.portfolio.online.impl.variableurl.macros;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.impl.variableurl.VariableURLConstructor;

public class TickerSymbol implements Macro
{
    public TickerSymbol(CharSequence input)
    {
        if (!"TICKER".equals(input))
            throw new IllegalArgumentException("Bad ticker symbol macro: " + input);
    }

    @Override
    public CharSequence resolve(Security security)
    {
        return security.getTickerSymbol();
    }

    @Override
    public VariableURLConstructor getVariableURLConstructor()
    {
        return null;
    }
}
