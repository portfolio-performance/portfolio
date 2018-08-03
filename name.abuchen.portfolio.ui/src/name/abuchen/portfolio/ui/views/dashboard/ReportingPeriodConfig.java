package name.abuchen.portfolio.ui.views.dashboard;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dialogs.ReportingPeriodDialog;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;

public class ReportingPeriodConfig implements WidgetConfig
{
    private final WidgetDelegate<?> delegate;
    private DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG);

    private ReportingPeriod reportingPeriod;

    public ReportingPeriodConfig(WidgetDelegate<?> delegate)
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

        MenuManager subMenu = new MenuManager(Messages.LabelReportingPeriod);

        subMenu.add(new LabelOnly(reportingPeriod != null ? getReportingPeriod().toString()
                        : Messages.LabelUsingDashboardDefaultReportingPeriod));
        subMenu.add(new Separator());

        subMenu.add(new SimpleAction(Messages.MenuUseDashboardDefaultReportingPeriod, a -> {
            reportingPeriod = null;
            delegate.getWidget().getConfiguration().remove(Dashboard.Config.REPORTING_PERIOD.name());

            delegate.update();
            delegate.markDirty();
        }));

        delegate.getDashboardData().getDefaultReportingPeriods().stream()
                        .forEach(p -> subMenu.add(new SimpleAction(p.toString(), a -> {
                            reportingPeriod = p;
                            delegate.getWidget().getConfiguration().put(Dashboard.Config.REPORTING_PERIOD.name(),
                                            p.getCode());

                            delegate.update();
                            delegate.markDirty();
                        })));
        subMenu.add(new Separator());

        subMenu.add(new SimpleAction(Messages.LabelReportingAddPeriod, a -> {
            ReportingPeriodDialog dialog = new ReportingPeriodDialog(Display.getDefault().getActiveShell(),
                            getReportingPeriod());
            if (dialog.open() == ReportingPeriodDialog.OK)
            {
                reportingPeriod = dialog.getReportingPeriod();

                if (!delegate.getDashboardData().getDefaultReportingPeriods().contains(reportingPeriod))
                    delegate.getDashboardData().getDefaultReportingPeriods().add(reportingPeriod);

                delegate.getWidget().getConfiguration().put(Dashboard.Config.REPORTING_PERIOD.name(),
                                reportingPeriod.getCode());

                delegate.update();
                delegate.markDirty();
            }
        }));

        manager.add(subMenu);
    }

    @Override
    public String getLabel()
    {
        StringBuilder label = new StringBuilder();
        label.append(Messages.LabelReportingPeriod).append(": "); //$NON-NLS-1$
        label.append(getReportingPeriod().toString());
        label.append(" (").append(formatter.format(getReportingPeriod().getStartDate())) //$NON-NLS-1$
                        .append(" - ") //$NON-NLS-1$
                        .append(formatter.format(getReportingPeriod().getEndDate())).append(")"); //$NON-NLS-1$
        if (reportingPeriod == null)
            label.append(" | ").append(Messages.LabelUsingDashboardDefaultReportingPeriod); //$NON-NLS-1$
        return label.toString();
    }
}
