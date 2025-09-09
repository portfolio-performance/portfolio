package name.abuchen.portfolio.ui.util;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.DateSelectionDialog;
import name.abuchen.portfolio.util.TradeCalendarManager;

public final class TimeMachineDropDown extends DropDown implements IMenuListener
{
    /**
     * The date for which to calculate the statement of assets. If the date is
     * empty, it means using the current date. We do not save the current date
     * here, because some users have the view open more than 24 hours.
     */
    private Optional<LocalDate> snapshotDate = Optional.empty();

    private Consumer<Optional<LocalDate>> updateListener;

    public TimeMachineDropDown(Consumer<Optional<LocalDate>> updateListener)
    {
        super(Messages.LabelPortfolioTimeMachine, Images.CALENDAR_OFF, SWT.NONE);
        this.updateListener = Objects.requireNonNull(updateListener);

        setMenuListener(this);
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        manager.add(new LabelOnly(Values.Date.format(snapshotDate.orElse(LocalDate.now()))));
        manager.add(new Separator());

        addTodayAction(manager);
        addPreviousTradingDayAction(manager);
        addDateSelectionAction(manager);
    }

    private void addTodayAction(IMenuManager manager)
    {
        var action = new SimpleAction(Messages.LabelToday, a -> {
            snapshotDate = Optional.empty();
            updateListener.accept(snapshotDate);
            setImage(Images.CALENDAR_OFF);
        });
        action.setEnabled(snapshotDate.isPresent());
        manager.add(action);
    }

    private void addPreviousTradingDayAction(IMenuManager manager)
    {
        var actionDate = findPreviousTradingDay();

        var action = new SimpleAction(Messages.LabelPreviousTradingDay, a -> {
            snapshotDate = Optional.of(actionDate);
            updateListener.accept(snapshotDate);
            setImage(Images.CALENDAR_ON);
        });

        snapshotDate.ifPresent(date -> action.setEnabled(!date.equals(actionDate)));
        manager.add(action);
    }

    private void addDateSelectionAction(IMenuManager manager)
    {
        manager.add(new SimpleAction(Messages.MenuPickOtherDate, a -> {
            var dialog = new DateSelectionDialog(Display.getDefault().getActiveShell());
            dialog.setSelection(snapshotDate.orElse(LocalDate.now()));
            if (dialog.open() != DateSelectionDialog.OK)
                return;
            if (snapshotDate.isPresent() && snapshotDate.get().equals(dialog.getSelection()))
                return;
            snapshotDate = LocalDate.now().equals(dialog.getSelection()) ? Optional.empty()
                            : Optional.of(dialog.getSelection());
            updateListener.accept(snapshotDate);
            setImage(snapshotDate.isPresent() ? Images.CALENDAR_ON : Images.CALENDAR_OFF);
        }));
    }

    private LocalDate findPreviousTradingDay()
    {
        var date = LocalDate.now().minusDays(1);

        var calendar = TradeCalendarManager.getDefaultInstance();
        while (calendar.isHoliday(date))
        {
            date = date.minusDays(1);
        }

        return date;
    }

    public Optional<LocalDate> getTimeMachineDate()
    {
        return snapshotDate;
    }
}
