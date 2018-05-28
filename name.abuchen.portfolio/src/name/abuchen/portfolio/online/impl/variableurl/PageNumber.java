package name.abuchen.portfolio.online.impl.variableurl;

import name.abuchen.portfolio.model.Security;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class PageNumber implements VariableURL
{
    private class PagesIterator implements Iterator<String>
    {
        long current;

        private PagesIterator()
        {
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
            Iterator<CharSequence> oarts = PageNumber.this.constants.iterator();

            for (long i = 0; i < variables; ++i)
            {
                result.append(oarts.next());
                result.append(current);
            }

            while (oarts.hasNext())
                result.append(oarts.next());

            ++current;

            return result.toString();
        }
    }

    private List<CharSequence> constants;
    private long variables;

    public PageNumber(List<CharSequence> parts) throws IllegalArgumentException // NOSONAR
    {
        boolean isMacro = false;
        constants = new LinkedList<>();
        variables = 0;

        for (CharSequence part : parts)
        {
            if (isMacro)
            {
                if (!"PAGE".equals(part)) //$NON-NLS-1$
                    throw new IllegalArgumentException("Bad macro: " + part); //$NON-NLS-1$

                ++variables;
            }
            else
            {
                constants.add(part);
            }

            isMacro = !isMacro;
        }
    }

    @Override
    public void setSecurity(Security security)
    {
        // security not needed to generate page number
    }

    @Override
    public long getMaxFailedAttempts()
    {
        return 0;
    }

    @Override
    public Iterator<String> iterator()
    {
        return new PagesIterator();
    }
}
