package name.abuchen.portfolio.ui.handlers;

import java.util.Optional;

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
        // issue: on unity desktop, the enablement of menu items is only checked
        // once at the start. Therefore we always enable all menu items
        // regardless whether a file is open
        if (UnityDesktopHelper.isUnity())
            return true;

        // issue: check for the availability of Client, not ClientInput, because
        // if the part is open, but the Client not yet decrypted, menus still
        // must be deactivated

        return null != part && part.getObject() instanceof PortfolioPart
                        && ((PortfolioPart) part.getObject()).getClient() != null;
    }

    /* package */static Optional<Client> getActiveClient(MPart part)
    {
        return getActiveClientInput(part).map(ClientInput::getClient);
    }

    /* package */static Optional<ClientInput> getActiveClientInput(MPart part)
    {
        return getActiveClientInput(part, true);
    }

    /* package */static Optional<ClientInput> getActiveClientInput(MPart part, boolean showWarning)
    {
        if (part == null || !(part.getObject() instanceof PortfolioPart)
                        || ((PortfolioPart) part.getObject()).getClient() == null)
        {
            if (showWarning)
            {
                // as the menu is always active on Linux, show a dialog why
                // nothing happens when choosing the menu
                MessageDialog.openWarning(Display.getDefault().getActiveShell(), Messages.MsgNoFileOpen,
                                Messages.MsgNoFileOpenText);
            }
            return Optional.empty();
        }

        return Optional.of(((PortfolioPart) part.getObject()).getClientInput());
    }

}
