package name.abuchen.portfolio.ui.util.viewers;

import java.util.function.Function;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.util.Colors;

public final class MoneyColorLabelProvider extends ColumnLabelProvider
{
    private final Function<Object, Money> valueProvider;
    private final Client client;

    public MoneyColorLabelProvider(Function<Object, Money> valueProvider, Client client)
    {
        this.valueProvider = valueProvider;
        this.client = client;
    }

    @Override
    public Color getForeground(Object element)
    {
        Money money = valueProvider.apply(element);
        return MoneyColorLabelProvider.getForeground(money);
    }

    public static Color getForeground(Money money)
    {
        if (money == null || money.isZero())
            return null;

        return money.getAmount() >= 0 ? Colors.theme().positiveForeground() : Colors.theme().negativeForeground();
    }

    @Override
    public Image getImage(Object element)
    {
        Money money = valueProvider.apply(element);
        if (money == null || money.isZero())
            return null;

        if (money.getAmount() >= 0)
            return Colors.theme().useGreenPositive() ? Images.GREEN_UP_ARROW.image() : Images.RED_UP_ARROW.image();
        else
            return Colors.theme().useGreenPositive() ? Images.RED_DOWN_ARROW.image() : Images.GREEN_DOWN_ARROW.image();
    }

    @Override
    public String getText(Object element)
    {
        Money money = valueProvider.apply(element);
        if (money == null)
            return null;

        return Values.Money.format(money, client.getBaseCurrency());
    }

}
