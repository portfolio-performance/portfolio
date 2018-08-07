package name.abuchen.portfolio.online.impl.variableurl.macros;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.impl.variableurl.VariableURLConstructor;
import name.abuchen.portfolio.online.impl.variableurl.urls.PageURL;

public class PageNumber implements Macro
{
    public PageNumber(CharSequence input) throws IllegalArgumentException
    {
        if (!"PAGE".equals(input)) //$NON-NLS-1$
            throw new IllegalArgumentException();
    }

    @Override
    public VariableURLConstructor getVariableURLConstructor()
    {
        return PageURL::new;
    }

    @Override
    public CharSequence resolve(Security security) throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException();
    }

    public String resolve(long page)
    {
        return String.valueOf(page);
    }
}
