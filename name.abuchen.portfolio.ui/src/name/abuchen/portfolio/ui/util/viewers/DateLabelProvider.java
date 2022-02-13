package name.abuchen.portfolio.ui.util.viewers;

import java.time.LocalDate;
import java.util.function.Function;

import org.eclipse.jface.viewers.ColumnLabelProvider;

import name.abuchen.portfolio.money.Values;

public class DateLabelProvider extends ColumnLabelProvider
{
    private final Function<Object, LocalDate> provider;
    private final String alternativeText;

    public DateLabelProvider(Function<Object, LocalDate> provider)
    {
        this(provider, null);
    }

    public DateLabelProvider(Function<Object, LocalDate> provider, String alternativeText)
    {
        this.provider = provider;
        this.alternativeText = alternativeText;
    }

    public LocalDate getValue(Object element)
    {
        return provider.apply(element);
    }

    @Override
    public String getText(Object element)
    {
        LocalDate date = provider.apply(element);
        return date != null ? Values.Date.format(date) : alternativeText;
    }
}
