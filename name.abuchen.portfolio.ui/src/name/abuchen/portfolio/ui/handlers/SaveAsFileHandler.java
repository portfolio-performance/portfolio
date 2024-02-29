package name.abuchen.portfolio.ui.handlers;

import java.util.EnumSet;
import java.util.Set;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.SaveFlag;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.util.swt.ActiveShell;

public class SaveAsFileHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        return MenuHelper.isClientPartActive(part);
    }

    @SuppressWarnings("nls")
    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell,
                    @Named(UIConstants.Parameter.FILE_TYPE) String type,
                    @Optional @Named("name.abuchen.portfolio.ui.param.create-copy") String doCreateCopy)
    {
        if (!MenuHelper.getActiveClientInput(part).isPresent())
            return;

        if (type == null)
            throw new IllegalArgumentException("Missing file extension parameter"); //$NON-NLS-1$

        Set<SaveFlag> flags = EnumSet.noneOf(SaveFlag.class);
        String extension = null;

        switch (type)
        {
            case "xml":
                flags.add(SaveFlag.XML);
                extension = "xml";
                break;
            case "xml+zip":
                flags.add(SaveFlag.XML);
                flags.add(SaveFlag.COMPRESSED);
                extension = "zip";
                break;
            case "xml+aes256":
                flags.add(SaveFlag.XML);
                flags.add(SaveFlag.ENCRYPTED);
                flags.add(SaveFlag.AES256);
                extension = "portfolio";
                break;
            case "xml+aes128":
                flags.add(SaveFlag.XML);
                flags.add(SaveFlag.ENCRYPTED);
                flags.add(SaveFlag.AES128);
                extension = "portfolio";
                break;
            case "binary":
                flags.add(SaveFlag.BINARY);
                flags.add(SaveFlag.COMPRESSED);
                extension = "portfolio";
                break;
            case "binary+aes256":
                flags.add(SaveFlag.BINARY);
                flags.add(SaveFlag.ENCRYPTED);
                flags.add(SaveFlag.AES256);
                extension = "portfolio";
                break;
            default:
                throw new IllegalArgumentException("Unknown file type " + type); //$NON-NLS-1$

        }

        // trigger part to save file
        try
        {
            if (Boolean.parseBoolean(doCreateCopy))
            {
                ((PortfolioPart) part.getObject()).doExportAs(shell, extension, flags);
            }
            else
            {
                ((PortfolioPart) part.getObject()).doSaveAs(shell, extension, flags);
            }
        }
        catch (RuntimeException e)
        {
            PortfolioPlugin.log(e);

            Display.getDefault().asyncExec(
                            () -> MessageDialog.openError(ActiveShell.get(), Messages.LabelError, e.getMessage()));
        }
    }
}
