package name.abuchen.portfolio.ui.handlers;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;

import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.ui.ClientEditor;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.handlers.HandlerUtil;

public class SaveAsFileHandler extends AbstractHandler
{

    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        // determine extension
        String extension = event.getParameter(UIConstants.Parameter.EXTENSION);
        if (extension == null)
            throw new IllegalArgumentException("Missing file extension parameter"); //$NON-NLS-1$

        // check whether encryption is supported
        String encryptionMethod = event.getParameter(UIConstants.Parameter.ENCRYPTION_METHOD);

        if ("AES256".equals(encryptionMethod) && !ClientFactory.isKeyLengthSupported(256)) //$NON-NLS-1$
        {
            Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            new JurisdictionFilesDownloadDialog(shell).open();
            return null;
        }

        // trigger editor to save file
        IWorkbenchPage page = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
        final IEditorPart editor = page.getActiveEditor();

        if (!(editor instanceof ClientEditor))
            return null;

        ((ClientEditor) editor).doSaveAs(extension, encryptionMethod);

        return null;
    }

    private static class JurisdictionFilesDownloadDialog extends MessageDialog
    {
        public JurisdictionFilesDownloadDialog(Shell parentShell)
        {
            super(parentShell, Messages.JurisdictionFilesDownloadTitle, null,
                            Messages.JurisdictionFilesDownloadMessage, CONFIRM,
                            new String[] { IDialogConstants.OK_LABEL }, 0);
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
            if (javaVersion.startsWith("1.6")) //$NON-NLS-1$
                downloadURL = "http://www.oracle.com/technetwork/java/javase/downloads/jce-6-download-429243.html"; //$NON-NLS-1$
            else if (javaVersion.startsWith("1.7")) //$NON-NLS-1$
                downloadURL = "http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html"; //$NON-NLS-1$
            else if (javaVersion.startsWith("1.8")) //$NON-NLS-1$
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
                    try
                    {
                        final IWebBrowser browser = PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser();
                        browser.openURL(new URL(event.text));
                    }
                    catch (PartInitException e)
                    {
                        PortfolioPlugin.log(e);
                    }
                    catch (MalformedURLException e)
                    {
                        PortfolioPlugin.log(e);
                    }
                }
            });

            return explanation;
        }
    }
}
