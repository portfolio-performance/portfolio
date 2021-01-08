package name.abuchen.portfolio.ui.util;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.EditReportingPeriodsDialog;
import name.abuchen.portfolio.ui.dialogs.ReportingPeriodDialog;
import name.abuchen.portfolio.ui.editor.PortfolioPart;

public final class ReportingPeriodDropDown extends DropDown implements IMenuListener
{
    @FunctionalInterface
    public interface ReportingPeriodListener
    {
        void reportingPeriodUpdated();
    }

    private final PortfolioPart part;
    private final ReportingPeriodListener listener;

    private ReportingPeriod selected;
    private List<ReportingPeriod> periods;

    public ReportingPeriodDropDown(final PortfolioPart part, ReportingPeriodListener listener)
    {
        super("x", null, SWT.DROP_DOWN, null); //$NON-NLS-1$
        this.part = part;
        this.listener = Objects.requireNonNull(listener);

        this.selected = part.getSelectedPeriod();
        this.periods = part.getReportingPeriods();

        setMenuListener(this);
        setLabel(selected.toString());
        setToolTip(selected.toInterval(LocalDate.now()).toString());
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        for (ReportingPeriod period : periods)
        {
            Action action = createActionFor(period);
            action.setChecked(period.equals(selected));
            manager.add(action);
        }

        manager.add(new Separator());
        manager.add(new SimpleAction(Messages.LabelReportingAddPeriod, a -> {
            ReportingPeriodDialog dialog = new ReportingPeriodDialog(Display.getDefault().getActiveShell(), selected);

            if (dialog.open() == Window.OK)
            {
                ReportingPeriod period = dialog.getReportingPeriod();

                doSelect(period);

                if (!periods.contains(period))
                    periods.add(period);

                if (listener != null)
                    listener.reportingPeriodUpdated();
            }
        }));

        manager.add(new SimpleAction(Messages.MenuReportingPeriodManage, a -> {
            EditReportingPeriodsDialog dialog = new EditReportingPeriodsDialog(Display.getDefault().getActiveShell());
            dialog.setReportingPeriods(periods);

            if (dialog.open() == Window.OK)
            {
                periods.clear();
                periods.addAll(dialog.getReportingPeriods());

                // make sure at least one entry exists
                if (periods.isEmpty())
                    periods.add(selected);

                if (!periods.contains(selected))
                {
                    doSelect(periods.get(0));
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
        setToolTip(selected.toInterval(LocalDate.now()).toString());
    }

    private Action createActionFor(final ReportingPeriod period)
    {
        return new SimpleAction(period.toString(), a -> {
            doSelect(period);

            if (listener != null)
                listener.reportingPeriodUpdated();
        });
    }

    public ReportingPeriod getSelectedPeriod()
    {
        return selected;
    }
}
