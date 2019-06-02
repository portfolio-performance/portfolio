package name.abuchen.portfolio.ui.handlers;

import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.ClientInput;
import name.abuchen.portfolio.ui.editor.PortfolioPart;

/* package */class MenuHelper
{
    private MenuHelper()
    {
    }

    /* package */static boolean isClientPartActive(MPart part)
    {
        // issue: on Linux, the menu must always be active because activation
        // status is only checked once

        // issue: if the part is open, but the Client not yet decrypted, the
        // menu must be deactivated

        return Platform.OS_LINUX.equals(Platform.getOS())
                        || (null != part && part.getObject() instanceof PortfolioPart && ((PortfolioPart) part
                                        .getObject()).getClient() != null);
    }

    /* package */static Client getActiveClient(MPart part)
    {
        if (part == null || !(part.getObject() instanceof PortfolioPart)
                        || ((PortfolioPart) part.getObject()).getClient() == null)
        {
            // as the menu is always active on Linux, show a dialog why nothing
            // happens when choosing the menu
            MessageDialog.openWarning(Display.getDefault().getActiveShell(), Messages.MsgNoFileOpen,
                            Messages.MsgNoFileOpenText);
            return null;
        }

        return ((PortfolioPart) part.getObject()).getClient();
    }

    /* package */static ClientInput getActiveClientInput(MPart part)
    {
        if (part == null || !(part.getObject() instanceof PortfolioPart)
                        || ((PortfolioPart) part.getObject()).getClient() == null)
        {
            // as the menu is always active on Linux, show a dialog why nothing
            // happens when choosing the menu
            MessageDialog.openWarning(Display.getDefault().getActiveShell(), Messages.MsgNoFileOpen,
                            Messages.MsgNoFileOpenText);
            return null;
        }

        return ((PortfolioPart) part.getObject()).getClientInput();
    }

}
