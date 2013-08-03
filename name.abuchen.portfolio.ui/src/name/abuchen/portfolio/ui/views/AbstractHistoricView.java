package name.abuchen.portfolio.ui.views;

import java.util.LinkedList;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.ClientEditor;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.ReportingPeriodDialog;
import name.abuchen.portfolio.ui.util.AbstractDropDown;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.ToolBar;

/* package */abstract class AbstractHistoricView extends AbstractFinanceView
{
    public static final String IDENTIFIER = AbstractHistoricView.class.getSimpleName();

    private LinkedList<ReportingPeriod> periods = new LinkedList<ReportingPeriod>();

    @Override
    public void init(ClientEditor clientEditor, Object parameter)
    {
        super.init(clientEditor, parameter);
        periods = getClientEditor().loadReportingPeriods();
    }

    @Override
    public void dispose()
    {
        getClientEditor().storeReportingPeriods(periods);
        super.dispose();
    }

    protected abstract void reportingPeriodUpdated();

    @Override
    protected void addButtons(final ToolBar toolBar)
    {
        new AbstractDropDown(toolBar, periods.getFirst().toString())
        {
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
                            reportingPeriodUpdated();
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
                        ReportingPeriodDialog dialog = new ReportingPeriodDialog(toolBar.getShell(), periods.getFirst());

                        if (dialog.open() == Dialog.OK)
                        {
                            ReportingPeriod period = dialog.getReportingPeriod();
                            periods.addFirst(period);
                            setLabel(period.toString());
                            reportingPeriodUpdated();

                            if (periods.size() > 5)
                                periods.removeLast();
                        }
                    }
                });
            }
        };
    }

    protected final ReportingPeriod getReportingPeriod()
    {
        return periods.getFirst();
    }
}
