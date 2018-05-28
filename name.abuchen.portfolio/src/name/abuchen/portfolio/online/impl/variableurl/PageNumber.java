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
            Iterator<CharSequence> constants = PageNumber.this.constants.iterator();

            for (long i = 0; i < variables; ++i)
            {
                result.append(constants.next());
                result.append(current);
            }

            while (constants.hasNext())
                result.append(constants.next());

            ++current;

            return result.toString();
        }
    }

    private List<CharSequence> constants;
    private long variables;

    public PageNumber(List<CharSequence> parts) throws IllegalArgumentException
    {
        boolean isMacro = false;
        constants = new LinkedList<>();
        variables = 0;

        for (CharSequence part : parts)
        {
            if (isMacro)
            {
                if (!part.equals("PAGE"))
                    throw new IllegalArgumentException("Bad macro: " + part);

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
