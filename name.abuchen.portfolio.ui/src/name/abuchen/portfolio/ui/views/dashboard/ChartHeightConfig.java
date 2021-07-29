package name.abuchen.portfolio.ui.views.dashboard;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;

public class ChartHeightConfig implements WidgetConfig
{
    public enum Height
    {
        SMALLER(70, Messages.LabelSmallerSize), //
        NORMAL(140, Messages.LabelNormalSize), //
        BIGGER(210, Messages.LabelBiggerSize), //
        TWOX(280, "2X"), //$NON-NLS-1$
        THREEX(420, "3X"), //$NON-NLS-1$
        FOURX(560, "4X"), //$NON-NLS-1$
        FIVEX(700, "5X"); //$NON-NLS-1$

        private int pixel;
        private String label;

        private Height(int pixel, String label)
        {
            this.pixel = pixel;
            this.label = label;
        }
    }

    private final WidgetDelegate<?> delegate;

    private Height height = Height.NORMAL;

    public ChartHeightConfig(WidgetDelegate<?> delegate)
    {
        this.delegate = delegate;

        String code = delegate.getWidget().getConfiguration().get(Dashboard.Config.HEIGHT.name());

        if (code != null)
        {
            try
            {
                int pixel = Integer.parseInt(code);

                for (Height h : Height.values())
                {
                    if (pixel == h.pixel)
                    {
                        this.height = h;
                        break;
                    }
                }
            }
            catch (NumberFormatException e)
            {
                // ignore -> use the default height
            }
        }
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME, new LabelOnly(getLabel()));

        MenuManager subMenu = new MenuManager(Messages.ColumnHeight);
        manager.add(subMenu);

        for (Height h : Height.values())
        {
            SimpleAction action = new SimpleAction(h.label, a -> {

                this.height = h;

                delegate.getWidget().getConfiguration().put(Dashboard.Config.HEIGHT.name(), String.valueOf(h.pixel));

                delegate.update();
                delegate.getClient().touch();
            });
            action.setChecked(h == this.height);
            subMenu.add(action);
        }
    }

    @Override
    public String getLabel()
    {
        return Messages.ColumnHeight + ": " + height.label; //$NON-NLS-1$
    }

    public int getPixel()
    {
        return height.pixel;
    }
}
