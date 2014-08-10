package name.abuchen.portfolio.ui.util;

import java.util.LinkedList;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPart;
import name.abuchen.portfolio.ui.dialogs.ReportingPeriodDialog;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.ToolBar;

public final class ReportingPeriodDropDown extends AbstractDropDown
{
    public interface ReportingPeriodListener
    {
        void reportingPeriodUpdated();
    }

    private ReportingPeriodListener listener;
    // TODO: move reporting periods to kepler e4 application model?
    private LinkedList<ReportingPeriod> periods = new LinkedList<ReportingPeriod>();

    public ReportingPeriodDropDown(ToolBar toolBar, final PortfolioPart part, ReportingPeriodListener listener)
    {
        super(toolBar, "x"); //$NON-NLS-1$
        this.periods = part.loadReportingPeriods();
        this.listener = listener;

        getToolItem().setText(periods.getFirst().toString());

        toolBar.addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                part.storeReportingPeriods(periods);
            }
        });
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        boolean isFirst = true;
        for (final ReportingPeriod period : periods)
        {
            Action action = new Action(period.toString())
            {
                @Override
                public void run()
                {
                    periods.remove(period);
                    periods.addFirst(period);
                    setLabel(period.toString());

                    if (listener != null)
                        listener.reportingPeriodUpdated();
                }
            };
            if (isFirst)
                action.setChecked(true);
            isFirst = false;

            manager.add(action);
        }

        manager.add(new Separator());
        manager.add(new Action(Messages.LabelReportingAddPeriod)
        {
            @Override
            public void run()
            {
                ReportingPeriodDialog dialog = new ReportingPeriodDialog(getToolBar().getShell(), periods.getFirst());

                if (dialog.open() == Dialog.OK)
                {
                    ReportingPeriod period = dialog.getReportingPeriod();
                    periods.addFirst(period);
                    setLabel(period.toString());

                    if (listener != null)
                        listener.reportingPeriodUpdated();

                    if (periods.size() > 5)
                        periods.removeLast();
                }
            }
        });
    }

    public LinkedList<ReportingPeriod> getPeriods()
    {
        return periods;
    }
}
