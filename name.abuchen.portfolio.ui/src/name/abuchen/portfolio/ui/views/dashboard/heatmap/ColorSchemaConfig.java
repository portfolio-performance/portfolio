package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.views.dashboard.WidgetConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;

class ColorSchemaConfig implements WidgetConfig
{
    private final WidgetDelegate delegate;

    private ColorSchema colorSchema = ColorSchema.GREEN_YELLOW_RED;

    public ColorSchemaConfig(WidgetDelegate delegate)
    {
        this.delegate = delegate;

        String code = delegate.getWidget().getConfiguration().get(Dashboard.Config.COLOR_SCHEMA.name());

        if (code != null)
        {
            try
            {
                colorSchema = ColorSchema.valueOf(code);
            }
            catch (IllegalArgumentException ignore)
            {
                PortfolioPlugin.log(ignore);
            }
        }
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        MenuManager subMenu = new MenuManager(Messages.LabelColorSchema);
        manager.add(subMenu);

        subMenu.add(buildAction(ColorSchema.GREEN_YELLOW_RED));
        subMenu.add(buildAction(ColorSchema.GREEN_WHITE_RED));
    }

    private Action buildAction(ColorSchema schema)
    {
        Action action = new SimpleAction(schema.toString(), a -> {
            this.colorSchema = schema;
            delegate.getWidget().getConfiguration().put(Dashboard.Config.COLOR_SCHEMA.name(), schema.name());
            delegate.getClient().markDirty();
        });
        action.setChecked(this.colorSchema == schema);
        return action;
    }

    @Override
    public String getLabel()
    {
        return Messages.LabelColorSchema + ": " + colorSchema.toString(); //$NON-NLS-1$
    }

    public ColorSchema getColorSchema()
    {
        return colorSchema;
    }
}