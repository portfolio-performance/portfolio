package name.abuchen.portfolio.online.impl.variableurl;

import java.util.List;

import name.abuchen.portfolio.online.impl.variableurl.macros.Macro;
import name.abuchen.portfolio.online.impl.variableurl.urls.VariableURL;

public interface VariableURLConstructor
{
    VariableURL construct(List<Macro> macros);
}
