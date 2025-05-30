package name.abuchen.portfolio.ui.util;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.oauth.AuthenticationException;
import name.abuchen.portfolio.oauth.OAuthClient;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.impl.PortfolioPerformanceFeed;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.preferences.PPIDPreferencePage.AccessTokenRunnable;
import name.abuchen.portfolio.ui.util.swt.ActiveShell;
import name.abuchen.portfolio.ui.util.swt.StyledLabel;

public class LoginButton
{
    public static class LoginDialog extends Dialog
    {
        private OAuthClient oauthClient = OAuthClient.INSTANCE;

        public LoginDialog(Shell parentShell)
        {
            super(parentShell);
        }

        @Override
        protected void configureShell(Shell shell)
        {
            super.configureShell(shell);
            shell.setText(Factory.getQuoteFeed(PortfolioPerformanceFeed.class).getName());
        }

        @Override
        protected Control createDialogArea(Composite parent)
        {
            Composite container = (Composite) super.createDialogArea(parent);
            GridLayoutFactory.fillDefaults().numColumns(1).extendedMargins(10, 10, 10, 10).applyTo(container);

            var description = new StyledLabel(container, SWT.WRAP);
            GridDataFactory.swtDefaults().hint(500, SWT.DEFAULT).applyTo(description);
            description.setText(Messages.PrefDescriptionPortfolioPerformanceID);

            return container;
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent)
        {
            createButton(parent, 1000, oauthClient.isAuthenticated() ? Messages.CmdLogout : Messages.CmdLogin, true);
            createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, false);
        }

        @Override
        protected void buttonPressed(int buttonId)
        {
            switch (buttonId)
            {
                case 1000 -> {
                    try
                    {
                        if (oauthClient.isAuthenticated())
                        {
                            run(() -> {
                                oauthClient.signOut();
                                return null;
                            });
                        }
                        else
                        {
                            oauthClient.signIn(DesktopAPI::browse);
                        }
                        close();
                    }
                    catch (AuthenticationException e)
                    {
                        PortfolioPlugin.log(e);
                        MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError,
                                        e.getMessage());
                    }
                }
                case IDialogConstants.CLOSE_ID -> close();
                default -> super.buttonPressed(buttonId);
            }
        }

        private static <T> void run(AccessTokenRunnable<T> supplier)
        {
            Job.createSystem("Asynchronously retrieve token", monitor -> { //$NON-NLS-1$
                try
                {
                    supplier.get();
                }
                catch (AuthenticationException e)
                {
                    Display.getDefault().asyncExec(() -> MessageDialog.openError(Display.getDefault().getActiveShell(),
                                    Messages.LabelError, e.getMessage()));
                }
            }).schedule();
        }
    }

    private LoginButton()
    {
    }

    public static Button create(Composite parent)
    {
        var oauthClient = OAuthClient.INSTANCE;

        var action = new Button(parent, SWT.NONE);
        action.setEnabled(!oauthClient.isAuthenticationOngoing());
        action.setText(oauthClient.isAuthenticated() ? Messages.CmdLogout : Messages.CmdLogin);

        Runnable updateListener = () -> Display.getDefault().asyncExec(() -> {
            action.setEnabled(!oauthClient.isAuthenticationOngoing());
            action.setText(oauthClient.isAuthenticated() ? Messages.CmdLogout : Messages.CmdLogin);
        });

        oauthClient.addStatusListener(updateListener);
        parent.addDisposeListener(event -> oauthClient.removeStatusListener(updateListener));

        action.addSelectionListener(
                        SelectionListener.widgetSelectedAdapter(event -> new LoginDialog(ActiveShell.get()).open()));

        return action;
    }
}
