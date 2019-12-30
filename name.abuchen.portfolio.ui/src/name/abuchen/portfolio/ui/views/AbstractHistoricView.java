package name.abuchen.portfolio.ui.views;

import org.eclipse.jface.action.ToolBarManager;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.ReportingPeriodDropDown;
import name.abuchen.portfolio.ui.util.ReportingPeriodDropDown.ReportingPeriodListener;

public abstract class AbstractHistoricView extends AbstractFinanceView implements ReportingPeriodListener
{
    private ReportingPeriodDropDown dropDown;

    @Override
    protected void addButtons(final ToolBarManager toolBarManager)
    {
        dropDown = new ReportingPeriodDropDown(getPart(), this);
        toolBarManager.add(dropDown);
    }

    protected final ReportingPeriod getReportingPeriod()
    {
        return dropDown.getSelectedPeriod();
    }
}
