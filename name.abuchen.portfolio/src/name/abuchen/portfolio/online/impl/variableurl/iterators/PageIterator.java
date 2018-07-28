package name.abuchen.portfolio.online.impl.variableurl.iterators;

import name.abuchen.portfolio.online.impl.variableurl.macros.Macro;
import name.abuchen.portfolio.online.impl.variableurl.macros.PageNumber;
import name.abuchen.portfolio.online.impl.variableurl.urls.PageURL;

import java.util.Iterator;

public class PageIterator implements Iterator<String>
{
    private PageURL url;
    private long current;

    public PageIterator(PageURL url)
    {
        this.url = url;
        current = 1;
    }

    @Override
    public boolean hasNext()
    {
        return true;
    }

    @Override
    public String next()
    {
        StringBuilder result = new StringBuilder();

        for (Macro macro : url.getMacros())
        {
            if (macro instanceof PageNumber)
            {
                result.append(((PageNumber)macro).resolve(current));
            }
            else
            {
                result.append(macro.resolve(url.getSecurity()));
            }
        }

        ++current;

        return result.toString();
    }
}
