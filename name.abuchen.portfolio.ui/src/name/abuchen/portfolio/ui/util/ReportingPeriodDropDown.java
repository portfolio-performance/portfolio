package name.abuchen.portfolio.ui.util;

import java.time.LocalDate;
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
import name.abuchen.portfolio.ui.editor.ReportingPeriods;

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
    private ReportingPeriods periods;

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
        periods.stream().forEach(p -> {
            Action action = createActionFor(p);
            action.setChecked(p.equals(selected));
            manager.add(action);
        });

        manager.add(new Separator());
        manager.add(new SimpleAction(Messages.LabelReportingAddPeriod, a -> {
            ReportingPeriodDialog dialog = new ReportingPeriodDialog(Display.getDefault().getActiveShell(), selected);

            if (dialog.open() == Window.OK)
            {
                ReportingPeriod period = dialog.getReportingPeriod();

                doSelect(period);

                periods.add(period);

                if (listener != null)
                    listener.reportingPeriodUpdated();
            }
        }));

        manager.add(new SimpleAction(Messages.MenuReportingPeriodManage, a -> {
            EditReportingPeriodsDialog dialog = new EditReportingPeriodsDialog(Display.getDefault().getActiveShell());
            dialog.setReportingPeriods(periods.stream());

            if (dialog.open() == Window.OK)
            {
                periods.replaceAll(dialog.getReportingPeriods());

                // make sure at least one entry exists
                if (periods.stream().findAny().isEmpty())
                    periods.add(selected);

                if (!periods.contains(selected))
                {
                    doSelect(periods.stream().findFirst().get());
                    listener.reportingPeriodUpdated();
                }

                part.getClient().touch();
            }
        }));
    }

    public void doSelect(ReportingPeriod period)
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
