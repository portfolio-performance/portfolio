package name.abuchen.portfolio.ui.util.viewers;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.ViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.util.FormatHelper;

public abstract class QuotesLabelProvider extends OwnerDrawLabelProvider
{
    private final Client client;

    private ColumnViewer viewer;
    private TextLayout cachedTextLayout;

    public QuotesLabelProvider(Client client)
    {
        this.client = client;
    }

    public Color getForeground(Object element)
    {
        return null;
    }

    @Override
    public String getToolTipText(Object element)
    {
        Quote quote = getQuote(element);
        if (quote == null)
            return null;

        return quote.formatFull();
    }

    protected abstract Quote getQuote(Object element);

    private TextLayout getSharedTextLayout(Display display)
    {
        if (this.cachedTextLayout == null)
        {
            int orientation = this.viewer.getControl().getStyle() & (SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT);
            this.cachedTextLayout = new TextLayout(display);
            this.cachedTextLayout.setOrientation(orientation);
        }
        return this.cachedTextLayout;
    }

    @Override
    protected void initialize(ColumnViewer viewer, ViewerColumn column)
    {
        this.viewer = viewer;
        super.initialize(viewer, column);
    }

    private Rectangle getBounds(Widget widget, int index)
    {
        if (widget instanceof TableItem)
            return ((TableItem) widget).getBounds(index);
        else if (widget instanceof TreeItem)
            return ((TreeItem) widget).getBounds(index);
        else
            throw new IllegalArgumentException();
    }

    private Rectangle getSize(Event event, String text)
    {
        StringBuilder builder = new StringBuilder(text);
        builder.append(' ');

        TextLayout textLayout = getSharedTextLayout(event.display);
        textLayout.setText(builder.toString());

        return textLayout.getBounds();
    }

    @Override
    protected void measure(Event event, Object element)
    {
        Quote quote = getQuote(element);
        if (quote != null)
        {
            String text = quote.format(client.getBaseCurrency());
            Rectangle size = getSize(event, text);
            event.setBounds(new Rectangle(event.x, event.y, size.width, event.height));
        }
    }

    @Override
    public void paint(Event event, Object element)
    {
        Rectangle tableItem = getBounds(event.item, event.index);
        boolean isSelected = (event.detail & SWT.SELECTED) != 0 || (event.detail & SWT.HOT) != 0;

        Quote quote = getQuote(element);
        if (quote == null)
            return;

        Color oldForeground = null;
        Color newForeground = isSelected ? null : getForeground(element);
        if (newForeground != null)
        {
            oldForeground = event.gc.getForeground();
            event.gc.setForeground(newForeground);
        }

        String text = quote.format(client.getBaseCurrency());
        Rectangle size = getSize(event, text);

        TextLayout textLayout = getSharedTextLayout(event.display);
        textLayout.setText(text);

        Rectangle layoutBounds = textLayout.getBounds();
        int x = event.x + tableItem.width - Math.min(size.width, tableItem.width);
        int y = event.y + Math.max(0, (tableItem.height - layoutBounds.height) / 2);

        textLayout.draw(event.gc, x, y);

        if (oldForeground != null)
            event.gc.setForeground(oldForeground);
    }

    @Override
    protected void erase(Event event, Object element)
    {
        // use os-specific background
    }

    public static class Quote // with optional currency
    {
        private final String currency;
        private final long amount;

        private Quote(String currency, long amount)
        {
            this.currency = currency;
            this.amount = amount;
        }

        public static Quote of(name.abuchen.portfolio.money.Quote quote)
        {
            if (quote == null)
                return null;

            return new Quote(quote.getCurrencyCode(), quote.getAmount());
        }

        public static Quote of(String currency, long amount)
        {
            return new Quote(currency, amount);
        }

        public static Quote of(long amount)
        {
            return new Quote(null, amount);
        }

        private String formatFull()
        {
            if (currency == null)
                return Values.Quote.format(amount);
            else
                return Values.Quote.format(currency, amount);
        }

        private String format(String skipCurrency)
        {
            return FormatHelper.format(currency, amount, skipCurrency);
        }
    }
}
