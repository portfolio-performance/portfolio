package name.abuchen.portfolio.ui.views.dashboard;

import org.eclipse.jface.action.IMenuManager;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.SimpleAction;

public class ColoredValueConfig implements WidgetConfig
{
    private final WidgetDelegate<?> delegate;
    private boolean isValueColored = true;

    public ColoredValueConfig(WidgetDelegate<?> delegate, boolean defaultValue)
    {
        this.delegate = delegate;

        String code = delegate.getWidget().getConfiguration().get(Dashboard.Config.FLAG_COLORED_VALUE.name());
        this.isValueColored = code != null ? Boolean.parseBoolean(code) : defaultValue;
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        SimpleAction action = new SimpleAction(Messages.LabelColorValue, a -> {
            this.isValueColored = !this.isValueColored;

            delegate.getWidget().getConfiguration().put(Dashboard.Config.FLAG_COLORED_VALUE.name(),
                            String.valueOf(this.isValueColored));

            delegate.update();
            delegate.getClient().touch();
        });

        action.setChecked(this.isValueColored);
        manager.add(action);
    }

    @Override
    public String getLabel()
    {
        return Messages.LabelColorValue + ": " + (this.isValueColored ? Messages.LabelYes : Messages.LabelNo); //$NON-NLS-1$
    }

    public boolean isValueColored()
    {
        return this.isValueColored;
    }
}
