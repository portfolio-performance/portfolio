package name.abuchen.portfolio.online.impl.variableurl;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormattedDate implements VariableURL
{
    private class DatesIterator implements Iterator<String>
    {
        private LocalDate current;
        private LocalDate stop;
        private long step;
        private String previous;
        private String nextItem;

        public DatesIterator(LocalDate start, LocalDate stop, long step)
        {
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
            Iterator<CharSequence> parts = FormattedDate.this.constants.iterator();

            for (DateTimeFormatter variable : variables)
            {
                result.append(parts.next());
                result.append(date.format(variable));
            }

            while (parts.hasNext())
                result.append(parts.next());

            return result.toString();
        }
    }

    private static final Pattern URL_MACRO_DATE = Pattern.compile("DATE:(.*?)"); //$NON-NLS-1$

    private List<CharSequence> constants;
    private List<DateTimeFormatter> variables;
    private long maxFailedAttempts;
    private Security security;

    public FormattedDate(List<CharSequence> parts) throws IllegalArgumentException // NOSONAR
    {
        boolean isMacro = false;
        constants = new LinkedList<>();
        variables = new LinkedList<>();

        for (CharSequence part : parts)
        {
            if (isMacro)
            {
                Matcher matcher = URL_MACRO_DATE.matcher(part);

                if (!matcher.matches())
                    throw new IllegalArgumentException("Bad macro: " + part); //$NON-NLS-1$

                // throws IllegalArgumentException
                variables.add(DateTimeFormatter.ofPattern(matcher.group(1)));
            }
            else
            {
                constants.add(part);
            }

            isMacro = !isMacro;
        }

        List<String> results = new LinkedList<>();
        (new DatesIterator(LocalDate.of(2016, 1, 1), LocalDate.of(2017, 1, 1), 1)).forEachRemaining(results::add);

        // At most 100 days w/o quotes in the past; assume EOF
        maxFailedAttempts = (long) Math.ceil(100.0d / (366.0d / (double) results.size()));
    }

    @Override
    public void setSecurity(Security security)
    {
        this.security = security;
    }

    @Override
    public long getMaxFailedAttempts()
    {
        return maxFailedAttempts;
    }

    @Override
    public Iterator<String> iterator()
    {
        List<SecurityPrice> prices = security.getPrices();
        LocalDate now = LocalDate.now();

        if (prices.isEmpty())
            return new DatesIterator(now, LocalDate.MIN, -1);
        else
            return new DatesIterator(Collections.max(prices, new SecurityPrice.ByDate()).getDate(), now.plusDays(1), 1);
    }
}
