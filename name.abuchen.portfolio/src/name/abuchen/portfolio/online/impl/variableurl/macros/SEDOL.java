package name.abuchen.portfolio.online.impl.variableurl.macros;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.impl.variableurl.VariableURLConstructor;

public class SEDOL implements Macro
{
    public SEDOL(CharSequence input)
    {
        if (!"SEDOL".equals(input)) //$NON-NLS-1$
            throw new IllegalArgumentException();
    }

    @Override
    public CharSequence resolve(Security security)
    {
        return security != null ? security.getSedol() : null;
    }

    @Override
    public VariableURLConstructor getVariableURLConstructor()
    {
        return null;
    }
}
