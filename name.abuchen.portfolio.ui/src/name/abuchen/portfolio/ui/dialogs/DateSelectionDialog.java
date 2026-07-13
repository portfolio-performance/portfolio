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

        var datePicker = new DatePicker(container, false);
        datePicker.setSelection(selection);
        GridDataFactory.fillDefaults().grab(true, false).align(SWT.CENTER, SWT.CENTER)
                        .applyTo(datePicker.getControl());

        var calendar = new DateTime(container, SWT.CALENDAR | SWT.BORDER);
        calendar.setDate(selection.getYear(), selection.getMonthValue() - 1, selection.getDayOfMonth());

        datePicker.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            selection = datePicker.getSelection();
            // DateTime widget has zero-based months
            calendar.setDate(selection.getYear(), selection.getMonthValue() - 1, selection.getDayOfMonth());
            DateSelectionDialog.this.getButton(OK).setEnabled(validator.test(selection));
        }));

        calendar.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            // DateTime widget has zero-based months
            selection = LocalDate.of(calendar.getYear(), calendar.getMonth() + 1, calendar.getDay());
            datePicker.setSelection(selection);
            DateSelectionDialog.this.getButton(OK).setEnabled(validator.test(selection));
        }));

        GridDataFactory.fillDefaults().grab(true, true).align(SWT.CENTER, SWT.FILL).applyTo(calendar);

        return container;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);

        getButton(OK).setEnabled(validator.test(selection));
    }
}
