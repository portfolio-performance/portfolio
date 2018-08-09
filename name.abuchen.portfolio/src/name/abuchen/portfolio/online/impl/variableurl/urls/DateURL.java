package name.abuchen.portfolio.online.impl.variableurl.urls;

import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.online.impl.variableurl.iterators.DateIterator;
import name.abuchen.portfolio.online.impl.variableurl.macros.Macro;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class DateURL extends BaseURL
{
    private long maxFailedAttempts;

    public DateURL(List<Macro> macros)
    {
        super(macros);

        List<String> results = new LinkedList<>();
        (new DateIterator(this, LocalDate.of(2016, 1, 1), LocalDate.of(2017, 1, 1), 1)).forEachRemaining(results::add);

        // At most 100 days w/o quotes in the past; assume EOF
        maxFailedAttempts = (long) Math.ceil(100.0d / (366.0d / (double) results.size()));
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
            return new DateIterator(this, now, LocalDate.MIN, -1);
        else
            return new DateIterator(this, Collections.max(prices, new SecurityPrice.ByDate()).getDate(), now.plusDays(1), 1);
    }
}
