package name.abuchen.portfolio.ui.util.viewers;

import java.time.LocalDateTime;
import java.util.function.Function;

import org.eclipse.jface.viewers.ColumnLabelProvider;

import name.abuchen.portfolio.money.Values;

public class DateTimeLabelProvider extends ColumnLabelProvider
{
    private final Function<Object, LocalDateTime> provider;
    private final String alternativeText;

    public DateTimeLabelProvider(Function<Object, LocalDateTime> provider)
    {
        this(provider, null);
    }

    public DateTimeLabelProvider(Function<Object, LocalDateTime> provider, String alternativeText)
    {
        this.provider = provider;
        this.alternativeText = alternativeText;
    }

    public LocalDateTime getValue(Object element)
    {
        return provider.apply(element);
    }

    @Override
    public String getText(Object element)
    {
        LocalDateTime dateTime = provider.apply(element);
        return dateTime != null ? Values.DateTime.format(dateTime) : alternativeText;
    }
}
