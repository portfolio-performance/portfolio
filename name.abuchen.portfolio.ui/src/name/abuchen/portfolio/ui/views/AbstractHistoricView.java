package name.abuchen.portfolio.ui.views;

import java.io.IOException;
import java.util.LinkedList;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.ClientEditor;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dialogs.ReportingPeriodDialog;
import name.abuchen.portfolio.ui.util.AbstractDropDown;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.ToolBar;

/* package */abstract class AbstractHistoricView extends AbstractFinanceView
{
    private static final String IDENTIFIER = AbstractHistoricView.class.getSimpleName();

    private LinkedList<ReportingPeriod> periods = new LinkedList<ReportingPeriod>();

    @Override
    public void init(ClientEditor clientEditor, Object parameter)
    {
        super.init(clientEditor, parameter);
        load();
    }

    @Override
    public void dispose()
    {
        StringBuilder buf = new StringBuilder();
        for (ReportingPeriod p : periods)
        {
            p.writeTo(buf);
            buf.append(';');
        }

        getClientEditor().getPreferenceStore().setValue(IDENTIFIER, buf.toString());
        super.dispose();
    }

    private void load()
    {
        String config = getClientEditor().getPreferenceStore().getString(IDENTIFIER);
        if (config != null && config.trim().length() > 0)
        {
            String[] codes = config.split(";"); //$NON-NLS-1$
            for (String c : codes)
            {
                try
                {
                    periods.add(ReportingPeriod.from(c));
                }
                catch (IOException ignore)
                {
                    PortfolioPlugin.log(ignore);
                }
            }
        }

        if (periods.isEmpty())
        {
            for (int ii = 1; ii <= 5; ii++)
                periods.add(new ReportingPeriod.LastX(ii, 0));
        }
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
