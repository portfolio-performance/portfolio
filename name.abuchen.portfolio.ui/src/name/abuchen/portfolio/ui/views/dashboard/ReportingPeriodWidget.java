package name.abuchen.portfolio.ui.views.dashboard;

import java.io.IOException;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dialogs.ReportingPeriodDialog;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;

public abstract class ReportingPeriodWidget extends WidgetDelegate
{
    private ReportingPeriod reportingPeriod;

    public ReportingPeriodWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        String code = widget.getConfiguration().get(Dashboard.Config.REPORTING_PERIOD.name());

        try
        {
            if (code != null && !code.isEmpty())
                reportingPeriod = ReportingPeriod.from(code);
        }
        catch (IOException ignore)
        {
            PortfolioPlugin.log(ignore);
        }
    }

    protected ReportingPeriod getReportingPeriod()
    {
        return reportingPeriod != null ? reportingPeriod : getDashboardData().getDefaultReportingPeriod();
    }

    @Override
    public void configMenuAboutToShow(IMenuManager manager)
    {
        MenuManager subMenu = new MenuManager("Berichtszeitraum");

        subMenu.add(new LabelOnly(reportingPeriod != null ? getReportingPeriod().toString() : "<dashboard default>"));
        subMenu.add(new Separator());

        subMenu.add(new SimpleAction("Use Dashboard default", a -> {
            reportingPeriod = null;
            getWidget().getConfiguration().remove(Dashboard.Config.REPORTING_PERIOD.name());
            getClient().markDirty();
        }));

        getDashboardData().getDefaultReportingPeriods().stream()
                        .forEach(p -> subMenu.add(new SimpleAction(p.toString(), a -> {
                            reportingPeriod = p;
                            getWidget().getConfiguration().put(Dashboard.Config.REPORTING_PERIOD.name(), p.getCode());
                            getClient().markDirty();
                        })));
        subMenu.add(new Separator());

        subMenu.add(new SimpleAction("Neu...", a -> {
            ReportingPeriodDialog dialog = new ReportingPeriodDialog(Display.getDefault().getActiveShell(),
                            getReportingPeriod());
            if (dialog.open() == ReportingPeriodDialog.OK)
            {
                reportingPeriod = dialog.getReportingPeriod();
                getWidget().getConfiguration().put(Dashboard.Config.REPORTING_PERIOD.name(), reportingPeriod.getCode());
                getClient().markDirty();
            }
        }));

        manager.add(subMenu);
    }
}
