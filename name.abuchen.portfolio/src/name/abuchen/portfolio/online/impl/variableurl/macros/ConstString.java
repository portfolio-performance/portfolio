package name.abuchen.portfolio.online.impl.variableurl.macros;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.impl.variableurl.VariableURLConstructor;

public class ConstString implements Macro
{
    private CharSequence string;

    public ConstString(CharSequence string)
    {
        this.string = string;
    }

    @Override
    public CharSequence resolve(Security security)
    {
        return string;
    }

    @Override
    public VariableURLConstructor getVariableURLConstructor()
    {
        return null;
    }
}
