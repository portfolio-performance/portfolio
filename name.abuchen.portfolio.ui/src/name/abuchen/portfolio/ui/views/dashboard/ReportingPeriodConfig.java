package name.abuchen.portfolio.ui.views.dashboard;

import java.io.IOException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dialogs.ReportingPeriodDialog;

public class ReportingPeriodConfig
{
    private static final String CONFIG_PERIOD = "period"; //$NON-NLS-1$

    private final Dashboard.Widget widget;
    private ReportingPeriod reportingPeriod;

    public ReportingPeriodConfig(Dashboard.Widget widget)
    {
        this.widget = widget;

        String config = widget.getConfiguration().get(CONFIG_PERIOD);
        if (config == null || config.isEmpty())
            config = "L1Y0"; //$NON-NLS-1$

        try
        {
            this.reportingPeriod = ReportingPeriod.from(config);
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            this.reportingPeriod = new ReportingPeriod.LastX(1, 0);
        }
    }

    public ReportingPeriod getReportingPeriod()
    {
        return reportingPeriod;
    }

    public void menuAboutToShow(MenuManager manager)
    {
        manager.add(new Action("Berichtszeitraum")
        {
            @Override
            public void run()
            {
                ReportingPeriodDialog dialog = new ReportingPeriodDialog(Display.getDefault().getActiveShell(),
                                reportingPeriod);

                if (dialog.open() == Dialog.OK)
                {
                    reportingPeriod = dialog.getReportingPeriod();
                    widget.getConfiguration().put(CONFIG_PERIOD, reportingPeriod.getCode());
                }
            }
        });
    }

}
