package name.abuchen.portfolio.ui.views.dashboard;

import java.text.MessageFormat;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.SimpleAction;

public class CountConfig implements WidgetConfig
{
    private static final int DEFAULT_COUNT = 3;
    private static final int MAX_COUNT = 10;

    private final WidgetDelegate<?> delegate;
    private int count;

    public CountConfig(WidgetDelegate<?> delegate)
    {
        this.delegate = delegate;

        String code = delegate.getWidget().getConfiguration().get(Dashboard.Config.COUNT.name());
        if (code != null && !code.isEmpty())
        {
            try
            {
                count = Integer.parseInt(code);
            }
            catch (NumberFormatException ignore)
            {
                count = DEFAULT_COUNT;
            }
        }
        else
        {
            count = DEFAULT_COUNT;
        }
    }

    public int getCount()
    {
        return count;
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        var subMenu = new MenuManager(Messages.LabelTopAndBottomCount);

        for (int ii = 1; ii <= MAX_COUNT; ii++)
        {
            int value = ii;
            var action = new SimpleAction(String.valueOf(value), a -> {
                this.count = value;
                delegate.getWidget().getConfiguration().put(Dashboard.Config.COUNT.name(), String.valueOf(value));
                delegate.update();
                delegate.getClient().touch();
            });
            action.setChecked(count == value);
            subMenu.add(action);
        }

        manager.add(subMenu);
    }

    @Override
    public String getLabel()
    {
        return MessageFormat.format(Messages.LabelColonSeparated, Messages.LabelTopAndBottomCount, count);
    }
}
