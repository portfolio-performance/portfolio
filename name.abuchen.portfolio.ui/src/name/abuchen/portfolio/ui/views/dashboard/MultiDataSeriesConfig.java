package name.abuchen.portfolio.ui.views.dashboard;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.views.dataseries.BasicDataSeriesConfigurator;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesSerializer;

public class MultiDataSeriesConfig implements WidgetConfig
{
    private final BasicDataSeriesConfigurator configurator;

    public MultiDataSeriesConfig(WidgetDelegate delegate)
    {
        this.configurator = new BasicDataSeriesConfigurator(delegate.getClient(),
                        delegate.getDashboardData().getDataSeriesSet());

        String selected = delegate.getWidget().getConfiguration().get(Dashboard.Config.DATA_SERIES.name());
        if (selected != null && !selected.isEmpty())
            configurator.setSelectedData(selected);

        configurator.addListener(() -> {
            delegate.getWidget().getConfiguration().put(Dashboard.Config.DATA_SERIES.name(),
                            new DataSeriesSerializer().toString(configurator.getSelectedDataSeries()));

            delegate.getClient().markDirty();
        });
    }

    public List<DataSeries> getDataSeries()
    {
        return configurator.getSelectedDataSeries();
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME, new LabelOnly(MessageFormat
                        .format(Messages.LabelNumberDataSeries, configurator.getSelectedDataSeries().size())));

        MenuManager subMenu = new MenuManager(Messages.LabelDataSeries);
        configurator.configMenuAboutToShow(subMenu);
        manager.add(subMenu);
    }

    @Override
    public String getLabel()
    {
        return Messages.LabelDataSeries + ": " + MessageFormat.format(Messages.LabelNumberDataSeries, //$NON-NLS-1$
                        configurator.getSelectedDataSeries().size());
    }
}
