package name.abuchen.portfolio.ui.util.viewers;

import java.util.Optional;
import java.util.function.Function;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.trail.Trail;
import name.abuchen.portfolio.snapshot.trail.TrailProvider;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.util.Colors;

public final class MoneyColorLabelProvider extends ColumnLabelProvider
{
    private final Function<Object, Money> valueProvider;
    private final Function<Object, String> toolTipProvider;
    private final Client client;

    public MoneyColorLabelProvider(Function<Object, Money> valueProvider, Client client)
    {
        this(valueProvider, null, client);
    }

    public MoneyColorLabelProvider(Function<Object, Money> valueProvider, Function<Object, String> toolTipProvider,
                    Client client)
    {
        this.valueProvider = valueProvider;
        this.toolTipProvider = toolTipProvider;
        this.client = client;
    }

    @Override
    public Color getForeground(Object element)
    {
        Money money = valueProvider.apply(element);
        if (money == null || money.isZero())
            return null;

        return money.getAmount() >= 0 ? Colors.theme().greenForeground() : Colors.theme().redForeground();
    }

    @Override
    public Image getImage(Object element)
    {
        Money money = valueProvider.apply(element);
        if (money == null || money.isZero())
            return null;

        return money.getAmount() >= 0 ? Images.GREEN_ARROW.image() : Images.RED_ARROW.image();
    }

    @Override
    public String getText(Object element)
    {
        Money money = valueProvider.apply(element);
        if (money == null)
            return null;

        return Values.Money.format(money, client.getBaseCurrency());
    }

    @Override
    public String getToolTipText(Object element)
    {
        if (toolTipProvider == null)
            return null;

        String tooltip = toolTipProvider.apply(element);

        if (!(element instanceof TrailProvider))
            return tooltip;

        TrailProvider t = (TrailProvider) element;
        Optional<Trail> trail = t.explain(tooltip);

        if (!trail.isPresent() || trail.get().getRecord().isEmpty())
            return null;
        else
            return tooltip;
    }
}
