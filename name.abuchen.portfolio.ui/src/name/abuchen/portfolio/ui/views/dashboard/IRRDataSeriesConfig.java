package name.abuchen.portfolio.ui.views.dashboard;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.SimpleAction;

public class IRRDataSeriesConfig extends DataSeriesConfig
{

    public IRRDataSeriesConfig(WidgetDelegate<?> delegate)
    {
        super(delegate, false, true, true, null, Messages.LabelBenchmarks,
                        Dashboard.Config.SECONDARY_DATA_SERIES);
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        super.menuAboutToShow(manager);
        if (getDataSeries() != null)
        {
            IMenuManager subMenu = manager.findMenuUsingPath(Dashboard.Config.SECONDARY_DATA_SERIES.name());
            subMenu.add(new Separator());
            subMenu.add(new SimpleAction(Messages.MenuReportingPeriodDelete, a -> removeBenchmark()));
        }

    }

    private void removeBenchmark()
    {
        if (getDataSeries() == null)
            return;

        setDataSeries(null);
        getDelegate().getWidget().getConfiguration().remove(Dashboard.Config.SECONDARY_DATA_SERIES.name());
        getDelegate().getWidget().setLabel(WidgetFactory.valueOf(getDelegate().getWidget().getType()).getLabel());
        getDelegate().update();
        getDelegate().getClient().touch();
    }

}
