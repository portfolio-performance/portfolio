package name.abuchen.portfolio.online.impl.variableurl;

import name.abuchen.portfolio.model.Security;

import java.util.Collections;
import java.util.Iterator;

public class ConstString implements VariableURL
{
    private Iterable<String> strings;

    public ConstString(CharSequence string)
    {
        strings = Collections.singletonList(string.toString());
    }

    @Override
    public void setSecurity(Security security)
    {
    }

    @Override
    public long getMaxFailedAttempts()
    {
        return 0;
    }

    @Override
    public Iterator<String> iterator()
    {
        return strings.iterator();
    }
}
