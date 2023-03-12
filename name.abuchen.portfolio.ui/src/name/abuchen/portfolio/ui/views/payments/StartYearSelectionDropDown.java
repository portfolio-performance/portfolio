package name.abuchen.portfolio.ui.views.payments;

import java.text.MessageFormat;
import java.time.LocalDate;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.DropDown;
import name.abuchen.portfolio.ui.util.SimpleAction;

/* package */ class StartYearSelectionDropDown extends DropDown implements IMenuListener
{
    private PaymentsViewModel model;

    public StartYearSelectionDropDown(PaymentsViewModel model)
    {
        super(createLabelTextForYear(model.getStartYear()));
        this.model = model;

        setMenuListener(this);
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        int now = LocalDate.now().getYear();

        for (int ii = 0; ii < 10; ii++)
        {
            int year = now - ii;

            Action action = new Action(String.valueOf(year))
            {
                @Override
                public void run()
                {
                    model.updateWith(year);
                    setLabel(createLabelTextForYear(year));
                }
            };
            action.setChecked(year == model.getStartYear());
            manager.add(action);
        }

        manager.add(new Separator());
        manager.add(new SimpleAction(Messages.LabelSelectYear, a -> {

            IInputValidator validator = newText -> {
                try
                {
                    int year = Integer.parseInt(newText);
                    return year >= 1900 && year <= now ? null
                                    : MessageFormat.format(Messages.MsgErrorDividendsYearBetween1900AndNow,
                                                    String.valueOf(now));
                }
                catch (NumberFormatException nfe)
                {
                    return MessageFormat.format(Messages.MsgErrorDividendsYearBetween1900AndNow, String.valueOf(now));
                }
            };

            InputDialog dialog = new InputDialog(Display.getDefault().getActiveShell(), Messages.LabelYear,
                            Messages.LabelPaymentsSelectStartYear, String.valueOf(model.getStartYear()), validator);

            if (dialog.open() == InputDialog.OK)
            {
                int year = Integer.parseInt(dialog.getValue());
                model.updateWith(year);
                setLabel(createLabelTextForYear(year));
            }

        }));
    }

    private static String createLabelTextForYear(int year)
    {
        return Messages.LabelSelectYearSince + " " + year; //$NON-NLS-1$
    }
}
