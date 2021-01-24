package name.abuchen.portfolio.ui.views.columns;

import java.util.function.Function;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.viewers.Column;

public class CalculatedQuoteColumn extends Column
{
    public CalculatedQuoteColumn(String id, Client client, Function<Object, Quote> quoteProvider)
    {
        this(id, client, quoteProvider, null);
    }

    public CalculatedQuoteColumn(String id, Client client, Function<Object, Quote> quoteProvider,
                    Function<Object, Color> foregroundColorProvider)
    {
        super(id, Messages.ColumnPerShare, SWT.RIGHT, 80);

        setDescription(Messages.ColumnPerShare_Description);
        setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                Quote quote = quoteProvider.apply(e);
                return quote != null ? Values.CalculatedQuote.format(quote, client.getBaseCurrency()) : null;
            }

            @Override
            public String getToolTipText(Object e)
            {
                Quote quote = quoteProvider.apply(e);
                return quote != null ? Values.Quote.format(quote, client.getBaseCurrency()) : null;
            }

            @Override
            public Color getForeground(Object element)
            {
                return foregroundColorProvider != null ? foregroundColorProvider.apply(element) : null;
            }
        });
    }

}
