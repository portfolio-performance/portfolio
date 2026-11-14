package name.abuchen.portfolio.ui.dialogs;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.rest.spi.WriteConfirmation;
import name.abuchen.portfolio.ui.Messages;

/**
 * Shows what a local program is about to change in an open portfolio file and
 * waits for the user: allow this once, allow the same kind of change for the
 * rest of the session, or deny it.
 *
 * The text is composed here rather than handed over by the caller, because the
 * language lives in the application: what travels across the interface are
 * facts - which operation, which file, which thing, where to - and names taken
 * from the model as they are.
 *
 * Every way out of this dialog except one of the two allow buttons is a denial,
 * the window close and ESC included. At the expiry of the request the dialog
 * closes without a decision, which the caller reports as unanswered - the change
 * was not made either way, and the difference is only whether the caller may ask
 * again.
 */
public class ModelWriteApprovalDialog extends Dialog
{
    private static final int SESSION_ID = IDialogConstants.CLIENT_ID + 1;
    private static final int ONCE_ID = IDialogConstants.CLIENT_ID + 2;

    private final WriteConfirmation request;
    private boolean decided = false;

    public ModelWriteApprovalDialog(Shell parentShell, WriteConfirmation request)
    {
        super(parentShell);
        this.request = request;
    }

    @Override
    protected void configureShell(Shell newShell)
    {
        super.configureShell(newShell);
        newShell.setText(Messages.ApiWriteDialogTitle);
    }

    @Override
    public void create()
    {
        super.create();

        // pull the application forward; each OS applies its native policy
        getShell().forceActive();

        var remaining = Duration.between(Instant.now(), request.getExpiresAt()).toMillis();
        var display = getShell().getDisplay();
        display.timerExec((int) Math.max(0, remaining), () -> {
            if (getShell() != null && !getShell().isDisposed())
            {
                // expired: close without deciding - the caller reports it as
                // unanswered, and the change is not applied
                decided = true;
                close();
            }
        });
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        var container = (Composite) super.createDialogArea(parent);
        GridLayoutFactory.swtDefaults().margins(15, 15).applyTo(container);

        var message = new Label(container, SWT.WRAP);
        message.setText(MessageFormat.format(Messages.ApiWriteDialogMsg, request.getFileLabel()));
        GridDataFactory.fillDefaults().grab(true, false).hint(430, SWT.DEFAULT).applyTo(message);

        // what kind of change it is, in a row of its own rather than in the
        // sentence: an operation the application does not know has to be legible
        // too, and a sentence with a hole where a phrase should be is worse than
        // a label with an unfamiliar name on it
        if (isUsable(request.getOperation()))
            row(container, Messages.ApiWriteDialogChange, operationText());

        row(container, Messages.ApiWriteDialogWhat, subjectText());

        if (isUsable(request.getTargetName()))
            row(container, Messages.ApiWriteDialogWhere, request.getTargetName());

        if (isUsable(request.getDetail()))
            row(container, Messages.ApiWriteDialogDetails, request.getDetail());

        return container;
    }

    private static void row(Composite parent, String label, String value)
    {
        var name = new Label(parent, SWT.NONE);
        name.setText(label);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(name);

        var content = new Label(parent, SWT.WRAP);
        content.setText(value);
        GridDataFactory.fillDefaults().grab(true, false).indent(10, 0).hint(300, SWT.DEFAULT).applyTo(content);
    }

    /** which kind of change, in the user's language; an unknown one shows as it is named */
    private String operationText()
    {
        // only reached when the operation is usable, which the caller checks, so
        // the switch needs no empty string of its own
        switch (request.getOperation())
        {
            case "pp_assign_vehicle": //$NON-NLS-1$
                return Messages.ApiWriteOpAssign;
            case "pp_create_instrument": //$NON-NLS-1$
                return Messages.ApiWriteOpCreateInstrument;
            case "pp_create_watchlist": //$NON-NLS-1$
                return Messages.ApiWriteOpCreateWatchlist;
            case "pp_unassign_vehicle": //$NON-NLS-1$
                return Messages.ApiWriteOpUnassign;
            case "pp_update_instrument": //$NON-NLS-1$
                return Messages.ApiWriteOpUpdate;
            case "pp_update_quotes": //$NON-NLS-1$
                return Messages.ApiWriteOpUpdateQuotes;
            case "pp_update_watchlist": //$NON-NLS-1$
                return Messages.ApiWriteOpUpdateWatchlist;
            case "pp_delete_instrument": //$NON-NLS-1$
                return Messages.ApiWriteOpDelete;
            case "pp_delete_watchlist": //$NON-NLS-1$
                return Messages.ApiWriteOpDeleteWatchlist;
            default:
                return request.getOperation();
        }
    }

    /** the thing changed, named as the model names it, with its kind beside it */
    private String subjectText()
    {
        var name = request.getSubjectName();
        var kind = kindText();
        return kind == null ? name : MessageFormat.format("{0} ({1})", name, kind); //$NON-NLS-1$
    }

    /** which kind of vehicle, in the user's language; null when this API has no word for it */
    private String kindText()
    {
        var type = request.getSubjectType();
        if (type == null)
            return null;

        switch (type)
        {
            case "instrument": //$NON-NLS-1$
                return Messages.ApiWriteTypeInstrument;
            case "watchlist": //$NON-NLS-1$
                return Messages.ApiWriteTypeWatchlist;
            case "cash-account": //$NON-NLS-1$
                return Messages.ApiWriteTypeCashAccount;
            case "quote-update": //$NON-NLS-1$
                return Messages.ApiWriteTypeQuoteUpdate;
            default:
                return null;
        }
    }

    private static boolean isUsable(String value)
    {
        return value != null && !value.isBlank();
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, ONCE_ID, Messages.ApiWriteBtnAllowThis, false);
        createButton(parent, SESSION_ID, Messages.ApiWriteBtnAllowSession, false);
        // the denial is the default: return and ESC both refuse, so that an
        // absent-minded keypress cannot change the file. The focus goes to it as
        // well, because space activates the focused button rather than the
        // default one, and the buttons are created left to right - which would
        // otherwise leave a thumb already on spacebar reading as consent
        var deny = createButton(parent, IDialogConstants.CANCEL_ID, Messages.ApiWriteBtnDeny, true);
        deny.setFocus();
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        switch (buttonId)
        {
            case ONCE_ID:
                decide(request::allowOnce);
                break;
            case SESSION_ID:
                decide(request::allowForSession);
                break;
            case IDialogConstants.CANCEL_ID:
                decide(request::decline);
                break;
            default:
                super.buttonPressed(buttonId);
        }
    }

    /** window close and ESC count as a denial, too */
    @Override
    protected void handleShellCloseEvent()
    {
        decide(request::decline);
    }

    private void decide(Runnable decision)
    {
        if (!decided)
        {
            decided = true;
            decision.run();
        }
        close();
    }
}
