package name.abuchen.portfolio.online.impl.variableurl;

import name.abuchen.portfolio.online.impl.variableurl.macros.Macro;
import name.abuchen.portfolio.online.impl.variableurl.urls.VariableURL;

import java.util.List;

public interface VariableURLConstructor
{
    VariableURL construct(List<Macro> macros);
}
