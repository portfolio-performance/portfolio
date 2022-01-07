package name.abuchen.portfolio.ui.views.dashboard;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.ReportingPeriodDialog;
import name.abuchen.portfolio.ui.util.SimpleAction;

public class ReportingPeriodApplyToAll
{
    private DashboardData dashboardData;

    public ReportingPeriodApplyToAll(DashboardData dashboardData)
    {
        this.dashboardData = dashboardData;
    }

    public void menuAboutToShow(MenuManager manager, Composite columnControl)
    {
        MenuManager subMenu = new MenuManager(Messages.LabelReportingPeriod);

        subMenu.add(new SimpleAction(Messages.MenuUseDashboardDefaultReportingPeriod, a -> apply(null, columnControl)));

        dashboardData.getDefaultReportingPeriods().stream()
                        .forEach(p -> subMenu.add(new SimpleAction(p.toString(), a -> apply(p, columnControl))));
        subMenu.add(new Separator());

        subMenu.add(new SimpleAction(Messages.LabelReportingAddPeriod, a -> {
            ReportingPeriodDialog dialog = new ReportingPeriodDialog(Display.getDefault().getActiveShell(),
                            dashboardData.getDefaultReportingPeriod());
            if (dialog.open() == ReportingPeriodDialog.OK)
            {
                ReportingPeriod reportingPeriod = dialog.getReportingPeriod();

                if (!dashboardData.getDefaultReportingPeriods().contains(reportingPeriod))
                    dashboardData.getDefaultReportingPeriods().add(reportingPeriod);

                apply(reportingPeriod, columnControl);
            }
        }));

        manager.add(subMenu);
    }

    private void apply(ReportingPeriod reportingPeriod, Composite columnControl)
    {
        for (Control child : columnControl.getChildren())
        {
            @SuppressWarnings("unchecked")
            WidgetDelegate<Object> delegate = (WidgetDelegate<Object>) child.getData(DashboardView.DELEGATE_KEY);
            if (delegate != null)
            {
                delegate.optionallyGet(ReportingPeriodConfig.class)
                                .ifPresent(config -> config.setReportingPeriod(reportingPeriod));
            }
        }
    }
}
