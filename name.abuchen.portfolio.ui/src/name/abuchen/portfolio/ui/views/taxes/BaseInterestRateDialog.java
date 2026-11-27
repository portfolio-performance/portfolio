package name.abuchen.portfolio.ui.views.taxes;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.Messages;

/**
 * Asks for the base interest rate of a year. If a manual rate is stored for
 * the year, the dialog offers to drop it.
 */
/* package */ class BaseInterestRateDialog extends InputDialog
{
    /* package */ static final int RESET_ID = 0x1000;

    private final boolean canReset;

    /* package */ BaseInterestRateDialog(Shell parentShell, int year, String initialValue, boolean canReset)
    {
        super(parentShell, Messages.LabelBaseInterestRate,
                        MessageFormat.format(Messages.LabelEnterBaseInterestRate, String.valueOf(year)),
                        initialValue,
                        text -> ManualBaseInterestRates.parsePercent(text) == null ? Messages.MsgInvalidBaseInterestRate
                                        : null);

        this.canReset = canReset;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        if (canReset)
            createButton(parent, RESET_ID, Messages.BtnLabelResetBaseInterestRate, false);

        super.createButtonsForButtonBar(parent);
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        if (buttonId != RESET_ID)
        {
            super.buttonPressed(buttonId);
            return;
        }

        setReturnCode(RESET_ID);
        close();
    }
}
