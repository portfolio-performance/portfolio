package name.abuchen.portfolio.ui.util.viewers;

import java.util.function.Function;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;

public final class MoneyColorLabelProvider extends ColumnLabelProvider
{
    private final Function<Object, Money> provider;
    private final Client client;

    public MoneyColorLabelProvider(Function<Object, Money> provider, Client client)
    {
        this.provider = provider;
        this.client = client;
    }

    @Override
    public Color getForeground(Object element)
    {
        Money money = provider.apply(element);
        if (money == null)
            return null;

        return Display.getCurrent().getSystemColor(money.getAmount() >= 0 ? SWT.COLOR_DARK_GREEN : SWT.COLOR_DARK_RED);
    }

    @Override
    public Image getImage(Object element)
    {
        Money money = provider.apply(element);
        if (money == null)
            return null;

        return money.getAmount() >= 0 ? Images.GREEN_ARROW.image() : Images.RED_ARROW.image();
    }

    @Override
    public String getText(Object element)
    {
        Money money = provider.apply(element);
        if (money == null)
            return null;

        return Values.Money.format(money, client.getBaseCurrency());
    }
}
