package name.abuchen.portfolio.ui.views;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.ReportingPeriodDropDown;
import name.abuchen.portfolio.ui.util.ReportingPeriodDropDown.ReportingPeriodListener;

import org.eclipse.swt.widgets.ToolBar;

/* package */abstract class AbstractHistoricView extends AbstractFinanceView implements ReportingPeriodListener
{
    private ReportingPeriodDropDown dropDown;

    @Override
    protected void addButtons(final ToolBar toolBar)
    {
        dropDown = new ReportingPeriodDropDown(toolBar, getClientEditor(), this);
    }

    protected final ReportingPeriod getReportingPeriod()
    {
        return dropDown.getPeriods().getFirst();
    }
}
