package name.abuchen.portfolio.ui.handlers;

import java.io.File;
import java.text.MessageFormat;
import java.util.EnumSet;
import java.util.Set;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.SaveFlag;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.util.DesktopAPI;
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

        // check whether encryption is supported
        if (flags.contains(SaveFlag.AES256) && !ClientFactory.isKeyLengthSupported(256)) // $NON-NLS-1$
        {
            new JurisdictionFilesDownloadDialog(shell).open();
            return;
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

    private static class JurisdictionFilesDownloadDialog extends MessageDialog
    {
        public JurisdictionFilesDownloadDialog(Shell parentShell)
        {
            super(parentShell, Messages.JurisdictionFilesDownloadTitle, null, Messages.JurisdictionFilesDownloadMessage,
                            CONFIRM, new String[] { IDialogConstants.OK_LABEL }, 0);
        }

        @Override
        protected Control createCustomArea(Composite parent)
        {
            Composite container = new Composite(parent, SWT.NONE);
            GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);

            // Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction
            // Policy Files Download URL
            String javaVersion = System.getProperty("java.version"); //$NON-NLS-1$
            String downloadURL = null;
            if (javaVersion.startsWith("1.8")) //$NON-NLS-1$
                downloadURL = "http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html"; //$NON-NLS-1$
            else
                downloadURL = "http://www.oracle.com/technetwork/java/javase/downloads/"; //$NON-NLS-1$

            // Java home directory
            File target = new File(System.getProperty("java.home"), "lib/security"); //$NON-NLS-1$ //$NON-NLS-2$

            String message = MessageFormat.format(Messages.JurisdictionFilesDownloadExplanation, downloadURL,
                            target.getAbsolutePath());

            Link explanation = new Link(container, SWT.NONE);
            explanation.setText(message);
            GridDataFactory.fillDefaults().grab(true, true).applyTo(explanation);

            explanation.addSelectionListener(new SelectionAdapter()
            {
                @Override
                public void widgetSelected(SelectionEvent event)
                {
                    DesktopAPI.browse(String.valueOf(event.text));
                }
            });

            return explanation;
        }
    }
}
