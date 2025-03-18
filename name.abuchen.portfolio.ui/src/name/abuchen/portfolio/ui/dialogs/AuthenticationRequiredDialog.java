package name.abuchen.portfolio.ui.dialogs;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.oauth.AuthenticationException;
import name.abuchen.portfolio.oauth.OAuthClient;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.DesktopAPI;

public class AuthenticationRequiredDialog
{
    public static void open(Shell shell)
    {
        var dialog = new MessageDialog(shell, Messages.LabelInfo, null,
                        Messages.LabelAuthenticationRequiredToRetrieveHistoricalPrices, MessageDialog.WARNING, 0,
                        Messages.CmdLogin, Messages.CmdNotNow);

        var result = dialog.open();

        if (result == 0)
        {
            try
            {
                OAuthClient.INSTANCE.signIn(DesktopAPI::browse);
            }
            catch (AuthenticationException e)
            {
                PortfolioPlugin.log(e);
                MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.LabelError, e.getMessage());
            }
        }
    }

    private AuthenticationRequiredDialog()
    {
    }
}
