package name.abuchen.portfolio.online.impl.variableurl.macros;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.impl.variableurl.VariableURLConstructor;

public interface Macro
{
    VariableURLConstructor getVariableURLConstructor();

    CharSequence resolve(Security security) throws UnsupportedOperationException;
}
