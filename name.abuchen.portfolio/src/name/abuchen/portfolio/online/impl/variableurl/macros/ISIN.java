package name.abuchen.portfolio.online.impl.variableurl.macros;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.impl.variableurl.VariableURLConstructor;

public class ISIN implements Macro
{
    public ISIN(CharSequence input)
    {
        if (!"ISIN".equals(input)) //$NON-NLS-1$
            throw new IllegalArgumentException();
    }

    @Override
    public CharSequence resolve(Security security)
    {
        return security.getIsin();
    }

    @Override
    public VariableURLConstructor getVariableURLConstructor()
    {
        return null;
    }
}
