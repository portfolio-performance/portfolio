package name.abuchen.portfolio.ui.util;

import java.util.LinkedList;
import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.ToolBar;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.EditReportingPeriodsDialog;
import name.abuchen.portfolio.ui.dialogs.ReportingPeriodDialog;
import name.abuchen.portfolio.ui.editor.PortfolioPart;

public final class ReportingPeriodDropDown extends AbstractDropDown
{
    @FunctionalInterface
    public interface ReportingPeriodListener
    {
        void reportingPeriodUpdated();
    }

    private final PortfolioPart part;
    private final ReportingPeriodListener listener;

    private ReportingPeriod selected;
    private LinkedList<ReportingPeriod> periods;

    public ReportingPeriodDropDown(ToolBar toolBar, final PortfolioPart part, ReportingPeriodListener listener)
    {
        super(toolBar, "x"); //$NON-NLS-1$
        this.part = part;
        this.listener = Objects.requireNonNull(listener);

        this.selected = part.getSelectedPeriod();
        this.periods = part.getReportingPeriods();

        getToolItem().setText(selected.toString());
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        Action action = createActionFor(selected);
        action.setChecked(true);
        manager.add(action);

        for (final ReportingPeriod period : periods)
        {
            if (period.equals(selected))
                continue;
            manager.add(createActionFor(period));
        }

        manager.add(new Separator());
        manager.add(new SimpleAction(Messages.LabelReportingAddPeriod, a -> {
            ReportingPeriodDialog dialog = new ReportingPeriodDialog(getToolBar().getShell(), periods.getFirst());

            if (dialog.open() == Dialog.OK)
            {
                ReportingPeriod period = dialog.getReportingPeriod();

                doSelect(period);

                periods.addFirst(period);

                if (listener != null)
                    listener.reportingPeriodUpdated();

                if (periods.size() > 20)
                    periods.removeLast();
            }
        }));

        manager.add(new SimpleAction(Messages.MenuReportingPeriodManage, a -> {
            EditReportingPeriodsDialog dialog = new EditReportingPeriodsDialog(getToolBar().getShell());
            dialog.setReportingPeriods(periods);

            if (dialog.open() == Dialog.OK)
            {
                periods.clear();
                periods.addAll(dialog.getReportingPeriods());

                // make sure at least one entry exists
                if (periods.isEmpty())
                    periods.add(selected);

                if (!selected.equals(periods.getFirst()))
                {
                    doSelect(periods.getFirst());
                    listener.reportingPeriodUpdated();
                }
            }
        }));
    }

    private void doSelect(ReportingPeriod period)
    {
        selected = period;
        part.setSelectedPeriod(period);
        setLabel(period.toString());
    }

    private Action createActionFor(final ReportingPeriod period)
    {
        return new SimpleAction(period.toString(), a -> {
            doSelect(period);

            periods.remove(period);
            periods.addFirst(period);

            if (listener != null)
                listener.reportingPeriodUpdated();
        });
    }

    public ReportingPeriod getSelectedPeriod()
    {
        return selected;
    }

    public LinkedList<ReportingPeriod> getPeriodsXXX()
    {
        return periods;
    }
}
