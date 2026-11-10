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
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.rest.spi.ApiAccessRequest;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;

/**
 * Asks the user whether a local program may access the REST API. The name is
 * self-declared by the requesting program and framed as such. Any dismissal
 * counts as a decline; the dialog dismisses itself when the request expires.
 */
public class ApiAccessApprovalDialog extends Dialog
{
    private static final int SESSION_ID = IDialogConstants.CLIENT_ID + 1;
    private static final int ALWAYS_ID = IDialogConstants.CLIENT_ID + 2;

    private final ApiAccessRequest request;
    private final boolean anyFileEnabled;
    private final Runnable openPreferences;

    private boolean decided = false;

    public ApiAccessApprovalDialog(Shell parentShell, ApiAccessRequest request, boolean anyFileEnabled,
                    Runnable openPreferences)
    {
        super(parentShell);
        this.request = request;
        this.anyFileEnabled = anyFileEnabled;
        this.openPreferences = openPreferences;
    }

    @Override
    protected void configureShell(Shell newShell)
    {
        super.configureShell(newShell);
        newShell.setText(Messages.ApiAccessDialogTitle);
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
                // expired: close without deciding - the service reports
                // "expired" to the polling client
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
        message.setText(MessageFormat.format(Messages.ApiAccessDialogMsg, request.getClientName()));
        GridDataFactory.fillDefaults().grab(true, false).hint(400, SWT.DEFAULT).applyTo(message);

        if (!anyFileEnabled)
        {
            var hint = new Link(container, SWT.WRAP);
            hint.setText(Messages.ApiAccessMsgNoFilesEnabled);
            hint.setForeground(Colors.theme().warningForeground());
            hint.addListener(SWT.Selection, event -> openPreferences.run());
            GridDataFactory.fillDefaults().grab(true, false).hint(400, SWT.DEFAULT).indent(0, 10).applyTo(hint);
        }

        return container;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        createButton(parent, SESSION_ID, Messages.ApiAccessBtnAllowSession, false);
        createButton(parent, ALWAYS_ID, Messages.ApiAccessBtnAllowAlways, false);
        createButton(parent, IDialogConstants.CANCEL_ID, Messages.ApiAccessBtnDecline, true);
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        switch (buttonId)
        {
            case SESSION_ID:
                decide(request::allowForSession);
                break;
            case ALWAYS_ID:
                decide(request::allowAlways);
                break;
            case IDialogConstants.CANCEL_ID:
                decide(request::decline);
                break;
            default:
                super.buttonPressed(buttonId);
        }
    }

    /** window close and ESC count as a decline, too */
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
