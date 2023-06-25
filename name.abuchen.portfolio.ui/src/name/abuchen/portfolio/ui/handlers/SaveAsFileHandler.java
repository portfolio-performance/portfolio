package name.abuchen.portfolio.ui.handlers;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.ClientFileType;
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

        ClientFileType fileType;

        switch (type)
        {
            case "xml":
                fileType = ClientFileType.XML;
                break;
            case "xml+id":
                fileType = ClientFileType.XML_ID;
                break;
            case "xml+zip":
                fileType = ClientFileType.XML_ZIP;
                break;
            case "xml+aes256":
                fileType = ClientFileType.XML_AES256;
                break;
            case "xml+aes128":
                fileType = ClientFileType.XML_AES128;
                break;
            case "binary":
                fileType = ClientFileType.BINARY;
                break;
            case "binary+aes256":
                fileType = ClientFileType.BINARY_AES256;
                break;
            default:
                throw new IllegalArgumentException("Unknown file type " + type); //$NON-NLS-1$
        }

        // trigger part to save file
        try
        {
            if (Boolean.parseBoolean(doCreateCopy))
            {
                ((PortfolioPart) part.getObject()).doExportAs(shell, fileType.getExtension(), fileType.getFlags());
            }
            else
            {
                ((PortfolioPart) part.getObject()).doSaveAs(shell, fileType.getExtension(), fileType.getFlags());
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
