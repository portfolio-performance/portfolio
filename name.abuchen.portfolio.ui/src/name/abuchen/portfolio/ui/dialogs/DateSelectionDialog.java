package name.abuchen.portfolio.ui.dialogs;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Predicate;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.DatePicker;

public class DateSelectionDialog extends Dialog
{
    private LocalDate selection = LocalDate.now();
    private Predicate<LocalDate> validator;

    public DateSelectionDialog(Shell parentShell)
    {
        // allow all dates
        this(parentShell, (LocalDate date) -> {
            return true;
        });
    }

    public DateSelectionDialog(Shell parentShell, Predicate<LocalDate> validator)
    {
        super(parentShell);

        this.validator = Objects.requireNonNull(validator);
    }

    public LocalDate getSelection()
    {
        return selection;
    }

    public void setSelection(LocalDate selection)
    {
        this.selection = selection;
    }

    @Override
    protected void configureShell(Shell shell)
    {
        super.configureShell(shell);
        shell.setText(Messages.DialogTitlePickDate);
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite container = (Composite) super.createDialogArea(parent);

        var datePicker = DatePicker.withoutDropDown(container);
        datePicker.setSelection(selection);
        GridDataFactory.fillDefaults().grab(true, false).align(SWT.CENTER, SWT.CENTER)
                        .applyTo(datePicker.getControl());

        // DateTime widget has zero-based months
        var calendar = new DateTime(container, SWT.CALENDAR | SWT.BORDER);
        calendar.setDate(selection.getYear(), selection.getMonthValue() - 1, selection.getDayOfMonth());
        GridDataFactory.fillDefaults().grab(true, true).align(SWT.CENTER, SWT.FILL).applyTo(calendar);

        // neither DateTime#setDate nor CDateTime#setSelection fires a selection
        // event, i.e. keeping the two widgets in sync does not loop back. Each
        // listener updates only the other widget: writing back into the source
        // widget would reset the caret while the user is still typing

        datePicker.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            LocalDate date = datePicker.getSelection();
            calendar.setDate(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
            select(date);
        }));

        calendar.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            LocalDate date = LocalDate.of(calendar.getYear(), calendar.getMonth() + 1, calendar.getDay());
            datePicker.setSelection(date);
            select(date);
        }));

        return container;
    }

    private void select(LocalDate date)
    {
        this.selection = date;
        getButton(OK).setEnabled(validator.test(date));
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);

        getButton(OK).setEnabled(validator.test(selection));
    }
}
