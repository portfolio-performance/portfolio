package name.abuchen.portfolio.online.impl.variableurl.urls;

import name.abuchen.portfolio.online.impl.variableurl.iterators.PageIterator;
import name.abuchen.portfolio.online.impl.variableurl.macros.Macro;

import java.util.Iterator;
import java.util.List;

public class PageURL extends BaseURL
{
    public PageURL(List<Macro> macros)
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
        return new PageIterator(this);
    }
}
