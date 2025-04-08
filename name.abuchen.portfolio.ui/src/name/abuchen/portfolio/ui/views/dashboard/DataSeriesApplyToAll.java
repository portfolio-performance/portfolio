package name.abuchen.portfolio.ui.views.dashboard;

import java.util.List;
import java.util.stream.Stream;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesSelectionDialog;

public class DataSeriesApplyToAll
{
    private DashboardData dashboardData;

    public DataSeriesApplyToAll(DashboardData dashboardData)
    {
        this.dashboardData = dashboardData;
    }

    public void menuAboutToShow(MenuManager manager, Composite columnControl)
    {
        MenuManager subMenu = new MenuManager(Messages.LabelDataSeries);

        subMenu.add(new SimpleAction(Messages.MenuSelectDataSeries, a -> {
            var showOnlyBenchmark = false;
            Stream<DataSeries> stream = dashboardData.getDataSeriesSet().getAvailableSeries().stream()
                            .filter(ds -> ds.isBenchmark() == showOnlyBenchmark);

            List<DataSeries> list = stream.toList();

            DataSeriesSelectionDialog dialog = new DataSeriesSelectionDialog(Display.getDefault().getActiveShell(),
                            dashboardData.getClient());
            dialog.setElements(list);
            dialog.setMultiSelection(false);

            if (dialog.open() == Window.OK)
            {
                List<DataSeries> result = dialog.getResult();
                if (result.isEmpty())
                    return;

                var dataSeries = result.get(0);

                apply(dataSeries, columnControl);
            }
        }));

        manager.add(subMenu);
    }

    private void apply(DataSeries dataSeries, Composite columnControl)
    {
        for (Control child : columnControl.getChildren())
        {
            @SuppressWarnings("unchecked")
            WidgetDelegate<Object> delegate = (WidgetDelegate<Object>) child.getData(DashboardView.DELEGATE_KEY);
            if (delegate != null)
            {
                delegate.optionallyGet(DataSeriesConfig.class).ifPresent(config -> config.setDataSeries(dataSeries));
            }
        }

        columnControl.update();
    }
}
