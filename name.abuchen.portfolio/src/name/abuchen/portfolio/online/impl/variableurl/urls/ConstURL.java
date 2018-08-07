package name.abuchen.portfolio.online.impl.variableurl.urls;

import name.abuchen.portfolio.online.impl.variableurl.macros.Macro;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ConstURL extends BaseURL
{
    public ConstURL(List<Macro> macros)
    {
        super(macros);
    }

    @Override
    public long getMaxFailedAttempts()
    {
        return 0;
    }

    @Override
    public Iterator<String> iterator()
    {
        StringBuilder result = new StringBuilder();

        for (Macro macro : macros)
        {
            result.append(macro.resolve(security));
        }

        return Collections.singletonList(result.toString()).iterator();
    }
}
