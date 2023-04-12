package name.abuchen.portfolio.online.impl.variableurl.iterators;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.NoSuchElementException;

import name.abuchen.portfolio.online.impl.variableurl.macros.FormattedDate;
import name.abuchen.portfolio.online.impl.variableurl.macros.Macro;
import name.abuchen.portfolio.online.impl.variableurl.urls.DateURL;

public class DateIterator implements Iterator<String>
{
    private DateURL url;
    private LocalDate current;
    private LocalDate stop;
    private long step;
    private String previous;
    private String nextItem;

    public DateIterator(DateURL url, LocalDate start, LocalDate stop, long step)
    {
        this.url = url;
        this.current = start;
        this.stop = stop;
        this.step = step;
        this.previous = null;
        this.nextItem = null;

        if (step < 0 ? current.isBefore(stop) : current.isAfter(stop))
            current = stop;
    }

    @Override
    public boolean hasNext()
    {
        if (nextItem != null)
            return true;

        while (!current.equals(stop))
        {
            String result = resolve(current);

            if (!result.equals(previous))
            {
                nextItem = result;
                current = current.plusDays(step);
                return true;
            }

            current = current.plusDays(step);
        }

        return false;
    }

    @Override
    public String next()
    {
        if (!hasNext())
            throw new NoSuchElementException();

        previous = nextItem;
        nextItem = null;

        return previous;
    }

    private String resolve(LocalDate date)
    {
        StringBuilder result = new StringBuilder();

        for (Macro macro : url.getMacros())
        {
            if (macro instanceof FormattedDate formattedDate)
            {
                result.append(formattedDate.resolve(date));
            }
            else
            {
                result.append(macro.resolve(url.getSecurity()));
            }
        }

        return result.toString();
    }
}
