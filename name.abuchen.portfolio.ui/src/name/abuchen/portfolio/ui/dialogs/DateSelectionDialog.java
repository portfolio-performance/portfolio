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

        DateTime dateTime = new DateTime(container, SWT.CALENDAR | SWT.BORDER);
        dateTime.setDate(selection.getYear(), selection.getMonthValue() - 1, selection.getDayOfMonth());
        dateTime.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            // DateTime widget has zero-based months
            selection = LocalDate.of(dateTime.getYear(), dateTime.getMonth() + 1, dateTime.getDay());
            DateSelectionDialog.this.getButton(OK).setEnabled(validator.test(selection));
        }));
        GridDataFactory.fillDefaults().grab(true, true).align(SWT.CENTER, SWT.FILL).applyTo(dateTime);

        return container;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);

        getButton(OK).setEnabled(validator.test(selection));
    }
}
