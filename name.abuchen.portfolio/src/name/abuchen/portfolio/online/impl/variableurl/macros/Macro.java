package name.abuchen.portfolio.online.impl.variableurl.macros;

import name.abuchen.portfolio.online.impl.variableurl.VariableURLConstructor;

public interface Macro
{
    VariableURLConstructor getVariableURLConstructor();

    CharSequence resolve() throws UnsupportedOperationException;
}
