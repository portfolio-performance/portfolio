package name.abuchen.portfolio.ui.views.dashboard;

import java.io.IOException;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dialogs.ReportingPeriodDialog;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;

public class ReportingPeriodConfig implements WidgetConfig
{
    private final WidgetDelegate delegate;

    private ReportingPeriod reportingPeriod;

    public ReportingPeriodConfig(WidgetDelegate delegate)
    {
        this.delegate = delegate;

        String code = delegate.getWidget().getConfiguration().get(Dashboard.Config.REPORTING_PERIOD.name());

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

    public ReportingPeriod getReportingPeriod()
    {
        return reportingPeriod != null ? reportingPeriod : delegate.getDashboardData().getDefaultReportingPeriod();
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME, new LabelOnly(getReportingPeriod().toString()));

        MenuManager subMenu = new MenuManager("Berichtszeitraum");

        subMenu.add(new LabelOnly(reportingPeriod != null ? getReportingPeriod().toString() : "<dashboard default>"));
        subMenu.add(new Separator());

        subMenu.add(new SimpleAction("Use Dashboard default", a -> {
            reportingPeriod = null;
            delegate.getWidget().getConfiguration().remove(Dashboard.Config.REPORTING_PERIOD.name());
            delegate.getClient().markDirty();
        }));

        delegate.getDashboardData().getDefaultReportingPeriods().stream()
                        .forEach(p -> subMenu.add(new SimpleAction(p.toString(), a -> {
                            reportingPeriod = p;
                            delegate.getWidget().getConfiguration().put(Dashboard.Config.REPORTING_PERIOD.name(),
                                            p.getCode());
                            delegate.getClient().markDirty();
                        })));
        subMenu.add(new Separator());

        subMenu.add(new SimpleAction("Neu...", a -> {
            ReportingPeriodDialog dialog = new ReportingPeriodDialog(Display.getDefault().getActiveShell(),
                            getReportingPeriod());
            if (dialog.open() == ReportingPeriodDialog.OK)
            {
                reportingPeriod = dialog.getReportingPeriod();
                delegate.getWidget().getConfiguration().put(Dashboard.Config.REPORTING_PERIOD.name(),
                                reportingPeriod.getCode());
                delegate.getClient().markDirty();
            }
        }));

        manager.add(subMenu);
    }

}
