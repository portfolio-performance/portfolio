package name.abuchen.portfolio.ui.handlers;

import javax.inject.Named;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPart;

import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

public class SaveFileHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        return Platform.OS_LINUX.equals(Platform.getOS())
                        || (null != part && part.getObject() instanceof PortfolioPart);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell)
    {
        if (part == null || !(part.getObject() instanceof PortfolioPart))
        {
            MessageDialog.openWarning(shell, Messages.MsgNoFileOpen, Messages.MsgNoFileOpenText);
            return;
        }

        // trigger part to save file
        ((PortfolioPart) part.getObject()).save(part, shell);
    }
}
