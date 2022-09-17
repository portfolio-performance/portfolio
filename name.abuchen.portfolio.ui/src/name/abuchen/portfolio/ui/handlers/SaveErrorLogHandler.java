package name.abuchen.portfolio.ui.handlers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.time.LocalDate;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.internal.workbench.E4Workbench;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

@SuppressWarnings("restriction")
public class SaveErrorLogHandler
{
    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell,
                    @Named(E4Workbench.INSTANCE_LOCATION) Location instanceLocation)
    {

        FileDialog dialog = new FileDialog(shell, SWT.SAVE);
        dialog.setOverwrite(true);
        dialog.setFileName(MessageFormat.format("pp-error-{0}.log", LocalDate.now().toString())); //$NON-NLS-1$
        dialog.setFilterPath(System.getProperty("user.home")); //$NON-NLS-1$

        String path = dialog.open();
        if (path == null)
            return;

        File logfile = new File(instanceLocation.getURL().getFile(), ".metadata/.log"); //$NON-NLS-1$

        if (!logfile.exists())
        {
            MessageDialog.openError(shell, Messages.LabelError,
                            MessageFormat.format(Messages.MsgErrorOpeningFile, logfile.getAbsoluteFile()));
            return;
        }

        try
        {
            Files.copy(logfile.toPath(), new File(path).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(shell, Messages.LabelError, e.getMessage());
        }
    }
}
